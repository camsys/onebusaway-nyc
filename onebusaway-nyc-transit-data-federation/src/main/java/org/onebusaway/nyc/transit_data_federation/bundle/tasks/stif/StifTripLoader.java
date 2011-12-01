/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.GeographyRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.StifRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.TimetableRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.TripRecord;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RunData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create a mapping from Destination Sign Code (DSC) to GTFS Trip objects using
 * data in STIF, MTA's internal format.
 */
public class StifTripLoader {

  private static final Logger _log = LoggerFactory.getLogger(StifTripLoader.class);

  private StifTripLoaderSupport support = new StifTripLoaderSupport();

  private Map<String, List<AgencyAndId>> tripIdsBySignCode = new HashMap<String, List<AgencyAndId>>();

  private int _tripsCount = 0;

  private int _tripsWithoutMatchCount = 0;

  private Map<AgencyAndId, RunData> runsForTrip = new HashMap<AgencyAndId, RunData>();

  private Map<Trip, RawRunData> rawRunDataByTrip = new HashMap<Trip, RawRunData>();

  private Map<ServiceCode, List<RawTrip>> rawData = new HashMap<ServiceCode, List<RawTrip>>();
  
  @Autowired
  public void setGtfsDao(GtfsMutableRelationalDao dao) {
    support.setGtfsDao(dao);
  }

  /**
   * Get the mapping from DSC and schedule id to list of trips.
   */
  public Map<String, List<AgencyAndId>> getTripMapping() {
    return tripIdsBySignCode;
  }

  public int getTripsCount() {
    return _tripsCount;
  }

  public int getTripsWithoutMatchCount() {
    return _tripsWithoutMatchCount;
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
    } catch (Exception e) {
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
          if (!rawData.containsKey(serviceCode)) {
            rawData.put(serviceCode, new ArrayList<RawTrip>());
          }
        }
        if (record instanceof GeographyRecord) {
          GeographyRecord geographyRecord = ((GeographyRecord) record);
          support.putStopIdForLocation(geographyRecord.getIdentifier(),
              geographyRecord.getBoxID());
        }
        if (record instanceof TripRecord) {
          TripRecord tripRecord = (TripRecord) record;
          int tripType = tripRecord.getTripType();

          String run1 = tripRecord.getRunId();
          String run2 = tripRecord.getReliefRunId();
          String run3 = tripRecord.getNextTripOperatorRunId();

          RawTrip rawTrips = new RawTrip(tripRecord.getRunId(),
              tripRecord.getReliefRunId(), tripRecord.getNextTripOperatorRunId(),
              StifTripType.byValue(tripType), tripRecord.getSignCode());
          rawTrips.serviceCode = serviceCode;
          rawTrips.firstStopTime = tripRecord.getOriginTime();
          rawTrips.lastStopTime = tripRecord.getDestinationTime();
          rawTrips.firstStop = support.getStopIdForLocation(tripRecord.getOriginLocation());
          rawTrips.lastStop = support.getStopIdForLocation(tripRecord.getDestinationLocation());
          rawTrips.recoveryTime = tripRecord.getRecoveryTime();
          rawTrips.firstTripInSequence = tripRecord.isFirstTripInSequence();
          rawTrips.lastTripInSequence = tripRecord.isLastTripInSequence();
          rawData.get(serviceCode).add(rawTrips);

          if (tripType == 2 || tripType == 3 || tripType == 4) {
            // deadhead or to/from depot
            continue;
          }
          String code = tripRecord.getSignCode();

          TripIdentifier id = support.getIdentifierForTripRecord(tripRecord);
          List<Trip> trips = support.getTripsForIdentifier(id);

          _tripsCount++;

          if (trips == null || trips.isEmpty()) {
            // trip in stif but not in gtfs
            if (!warned) {
              warned = true;
              _log.warn("gtfs trip not found for " + id);
            }
            _tripsWithoutMatchCount++;
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
            Character dayCode1 = serviceId.charAt(serviceId.length() - 2);
            Character dayCode2 = serviceId.charAt(serviceId.length() - 1);
            
            // schedule runs on on days where a dayCode1 is followed by a
            // dayCode2;
            // contains all trips from dayCode1, and pre-midnight trips for
            // dayCode2;

            if (tripRecord.getOriginTime() < 0) {
              /* possible trip records are those containing the previous day */
              if (StifTripLoaderSupport.scheduleIdForGtfsDayCode(dayCode2.toString()) != serviceCode) {
                trip = null;
              }
            } else {
              if (StifTripLoaderSupport.scheduleIdForGtfsDayCode(dayCode1.toString()) != serviceCode) {
                trip = null;
              }
            }

            if (trip != null) {

              rawTrips.addGtfsTrip(trip);

              int reliefTime = tripRecord.getReliefTime();
              String block = tripRecord.getBlockNumber();
              String depotCode = tripRecord.getDepotCode();
              RawRunData rawRunData = new RawRunData(run1, run2, run3, serviceId, serviceCode, block, depotCode);
              
              filtered.add(trip);
              rawRunDataByTrip.put(trip, rawRunData);
              runsForTrip.put(trip.getId(), new RunData(run1, run2, reliefTime));
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

  public Map<Trip, RawRunData> getRawRunDataByTrip() {
    return rawRunDataByTrip;
  }

  public Map<AgencyAndId, RunData> getRunsForTrip() {
    return runsForTrip;
  }

  public Map<ServiceCode, List<RawTrip>> getRawStifData() {
    return rawData;
  }
}
