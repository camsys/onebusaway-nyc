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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RunData;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Load STIF data, including the mapping between destination sign codes and trip
 * ids, into the database
 * 
 * @author bdferris
 * 
 */
public class StifTask implements Runnable {

  private Logger _log = LoggerFactory.getLogger(StifTask.class);

  private GtfsMutableRelationalDao _gtfsMutableRelationalDao;

  private List<File> _stifPaths = new ArrayList<File>();

  private Set<String> _notInServiceDscs = new HashSet<String>();

  private File _notInServiceDscPath;

  @Autowired 
  private NycFederatedTransitDataBundle _bundle;

  private String logPath;

  private boolean fallBackToStifBlocks = false;

  private MultiCSVLogger csvLogger;

  private HashMap<String, Set<AgencyAndId>> routeIdsByDsc = new HashMap<String, Set<AgencyAndId>>();

  @Autowired
  public void setGtfsMutableRelationalDao(
      GtfsMutableRelationalDao gtfsMutableRelationalDao) {
    _gtfsMutableRelationalDao = gtfsMutableRelationalDao;
  }

  /**
   * The path of the directory containing STIF files to process
   */
  public void setStifPath(File path) {
    _stifPaths.add(path);
  }

  public void setStifPaths(List<File> paths) {
    _stifPaths.addAll(paths);
  }

  public void setNotInServiceDsc(String notInServiceDsc) {
    _notInServiceDscs.add(notInServiceDsc);
  }

  public void setNotInServiceDscs(List<String> notInServiceDscs) {
    _notInServiceDscs.addAll(notInServiceDscs);
  }

  public void setNotInServiceDscPath(File notInServiceDscPath) {
    _notInServiceDscPath = notInServiceDscPath;
  }

  public void setLogPath(String logPath) {
    this.logPath = logPath;
  }
  
  public void run() {

    csvLogger = new MultiCSVLogger(logPath);

    StifTripLoader loader = new StifTripLoader();
    loader.setGtfsDao(_gtfsMutableRelationalDao);
    loader.setLogger(csvLogger);

    for (File path : _stifPaths) {
      loadStif(path, loader);
    }

    computeBlocksFromRuns(loader);
    warnOnMissingTrips();

    if (fallBackToStifBlocks) {
      loadStifBlocks(loader);
    }

    Map<AgencyAndId, RunData> runsForTrip = loader.getRunsForTrip();
    //store trip-run mapping in bundle
    try {
      ObjectSerializationLibrary.writeObject(_bundle.getTripRunDataPath(),
          runsForTrip);
    } catch (IOException e) {
          throw new IllegalStateException(e);
    }

    Map<String, List<AgencyAndId>> dscToTripMap = loader.getTripMapping();
    Map<AgencyAndId, String> tripToDscMap = new HashMap<AgencyAndId, String>();

    Set<String> inServiceDscs = new HashSet<String>();

    logDSCStatistics(dscToTripMap, tripToDscMap);

    int withoutMatch = loader.getTripsWithoutMatchCount();
    int total = loader.getTripsCount();

    _log.info("stif trips without match: " + withoutMatch + " / " + total);

    readNotInServiceDscs();

    serializeDSCData(dscToTripMap, tripToDscMap, inServiceDscs);

    csvLogger.summarize();
    csvLogger = null; //ensure no writes after summary
  }

  private void logDSCStatistics(Map<String, List<AgencyAndId>> dscToTripMap,
      Map<AgencyAndId, String> tripToDscMap) {
    csvLogger.header("dsc_statistics.csv", "dsc,number_of_trips_in_stif,number_of_distinct_route_ids_in_gtfs");
    for (Map.Entry<String, List<AgencyAndId>> entry : dscToTripMap.entrySet()) {
      String destinationSignCode = entry.getKey();
      List<AgencyAndId> tripIds = entry.getValue();
      for (AgencyAndId tripId : tripIds) {
    	 tripToDscMap.put(tripId, destinationSignCode);
      }

      Set<AgencyAndId> routeIds = routeIdsByDsc.get(destinationSignCode);
      if (routeIds != null) {
	  csvLogger.log("dsc_statistics.csv", destinationSignCode, tripIds.size(), routeIds.size());
      } else {
	  csvLogger.log("dsc_statistics.csv", destinationSignCode, tripIds.size(), 0);
      }
    }
  }

