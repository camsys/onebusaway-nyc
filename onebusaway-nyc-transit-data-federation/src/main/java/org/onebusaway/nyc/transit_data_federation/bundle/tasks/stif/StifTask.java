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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
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
import org.terracotta.agent.repkg.de.schlichtherle.io.FileOutputStream;

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
  
  private String tripsWoBlocksLogPath;

  private boolean fallBackToStifBlocks = false;

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
  
  public void setTripsWoBlocksLogPath(String tripsWoBlocksLogPath) {
	  this.tripsWoBlocksLogPath = tripsWoBlocksLogPath;
  }

  public void run() {

    StifTripLoader loader = new StifTripLoader();
    loader.setGtfsDao(_gtfsMutableRelationalDao);

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

    for (Map.Entry<String, List<AgencyAndId>> entry : dscToTripMap.entrySet()) {
      String destinationSignCode = entry.getKey();
      List<AgencyAndId> tripIds = entry.getValue();

      for (AgencyAndId tripId : tripIds) {
    	 tripToDscMap.put(tripId, destinationSignCode);
      }
    }

    int withoutMatch = loader.getTripsWithoutMatchCount();
    int total = loader.getTripsCount();

    _log.info("stif trips without match: " + withoutMatch + " / " + total);

    readNotInServiceDscs();

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
          return ((Integer) o1) - trip.firstStopTime;
        }
      } else {
        if (o2 instanceof Integer) {
          return ((RawTrip) o1).firstStopTime - ((Integer) o2);
        } else {
          RawTrip trip = (RawTrip) o2;
          return ((RawTrip) o1).firstStopTime - trip.firstStopTime;
        }
      }
    }
  }

  private void computeBlocksFromRuns(StifTripLoader loader) {

    FileOutputStream outputStream = null;
    PrintStream printStream = null;
    if (logPath != null) {
      try {
        outputStream = new FileOutputStream(new File(logPath));
        printStream = new PrintStream(outputStream);
        printStream.println("blockId,tripId,dsc,firstStop,firstStopTime,lastStop,lastStopTime,"+
        "runId,reliefRunId,recoveryTime,firstInSeq,lastInSeq,signCodeRoute");
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    int blockNo = 0;

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

          int nextTripStartTime = lastTrip.lastStopTime + lastTrip.recoveryTime;
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
                + ", so some trips will end up with missing blocks (the log may"
                + " also be incorrect.");
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

            dumpBlockDataForTrip(printStream, trip,
                gtfsTrip.getId().getId(), blockId);
          }
          if (lastTrip.type == StifTripType.DEADHEAD) {
            for (String blockId : blockIds) {
              dumpBlockDataForTrip(printStream, lastTrip, "deadhead", blockId);
            }
          }
        }
        unmatchedTrips.remove(lastTrip);

        for (String blockId : blockIds) {
          dumpBlockDataForTrip(printStream, pullout, "pullout", blockId);
          dumpBlockDataForTrip(printStream, lastTrip, "pullin", blockId);
        }
      }

      for (RawTrip trip : unmatchedTrips) {
        _log.warn("STIF trip: " + trip + " on schedule " + entry.getKey() + " trip type " + trip.type + " must not have an associated pullout");
        for (Trip gtfsTrip : trip.getGtfsTrips()) {
          blockNo++;
          String blockId = gtfsTrip.getServiceId() + "_" + trip.firstStop + "_" + trip.firstStopTime + "_" + trip.runId.replace("-","_") + "_orphan_" + blockNo;
          gtfsTrip.setBlockId(blockId);
        }
      }
    }
    
    if (outputStream != null) {
      try {
        outputStream.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    
    if (tripsWoBlocksLogPath != null) {
      FileOutputStream woBlocksOS = null;
      PrintStream woBlocksPrintStream = null;
      
      try {
        woBlocksOS = new FileOutputStream(new File(tripsWoBlocksLogPath));
        woBlocksPrintStream = new PrintStream(woBlocksOS);

        boolean header = true;
        for (Trip trip : _gtfsMutableRelationalDao.getAllTrips()) {
          if (trip.getBlockId() == null || trip.getBlockId().length() == 0) {
            if (header) {
              woBlocksPrintStream.println("Trips without blocks:");
              header = false;
            }
            woBlocksPrintStream.println(trip.getId().getId());
          }
        }
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      } finally {
        if (woBlocksOS != null) {
          try {
            woBlocksOS.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }

    }

  }

  /**
   * Dump some raw block matching data to a CSV file from stif trips
   * 
   * @param outputStream
   * @param rawRunDataByTrip
   * @param trip
   */
  private void dumpBlockDataForTrip(PrintStream printStream, RawTrip trip,
      String tripId, String blockId) {
    if (printStream == null) {
      return;
    }

    printStream.print(blockId);
    printStream.print(",");
    printStream.print(tripId);
    printStream.print(",");
    printStream.print(trip.getDsc());
    printStream.print(",");

    printStream.print(trip.firstStop);
    printStream.print(",");
    printStream.print(trip.firstStopTime);
    printStream.print(",");
    printStream.print(trip.lastStop);
    printStream.print(",");
    printStream.print(trip.lastStopTime);
    printStream.print(",");

    printStream.print(trip.runId);
    printStream.print(",");
    printStream.print(trip.reliefRunId);

    printStream.print(",");
    printStream.print(trip.recoveryTime);
    printStream.print(",");
    printStream.print(trip.firstTripInSequence);
    printStream.print(",");
    printStream.print(trip.lastTripInSequence);
    printStream.print(",");

    printStream.print(trip.getSignCodeRoute());
    printStream.print("\n");
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
  public boolean usesFallBackToStiffBlocks() {
    return fallBackToStifBlocks;
  }

  public void setFallBackToStifBlocks(boolean fallBackToStifBlocks) {
    this.fallBackToStifBlocks = fallBackToStifBlocks;
  }
}
