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
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.EventRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.GeographyRecord;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.SignCodeRecord;
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

  private MultiCSVLogger csvLogger;

  private Map<DuplicateTripCheckKey, RawTrip> tripsByRunAndStartTime = new HashMap<DuplicateTripCheckKey, RawTrip>();

  class DuplicateTripCheckKey {
    public DuplicateTripCheckKey(String runId, int startTime,
        ServiceCode serviceCode) {
      this.runId = runId;
      this.startTime = startTime;
      this.serviceCode = serviceCode;
    }

    String runId;
    Integer startTime;
    ServiceCode serviceCode;
  }

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
      run(in, path);
    } catch (Exception e) {
      throw new RuntimeException("Error loading " + path, e);
    }
  }

  public void run(InputStream stream, File path) {
    StifRecordReader reader;

    boolean warned = false;
    int lineNumber = 0;
    int tripLineNumber = 0;
    TripRecord tripRecord = null;
    EventRecord eventRecord = null;
    EventRecord firstEventRecord = null;
    try {
      reader = new StifRecordReader(stream);
      ServiceCode serviceCode = null;
      while (true) {
        StifRecord record = reader.read();
        lineNumber++;

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

        if (record instanceof EventRecord) {
          // track the first and last event records which are revenue stops
          // for use in trip processing
          if (tripRecord == null) {
            continue;
          }
          EventRecord possibleEventRecord = (EventRecord) record;
          if (!possibleEventRecord.isRevenue()) {
            // skip non-revenue stops
            continue;
          }
          eventRecord = possibleEventRecord;
          if (firstEventRecord == null) {
            firstEventRecord = eventRecord;
          }
        }

        if (record instanceof TripRecord || record instanceof SignCodeRecord) {
          // the SignCodeRecord is guaranteed by spec to be after all
          // TripRecords
          // So, for each trip's worth of event records, a SignCode record or a
          // TripRecord will follow.
          // this code parses the trip record whose events end on the line
          // *before* this.
          if (tripRecord == null) {
            // we have already finished with this trip
            if (record instanceof TripRecord) {
              // prepare for next trip
              tripLineNumber = lineNumber;
              tripRecord = (TripRecord) record;
              eventRecord = firstEventRecord = null;
            } else {
              tripRecord = null;
            }
            continue;
          }
          int tripType = tripRecord.getTripType();
          if (firstEventRecord == null) {
            // this must be a non-revenue trip
            assert (tripType == 2 || tripType == 3 || tripType == 4);

            RawTrip rawTrip = new RawTrip(tripRecord.getRunId(),
                tripRecord.getReliefRunId(),
                tripRecord.getNextTripOperatorRunId(),
                StifTripType.byValue(tripType), tripRecord.getSignCode());
            rawTrip.serviceCode = serviceCode;
            rawTrip.firstStopTime = tripRecord.getOriginTime();
            rawTrip.lastStopTime = tripRecord.getDestinationTime();
            rawTrip.listedFirstStopTime = tripRecord.getOriginTime();
            rawTrip.listedLastStopTime = tripRecord.getDestinationTime();
            rawTrip.firstStop = support.getStopIdForLocation(tripRecord.getOriginLocation());
            rawTrip.lastStop = support.getStopIdForLocation(tripRecord.getDestinationLocation());
            rawTrip.recoveryTime = tripRecord.getRecoveryTime();
            rawTrip.firstTripInSequence = tripRecord.isFirstTripInSequence();
            rawTrip.lastTripInSequence = tripRecord.isLastTripInSequence();
            rawTrip.signCodeRoute = tripRecord.getSignCodeRoute();
            rawTrip.path = path;
            rawTrip.lineNumber = tripLineNumber;
            rawData.get(serviceCode).add(rawTrip);

            if (record instanceof TripRecord) {
              tripLineNumber = lineNumber;
              tripRecord = (TripRecord) record;
              eventRecord = firstEventRecord = null;
            } else {
              tripRecord = null;
            }
            continue;

          } else {
            assert (tripType != 2 && tripType != 3 && tripType != 4);
          }

          String run1 = tripRecord.getRunId();
          String run2 = tripRecord.getReliefRunId();
          String run3 = tripRecord.getNextTripOperatorRunId();

          RawTrip rawTrip = new RawTrip(tripRecord.getRunId(),
              tripRecord.getReliefRunId(),
              tripRecord.getNextTripOperatorRunId(),
              StifTripType.byValue(tripType), tripRecord.getSignCode());
          rawTrip.serviceCode = serviceCode;
          rawTrip.firstStopTime = firstEventRecord.getTime();
          rawTrip.lastStopTime = eventRecord.getTime();
          rawTrip.firstStop = support.getStopIdForLocation(firstEventRecord.getLocation());
          rawTrip.lastStop = support.getStopIdForLocation(eventRecord.getLocation());
          rawTrip.listedFirstStopTime = tripRecord.getOriginTime();
          rawTrip.listedLastStopTime = tripRecord.getDestinationTime();
          rawTrip.recoveryTime = tripRecord.getRecoveryTime();
          rawTrip.firstTripInSequence = tripRecord.isFirstTripInSequence();
          rawTrip.lastTripInSequence = tripRecord.isLastTripInSequence();
          rawTrip.signCodeRoute = tripRecord.getSignCodeRoute();
          rawTrip.path = path;
          rawTrip.lineNumber = tripLineNumber;
          rawData.get(serviceCode).add(rawTrip);

          String code = tripRecord.getSignCode();

          TripIdentifier id = support.getIdentifierForStifTrip(tripRecord,
              rawTrip);
          rawTrip.id = id;

          if (rawTrip.signCodeRoute == null
              || rawTrip.signCodeRoute.length() == 0) {
            csvLogger.log("stif_trip_layers_with_missing_route.csv", id, path,
                tripLineNumber, "signCodeRoute");
          }
          if (rawTrip.getDsc() == null || rawTrip.getDsc().length() == 0) {
            csvLogger.log("stif_trip_layers_with_missing_route.csv", id, path,
                tripLineNumber, "DSC");
          }

          DuplicateTripCheckKey key = new DuplicateTripCheckKey(rawTrip.runId,
              rawTrip.firstStopTime, rawTrip.serviceCode);
          RawTrip oldTrip = tripsByRunAndStartTime.get(key);
          if (oldTrip == null) {
            tripsByRunAndStartTime.put(key, rawTrip);
          } else {
            csvLogger.log("trips_with_duplicate_run_and_start_time.csv", id,
                rawTrip.path, rawTrip.lineNumber, oldTrip.id, oldTrip.path,
                oldTrip.lineNumber);
          }

          List<Trip> trips = support.getTripsForIdentifier(id);

          _tripsCount++;

          if (trips == null || trips.isEmpty()) {
            csvLogger.log("stif_trips_with_no_gtfs_match.csv", id, path,
                tripLineNumber);

            // trip in stif but not in gtfs
            if (!warned) {
              warned = true;
              _log.warn("gtfs trip not found for " + id);
            }
            _tripsWithoutMatchCount++;
            if (record instanceof TripRecord) {
              // prepare for next trip
              tripLineNumber = lineNumber;
              tripRecord = (TripRecord) record;
              eventRecord = firstEventRecord = null;
            } else {
              tripRecord = null;
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
            Character dayCode1 = serviceId.charAt(serviceId.length() - 2);
            Character dayCode2 = serviceId.charAt(serviceId.length() - 1);

            // schedule runs on on days where a dayCode1 is followed by a
            // dayCode2;
            // contains all trips from dayCode1, and pre-midnight trips for
            // dayCode2;

            if (rawTrip.firstStopTime < 0) {
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

              rawTrip.addGtfsTrip(trip);

              int reliefTime = tripRecord.getReliefTime();
              String block = tripRecord.getBlockNumber();
              String depotCode = tripRecord.getDepotCode();
              RawRunData rawRunData = new RawRunData(run1, run2, run3,
                  serviceId, serviceCode, block, depotCode);

              filtered.add(trip);
              rawRunDataByTrip.put(trip, rawRunData);
              runsForTrip.put(trip.getId(), new RunData(run1, run2, reliefTime));

              if (rawTrip.type == StifTripType.REVENUE
                  && (code == null || code.length() == 0)) {
                _log.warn("Revenue trip " + rawTrip + " did not have a DSC");
                csvLogger.log("trips_with_null_dscs.csv", trip.getId(), id,
                    path, tripLineNumber);
              }
            }
          }
          if (filtered.size() == 0) {
            if (rawTrip.type == StifTripType.REVENUE
                && (code == null || code.length() == 0)) {
              _log.warn("Revenue trip " + rawTrip + " did not have a DSC");
              csvLogger.log("trips_with_null_dscs.csv", "(no GTFS trips)", id,
                  path, tripLineNumber);
            }
          }

          List<AgencyAndId> sctrips = tripIdsBySignCode.get(code);
          if (sctrips == null) {
            sctrips = new ArrayList<AgencyAndId>();
            tripIdsBySignCode.put(code, sctrips);
          }
          for (Trip trip : filtered)
            sctrips.add(trip.getId());
          tripRecord = null; // we are done processing this trip record
        }
        if (record instanceof TripRecord) {
          tripLineNumber = lineNumber;
          tripRecord = (TripRecord) record;
          eventRecord = firstEventRecord = null;
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

  public void setLogger(MultiCSVLogger csvLogger) {
    this.csvLogger = csvLogger;
    csvLogger.header("trips_with_null_dscs.csv",
        "gtfs_trip_id,stif_trip,stif_filename,stif_trip_record_line_num");
    csvLogger.header("stif_trips_with_no_gtfs_match.csv",
        "stif_trip,stif_filename,stif_trip_record_line_num");
    csvLogger.header("stif_trip_layers_with_missing_route.csv",
        "stif_trip,stif_filename,stif_trip_record_line_num,missing_field");
    csvLogger.header(
        "trips_with_duplicate_run_and_start_time.csv",
        "stif_trip1,stif_filename1,stif_trip_record_line_num1,stif_trip2,stif_filename2,stif_trip_record_line_num2");

  }

  public StifTripLoaderSupport getSupport() {
    return support;
  }

  public void setSupport(StifTripLoaderSupport support) {
    this.support = support;
  }
}