  private void serializeDSCData(Map<String, List<AgencyAndId>> dscToTripMap,
      Map<AgencyAndId, String> tripToDscMap, Set<String> inServiceDscs) {
    for (String notInServiceDsc : _notInServiceDscs) {
      if (inServiceDscs.contains(notInServiceDsc))
        _log.warn("overlap between in-service and not-in-service dscs: "
            + notInServiceDsc);

      // clear out trip mappings for out of service DSCs
      dscToTripMap.put(notInServiceDsc, new ArrayList<AgencyAndId>());
    }

    try {
        ObjectSerializationLibrary.writeObject(_bundle.getNotInServiceDSCs(), 
        		_notInServiceDscs);

        ObjectSerializationLibrary.writeObject(_bundle.getTripsForDSCIndex(), 
        		tripToDscMap);

      ObjectSerializationLibrary.writeObject(_bundle.getDSCForTripIndex(),
          dscToTripMap);
    } catch (IOException e) {
      throw new IllegalStateException("error serializing DSC/STIF data", e);
    }
  }

  private void loadStifBlocks(StifTripLoader loader) {

    Map<Trip, RawRunData> rawData = loader.getRawRunDataByTrip();
    for (Map.Entry<Trip, RawRunData> entry : rawData.entrySet()) {
      Trip trip = entry.getKey();
      if (trip.getBlockId() == null || trip.getBlockId().length() == 0) {
        RawRunData data = entry.getValue();
        trip.setBlockId(trip.getServiceId() + "_STIF_" + data.getDepotCode() + "_" + data.getBlock());
        _gtfsMutableRelationalDao.updateEntity(trip);
      }
    }
  }

  class TripWithStartTime implements Comparable<TripWithStartTime> {

    private int startTime;
    private Trip trip;

    public TripWithStartTime(Trip trip) {
      this.trip = trip;
      List<StopTime> stopTimes = _gtfsMutableRelationalDao.getStopTimesForTrip(trip);
      startTime = stopTimes.get(0).getDepartureTime();
    }

    // this is just for creating bogus objects for searching
    public TripWithStartTime(int startTime) {
      this.startTime = startTime;
    }

    @Override
    public int compareTo(TripWithStartTime o) {
      return startTime - o.startTime;
    }

    public String toString() {
      return "TripWithStartTime(" + startTime + ", " + trip + ")";
    }

  }

  @SuppressWarnings("rawtypes")
  class RawTripComparator implements Comparator {
    @Override
    public int compare(Object o1, Object o2) {
      if (o1 instanceof Integer) {
        if (o2 instanceof Integer) {
          return ((Integer) o1) - ((Integer) o2);
        } else {
          RawTrip trip = (RawTrip) o2;
          return ((Integer) o1) - trip.listedFirstStopTime;
        }
      } else {
        if (o2 instanceof Integer) {
          return ((RawTrip) o1).listedFirstStopTime - ((Integer) o2);
        } else {
          RawTrip trip = (RawTrip) o2;
          return ((RawTrip) o1).listedFirstStopTime - trip.listedFirstStopTime;
        }
      }
    }
  }

