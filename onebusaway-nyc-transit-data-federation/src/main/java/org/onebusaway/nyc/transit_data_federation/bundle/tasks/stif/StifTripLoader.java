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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.GeographyRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.StifRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.TimetableRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.TripRecord;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RunData;

import org.opentripplanner.graph_builder.services.DisjointSet;
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

  private DisjointSet<String> blocks = new DisjointSet<String>();

  private OutputStream outputStream;
  
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
          support.putStopIdForLocation(geographyRecord.getIdentifier(),
              geographyRecord.getBoxID());
        }
        if (record instanceof TripRecord) {
          TripRecord tripRecord = (TripRecord) record;
          int tripType = tripRecord.getTripType();
          if (tripType == 2 || tripType == 3 || tripType == 4) {
            continue; // deadhead or to/from depot
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

              String run0 = tripRecord.getPreviousRunId();
              String run1 = tripRecord.getRunId();
              String run2 = tripRecord.getReliefRunId();
              String run3 = tripRecord.getNextTripOperatorRunId();

              int reliefTime = tripRecord.getReliefTime();
              RawRunData rawRunData = new RawRunData(run1, run2);

              logTrip(trip, run0, run1, run2, run3, reliefTime);
              
              filtered.add(trip);
              rawRunDataByTrip.put(trip, rawRunData);
              runsForTrip.put(trip.getId(), new RunData(run1, run2, reliefTime));
              if (run0 != null && run0.length() > 0) {
                blocks.union(run0 + serviceCode, run1 + serviceCode);
              }
              if (run3 != null && run3.length() > 0) {
                blocks.union(run1 + serviceCode, run3 + serviceCode);
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

  private void logTrip(Trip trip, String run0, String run1, String run2, String run3,
      int reliefTime) {

    if (outputStream == null) {
      return;
    }

    PrintStream printStream = new PrintStream(outputStream);

    printStream.print(trip.getId().getId());
    printStream.print(",");

    GtfsMutableRelationalDao dao = support.getGtfsDao();
    List<StopTime> stopTimesForTrip = dao.getStopTimesForTrip(trip);
    StopTime firstStopTime = stopTimesForTrip.get(0);
    Stop firstStop = firstStopTime.getStop();
    printStream.print(firstStop.getId());
    printStream.print(",");
    printStream.print(firstStopTime.getDepartureTime());
    printStream.print(",");

    StopTime lastStopTime = stopTimesForTrip.get(stopTimesForTrip.size() - 1);
    Stop lastStop = lastStopTime.getStop();
    printStream.print(lastStop.getId());
    printStream.print(",");
    printStream.print(lastStopTime.getArrivalTime());
    printStream.print(",");

    printStream.print(run0);
    printStream.print(",");
    printStream.print(run1);
    printStream.print(",");
    printStream.print(run2);
    printStream.print(",");
    printStream.print(run3);
    printStream.print("\n");
  }

  public Map<Trip, RawRunData> getRawRunDataByTrip() {
    return rawRunDataByTrip;
  }
  
  public DisjointSet<String> getBlocks() {
    return blocks;
  }

  public Map<AgencyAndId, RunData> getRunsForTrip() {
    return runsForTrip;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  public void setOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }
}
