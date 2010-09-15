/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onebusaway.nyc.stif;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.onebusaway.gtfs.impl.HibernateGtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.HibernateOperation;
import org.onebusaway.nyc.stif.model.GeographyRecord;
import org.onebusaway.nyc.stif.model.ServiceCode;
import org.onebusaway.nyc.stif.model.StifRecord;
import org.onebusaway.nyc.stif.model.TimetableRecord;
import org.onebusaway.nyc.stif.model.TripRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create a mapping from Destination Sign Code (DSC) to GTFS Trip objects using
 * data in STIF, MTA's internal format.
 */
public class StifTripLoader {

  private static final Logger _log = LoggerFactory.getLogger(StifTripLoader.class);

  private class TripIdentifier {
    public int startTime;
    public String routeName;
    public String startStop;

    public TripIdentifier(String routeName, int startTime, String startStop) {
      this.routeName = routeName;
      this.startTime = startTime;
      this.startStop = startStop;
    }

    public TripIdentifier(TripRecord tripRecord) {
      routeName = tripRecord.getRoute();
      startStop = getStopIdForLocation(tripRecord.getOriginLocation());
      startTime = tripRecord.getOriginTime();
      if (startTime < 0) {
        // skip a day ahead for previous-day trips.
        startTime += 24 * 60 * 60;
      }
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof TripIdentifier) {
        TripIdentifier otherTrip = (TripIdentifier) other;
        return otherTrip.startTime == startTime
            && otherTrip.startStop.equals(startStop)
            && otherTrip.routeName.equals(routeName);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return startTime + routeName.hashCode() + 31 * startStop.hashCode();
    }

    @Override
    public String toString() {
      return "TripIdentifier(" + startTime + ", " + routeName + "," + startStop
          + ")";
    }
  }

  private GtfsRelationalDao gtfsDao;

  private Map<String, List<AgencyAndId>> tripIdsBySignCode = new HashMap<String, List<AgencyAndId>>();

  private Map<TripIdentifier, List<Trip>> tripsByIdentifier;

  private Map<String, String> stopIdsByLocation = new HashMap<String, String>();

  @Autowired
  public void setGtfsDao(GtfsRelationalDao dao) {
    this.gtfsDao = dao;
  }

  public String getStopIdForLocation(String originLocation) {
    return stopIdsByLocation.get(originLocation);
  }

  /**
   * Get the mapping from DSC and schedule id to list of trips.
   */
  public Map<String, List<AgencyAndId>> getTripMapping() {
    return tripIdsBySignCode;
  }

  public static ServiceCode scheduleIdForGtfsDayCode(char dayCode) {
    switch (dayCode) {
      case 'A':
        return ServiceCode.SATURDAY;
      case 'B':
        return ServiceCode.WEEKDAY_SCHOOL_CLOSED;
      case 'C':
        return ServiceCode.WEEKDAY_SCHOOL_OPEN;
      case 'D':
        return ServiceCode.SUNDAY;
      default:
        return null;
    }
  }

  /**
   * For each STIF file, call run().
   */
  public void run(File path) {
    try {
      _log.info("loading stif from " + path.getAbsolutePath());
      InputStream in = new FileInputStream(path);
      if (path.getName().endsWith(".gz"))
        in = new GZIPInputStream(in);
      run(in);
    } catch (IOException e) {
      throw new RuntimeException("Error loading " + path, e);
    }
  }