  private void computeBlocksFromRuns(StifTripLoader loader) {

    int blockNo = 0;

    HashSet<Trip> usedGtfsTrips = new HashSet<Trip>();

    csvLogger.header("non_pullin_without_next_movement.csv", "stif_trip,stif_filename,stif_trip_record_line_num");
    csvLogger.header(
        "stif_trips_without_pullout.csv",
        "stif_trip,stif_filename,stif_trip_record_line_num,gtfs_trip_id,synthesized_block_id");
    csvLogger.header("matched_trips_gtfs_stif.csv", "blockId,tripId,dsc,firstStop,firstStopTime,lastStop,lastStopTime,"+
        "runId,reliefRunId,recoveryTime,firstInSeq,lastInSeq,signCodeRoute");

    Map<ServiceCode, List<RawTrip>> rawData = loader.getRawStifData();
    for (Map.Entry<ServiceCode, List<RawTrip>> entry : rawData.entrySet()) {
      List<RawTrip> rawTrips = entry.getValue();
      // this is a monster -- we want to group these by run and find the
      // pullouts
      HashMap<String, List<RawTrip>> tripsByRun = new HashMap<String, List<RawTrip>>();
      HashSet<RawTrip> unmatchedTrips = new HashSet<RawTrip>();
      ArrayList<RawTrip> pullouts = new ArrayList<RawTrip>();
      for (RawTrip trip : rawTrips) {
        List<RawTrip> byRun = tripsByRun.get(trip.runId);
        if (byRun == null) {
          byRun = new ArrayList<RawTrip>();
          tripsByRun.put(trip.runId, byRun);
        }
        unmatchedTrips.add(trip);
        byRun.add(trip);
        if (trip.type == StifTripType.PULLOUT) {
          pullouts.add(trip);
        }
      }
      for (List<RawTrip> byRun : tripsByRun.values()) {
        Collections.sort(byRun);
      }

      for (RawTrip pullout : pullouts) {
        blockNo ++;
        RawTrip lastTrip = pullout;
        int i = 0;
        HashSet<String> blockIds = new HashSet<String>();
        while (lastTrip.type != StifTripType.PULLIN) {

          unmatchedTrips.remove(lastTrip);
          if (++i > 200) {
            _log.warn("We seem to be caught in an infinite loop; this is usually caused\n"
                + "by two trips on the same run having the same start time.  Since nobody\n"
                + "can be in two places at once, this is an error in the STIF.  Some trips\n"
                + "will end up with missing blocks and the log will be screwed up.  A \n"
                + "representative trip starts at "
                + lastTrip.firstStop
                + " at " + lastTrip.firstStopTime + " on " + lastTrip.runId + " on " + lastTrip.serviceCode);
            break;
          }
          if (lastTrip.nextRun == null) {
            csvLogger.log("non_pullin_without_next_movement.csv", lastTrip.id, lastTrip.path, lastTrip.lineNumber); 

            _log.warn("A non-pullin has no next run; some trips will end up with missing blocks"
                    + " and the log will be messed up. The bad trip starts at " + lastTrip.firstStop + " at "
                    + lastTrip.firstStopTime + " on " + lastTrip.runId + " on " + lastTrip.serviceCode);
            break;
          }

          List<RawTrip> trips = tripsByRun.get(lastTrip.nextRun);
          if (trips == null) {
            _log.warn("No trips for run " + lastTrip.nextRun);
            break;
          }

          int nextTripStartTime = lastTrip.listedLastStopTime + lastTrip.recoveryTime * 60;
          @SuppressWarnings("unchecked")
          int index = Collections.binarySearch(trips, nextTripStartTime, new RawTripComparator());

          if (index < 0) {
            index = -(index + 1);
          }
          if (index >= trips.size()) {
            _log.warn("The preceding trip says that the run "
                + lastTrip.nextRun
                + " is next, but there are no trips after "
                + lastTrip.firstStopTime
                + ", so some trips will end up with missing blocks.");
            break;
          }

          RawTrip trip = trips.get(index);
          if (trip == lastTrip) {
            index ++;
            if (index >= trips.size()) {
              _log.warn("The preceding trip says that the run "
                  + lastTrip.nextRun
                  + " is next, but there are no trips after "
                  + lastTrip.firstStopTime
                  + ", so some trips will end up with missing blocks (the log may"
                  + " also be incorrect.");
              break;
            }
            trip = trips.get(index);
          }
          lastTrip = trip;
          for (Trip gtfsTrip : lastTrip.getGtfsTrips()) {
            RawRunData rawRunData = loader.getRawRunDataByTrip().get(gtfsTrip);
            String blockId = gtfsTrip.getServiceId() + "_" + rawRunData.getDepotCode() + "_" + pullout.firstStopTime + "_" + pullout.runId + "_" + blockNo;

            blockId = blockId.intern();
            blockIds.add(blockId);
            gtfsTrip.setBlockId(blockId);
            _gtfsMutableRelationalDao.updateEntity(gtfsTrip);

            AgencyAndId routeId = gtfsTrip.getRoute().getId();
            addToMapSet(routeIdsByDsc, trip.getDsc(), routeId);
            dumpBlockDataForTrip(trip, gtfsTrip.getId().getId(), blockId, routeId.getId());

            usedGtfsTrips.add(gtfsTrip);
          }
          if (lastTrip.type == StifTripType.DEADHEAD) {
            for (String blockId : blockIds) {
              dumpBlockDataForTrip(lastTrip, "deadhead", blockId, "no gtfs trip");
            }
          }
        }
        unmatchedTrips.remove(lastTrip);

        for (String blockId : blockIds) {
          dumpBlockDataForTrip(pullout, "pullout", blockId, "no gtfs trip");
          dumpBlockDataForTrip(lastTrip, "pullin", blockId, "no gtfs trip");
        }
      }

      for (RawTrip trip : unmatchedTrips) {
        _log.warn("STIF trip: " + trip + " on schedule " + entry.getKey()
            + " trip type " + trip.type
            + " must not have an associated pullout");
        for (Trip gtfsTrip : trip.getGtfsTrips()) {
          blockNo++;
          String blockId = gtfsTrip.getServiceId() + "_" + trip.firstStop + "_"
              + trip.firstStopTime + "_" + trip.runId.replace("-", "_")
              + "_orphan_" + blockNo;
          _log.warn("Generating single-trip block id for GTFS trip: "
              + gtfsTrip.getId() + " : " + blockId);
          gtfsTrip.setBlockId(blockId);
          dumpBlockDataForTrip(trip, gtfsTrip.getId().getId(), blockId, gtfsTrip.getBlockId());
          csvLogger.log("stif_trips_without_pullout.csv", trip.id, trip.path,
              trip.lineNumber, gtfsTrip.getId(), blockId);
          usedGtfsTrips.add(gtfsTrip);
        }
      }
    }

    HashSet<Route> routesWithTrips = new HashSet<Route>();
    csvLogger.header("gtfs_trips_with_no_stif_match.csv", "gtfs_trip_id,stif_trip");
    Collection<Trip> allTrips = _gtfsMutableRelationalDao.getAllTrips();
    for (Trip trip : allTrips) {
      if (usedGtfsTrips.contains(trip)) {
        routesWithTrips.add(trip.getRoute());
      } else {
        csvLogger.log("gtfs_trips_with_no_stif_match.csv", trip.getId(), loader.getSupport().getTripAsIdentifier(trip));
      }
    }
    
    csvLogger.header("route_ids_with_no_trips.csv", "agency_id,route_id");
    for (Route route : _gtfsMutableRelationalDao.getAllRoutes()) {
      if (routesWithTrips.contains(route)) {
        continue;
      }
      csvLogger.log("route_ids_with_no_trips.csv", route.getId().getAgencyId(), route.getId().getId());
    }
  }

