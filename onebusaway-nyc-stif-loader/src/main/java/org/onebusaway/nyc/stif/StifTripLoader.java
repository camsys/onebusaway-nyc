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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.nyc.stif.model.ServiceCode;
import org.onebusaway.nyc.stif.model.StifRecord;
import org.onebusaway.nyc.stif.model.TimetableRecord;
import org.onebusaway.nyc.stif.model.TripRecord;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create a mapping from Destination Sign Code (DSC) to GTFS Trip objects using
 * data in STIF, MTA's internal format.
 */
public class StifTripLoader  implements Runnable {

  private class TripIdentifier {
    public int startTime;
    public String routeName;

    public TripIdentifier(Trip trip) {
      this.routeName = trip.getRoute().getId().getId();
      this.startTime = gtfsDao.getStopTimesForTrip(trip).get(0).getDepartureTime();
    }

    public TripIdentifier(TripRecord tripRecord) {
      routeName = tripRecord.getRoute();
      startTime = tripRecord.getOriginTime();
      if (startTime < 0) {
        //skip a day ahead for previous-day trips.
        startTime += 24 * 60 * 60;
      }
    }

    public boolean equals(Object other) {
      if (other instanceof TripIdentifier) {
        TripIdentifier otherTrip = (TripIdentifier) other;
        return otherTrip.startTime == startTime
            && otherTrip.routeName.equals(routeName);
      }
      return false;
    }

    public int hashCode() {
      return startTime ^ routeName.hashCode();
    }

    public String toString() {
      return "TripIdentifier(" + startTime + ", " + routeName + ")";
    }
  }

  @Autowired
  private GtfsRelationalDao gtfsDao;
  
  private Map<String, List<Trip>> tripsBySignCode;
  private Map<TripIdentifier, List<Trip>> tripsByIdentifier;

  public void setGtfsDao(GtfsRelationalDao dao) {
    this.gtfsDao = dao;
  }

  /**
   * Get the mapping from DSC and schedule id to list of trips.
   */
  public Map<String, List<Trip>> getTripMapping() {
    return tripsBySignCode;
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
   * After calling setGtfsDao, call init() to read the trips and set up the trip
   * mapping.
   */
  @PostConstruct
	public void init() {

		tripsByIdentifier = new HashMap<TripIdentifier, List<Trip>>();
		for (Trip trip : gtfsDao.getAllTrips()) {

			TripIdentifier id = new TripIdentifier(trip);
			List<Trip> trips = tripsByIdentifier.get(id);
			if (trips == null) {
				trips = new ArrayList<Trip>();
				tripsByIdentifier.put(id, trips);
			}
			trips.add(trip);
		}
		
		tripsBySignCode = new HashMap<String, List<Trip>>();
	}

  /**
   * For each STIF file, call run().
   */
  public void run(File path) {
    try {
      run(new FileInputStream(path));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Error loading " + path, e);
    }
  }

  public void run(InputStream stream) {
    StifRecordReader reader;

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
        if (record instanceof TripRecord) {
          TripRecord tripRecord = (TripRecord) record;
          int tripType = tripRecord.getTripType();
          if (tripType == 2 || tripType == 3 || tripType == 4) {
            continue; // deadhead or to/from depot
          }
          String code = tripRecord.getSignCode();
          TripIdentifier id = new TripIdentifier(tripRecord);
          List<Trip> trips = tripsByIdentifier.get(id);
          if (trips == null) {
            
            // trip in stif but not in gtfs
            throw new RuntimeException("trip not found for " + id);
          }
          
          List<Trip> filtered = new ArrayList<Trip>();
          /* filter trips by schedule */
          for (Trip trip : trips) { 
            /* Service codes are of the form
            20100627CA
            Only the last two characters are important.
            They have the meaning: 
            A = sat
            B = weekday closed
            C = weekday open
            D = sun

            The first character is for trips on that day's STIF schedule, while the second 
            character is for trips on the next day's STIF schedule (but that run on that day).
            
            To figure out whether a GTFS trip corresponds to a STIF trip,
            if the STIF trip is before midnight, check daycode1; else check daycode2
            */
            
            String serviceId = trip.getServiceId().getId();
            char dayCode1 = serviceId.charAt(serviceId.length() - 2);
            char dayCode2 = serviceId.charAt(serviceId.length() - 1);
            
            //schedule runs on on days where a dayCode1 is followed by a dayCode2;
            //contains all trips from dayCode1, and pre-midnight trips for dayCode2;
            
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
          
          List<Trip> sctrips = tripsBySignCode.get(code);
          if (sctrips == null) {
            sctrips = new ArrayList<Trip>();
            tripsBySignCode.put(code, sctrips);
          }
          sctrips.addAll(filtered);
        }

      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void run() {
    // TODO Auto-generated method stub
    
  }
}