  public void run(InputStream stream) {
    StifRecordReader reader;

    boolean warned = false;
    try {
      reader = new StifRecordReader(stream);
      ServiceCode serviceCode = null;
      while (true) {
        StifRecord record = reader.read();
        if (record == null) {
          break;
        }
        if (record instanceof TimetableRecord) {
          serviceCode = ((TimetableRecord) record).getServiceCode();
        }
        if (record instanceof GeographyRecord) {
          GeographyRecord geographyRecord = ((GeographyRecord) record);
          stopIdsByLocation.put(geographyRecord.getIdentifier(),
              geographyRecord.getBoxID());
        }
        if (record instanceof TripRecord) {
          TripRecord tripRecord = (TripRecord) record;
          int tripType = tripRecord.getTripType();
          if (tripType == 2 || tripType == 3 || tripType == 4) {
            continue; // deadhead or to/from depot
          }
          String code = tripRecord.getSignCode();
          TripIdentifier id = new TripIdentifier(tripRecord);
          List<Trip> trips = getTripsForIdentifier(id);
          if (trips == null || trips.isEmpty()) {

            // trip in stif but not in gtfs
            if (!warned) {
              warned = true;
              _log.warn("gtfs trip not found for " + id);
            }
            continue;
          }

          List<Trip> filtered = new ArrayList<Trip>();
          /* filter trips by schedule */
          for (Trip trip : trips) {
            /*
             * Service codes are of the form 20100627CA Only the last two
             * characters are important. They have the meaning: A = sat B =
             * weekday closed C = weekday open D = sun
             * 
             * The first character is for trips on that day's STIF schedule,
             * while the second character is for trips on the next day's STIF
             * schedule (but that run on that day).
             * 
             * To figure out whether a GTFS trip corresponds to a STIF trip, if
             * the STIF trip is before midnight, check daycode1; else check
             * daycode2
             */

            String serviceId = trip.getServiceId().getId();
            char dayCode1 = serviceId.charAt(serviceId.length() - 2);
            char dayCode2 = serviceId.charAt(serviceId.length() - 1);

            // schedule runs on on days where a dayCode1 is followed by a
            // dayCode2;
            // contains all trips from dayCode1, and pre-midnight trips for
            // dayCode2;

            if (tripRecord.getOriginTime() < 0) {
              /* possible trip records are those containing the previous day */
              if (scheduleIdForGtfsDayCode(dayCode2) == serviceCode) {
                filtered.add(trip);
                trip.setBlockId(tripRecord.getBlockNumber());
              }
            } else {
              if (scheduleIdForGtfsDayCode(dayCode1) == serviceCode) {
                filtered.add(trip);
                trip.setBlockId(tripRecord.getBlockNumber());
              }
            }

          }

          List<AgencyAndId> sctrips = tripIdsBySignCode.get(code);
          if (sctrips == null) {
            sctrips = new ArrayList<AgencyAndId>();
            tripIdsBySignCode.put(code, sctrips);
          }
          for (Trip trip : filtered)
            sctrips.add(trip.getId());
        }

      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Trip> getTripsForIdentifier(TripIdentifier id) {

    /**
     * Lazy initialization
     */
    if (tripsByIdentifier == null) {

      tripsByIdentifier = new HashMap<StifTripLoader.TripIdentifier, List<Trip>>();

      Collection<Trip> allTrips = gtfsDao.getAllTrips();
      int index = 0;

      for (Trip trip : allTrips) {

        if (index % 500 == 0)
          _log.info("trip=" + index + " / " + allTrips.size());
        index++;

        TripIdentifier tripIdentifier = getTripAsIdentifier(trip);
        List<Trip> trips = tripsByIdentifier.get(tripIdentifier);
        if (trips == null) {
          trips = new ArrayList<Trip>();
          tripsByIdentifier.put(tripIdentifier, trips);
        }
        trips.add(trip);
      }
    }

    return tripsByIdentifier.get(id);
  }

  private TripIdentifier getTripAsIdentifier(final Trip trip) {

    String routeName = trip.getRoute().getId().getId();
    int startTime = -1;
    String startStop;
    
    /**
     * This is WAY faster if we are working in hibernate, in that we don't need
     * to look up EVERY StopTime for a Trip, along with the Stop objects for
     * those StopTimes. Instead, we just grab the first departure time.
     */
    if (gtfsDao instanceof HibernateGtfsRelationalDaoImpl) {
      HibernateGtfsRelationalDaoImpl dao = (HibernateGtfsRelationalDaoImpl) gtfsDao;
      List<?> rows = (List<?>) dao.execute(new HibernateOperation() {
        @Override
        public Object doInHibernate(Session session) throws HibernateException,
            SQLException {
          Query query = session.createQuery("SELECT st.departureTime, st.stop.id.id FROM StopTime st WHERE st.trip = :trip AND st.departureTime >= 0 ORDER BY st.departureTime ASC LIMIT 1");
          query.setParameter("trip", trip);
          return query.list();
        }
      });
      Object[] values = (Object[]) rows.get(0);
      startTime = ((Integer) values[0]);
      startStop = (String) values[1];
    } else {
      StopTime stopTime = gtfsDao.getStopTimesForTrip(trip).get(0);
      startTime = stopTime.getDepartureTime();
      startStop = stopTime.getStop().getId().getId();
    }
    return new TripIdentifier(routeName, startTime, startStop);
  }
}