  /**
   * An extremely common pattern: add an item to a set in a hash value, creating that set if
   * necessary; based on code from OTP with permission of copyright holder (OpenPlans). 
   */
  public static final <T, U> void addToMapSet(Map<T, Set<U>> mapList, T key, U value) {
      Set<U> list = mapList.get(key);
      if (list == null) {
          list = new HashSet<U>();
          mapList.put(key, list);
      }
      list.add(value);
  }


  /**
   * Dump some raw block matching data to a CSV file from stif trips
   */
  private void dumpBlockDataForTrip(RawTrip trip,
      String tripId, String blockId, String routeId) {

    csvLogger.log("matched_trips_gtfs_stif.csv", blockId, tripId,
        trip.getDsc(), trip.firstStop, trip.firstStopTime, trip.lastStop,
        trip.lastStopTime, trip.runId, trip.reliefRunId, trip.recoveryTime,
        trip.firstTripInSequence, trip.lastTripInSequence,
        trip.getSignCodeRoute(), routeId);
  }

  private void warnOnMissingTrips() {
    for (Trip t : _gtfsMutableRelationalDao.getAllTrips()) {
      String blockId = t.getBlockId();
      if (blockId == null || blockId.equals("")) {
        _log.warn("When matching GTFS to STIF, failed to find block in STIF for "
            + t.getId());
      }
    }
  }

  public void loadStif(File path, StifTripLoader loader) {
    // Exclude files and directories like .svn
    if (path.getName().startsWith("."))
      return;

    if (path.isDirectory()) {
      for (String filename : path.list()) {
        File contained = new File(path, filename);
        loadStif(contained, loader);
      }
    } else {
      loader.run(path);
    }
  }

  private void readNotInServiceDscs() {
    if (_notInServiceDscPath != null) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(
            _notInServiceDscPath));
        String line = null;
        while ((line = reader.readLine()) != null)
          _notInServiceDscs.add(line);
      } catch (IOException ex) {
        throw new IllegalStateException("unable to read nonInServiceDscPath: "
            + _notInServiceDscPath);
      }
    }
  }

  /**
   * Whether blocks should come be computed from runs (true) or read from the STIF (false)
   * @return
   */
  public boolean usesFallBackToStifBlocks() {
    return fallBackToStifBlocks;
  }

  public void setFallBackToStifBlocks(boolean fallBackToStifBlocks) {
    this.fallBackToStifBlocks = fallBackToStifBlocks;
  }
}
