package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ReliefState;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.NycRefreshableResources;
import org.onebusaway.nyc.transit_data_federation.model.RunData;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RunServiceImpl implements RunService {
  private Logger _log = LoggerFactory.getLogger(RunServiceImpl.class);

  private NycFederatedTransitDataBundle _bundle;

  private Map<AgencyAndId, RunData> runDataByTrip;

  private TransitGraphDao transitGraph;

  private HashMap<String, List<RunTripEntry>> entriesByRun;

  private BlockCalendarService blockCalendarService;

  private ScheduledBlockLocationService scheduledBlockLocationService;

  private Map<AgencyAndId, List<RunTripEntry>> entriesByTrip;

  @Autowired
  public void setBundle(NycFederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }

  @Autowired
  public void setTransitGraph(TransitGraphDao graph) {
    this.transitGraph = graph;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService bcs) {
    this.blockCalendarService = bcs;
  }

  @Autowired
  public void setScheduledBlockLocationService(
      ScheduledBlockLocationService service) {
    this.scheduledBlockLocationService = service;
  }

  @PostConstruct
  @Refreshable(dependsOn = NycRefreshableResources.RUN_DATA)
  public void setup() throws IOException, ClassNotFoundException {
    File path = _bundle.getTripRunDataPath();

    if (path.exists()) {
      _log.info("loading trip run data");
      runDataByTrip = ObjectSerializationLibrary.readObject(path);
    } else
      return;

    transformRunData();
  }

  public void transformRunData() {
    entriesByRun = new HashMap<String, List<RunTripEntry>>();
    entriesByTrip = new HashMap<AgencyAndId, List<RunTripEntry>>();

    for (Map.Entry<AgencyAndId, RunData> entry : runDataByTrip.entrySet()) {
      TripEntry trip = transitGraph.getTripEntryForId(entry.getKey());
      if (trip != null) {
        RunData runData = entry.getValue();

        ReliefState initialReliefState = runData.hasRelief() ? ReliefState.BEFORE_RELIEF
            : ReliefState.NO_RELIEF;
        processTripEntry(trip, runData.initialRun, runData.reliefTime,
            initialReliefState);
        if (runData.hasRelief()) {
          processTripEntry(trip, runData.reliefRun, runData.reliefTime,
              ReliefState.AFTER_RELIEF);
        }
      } else {
        _log.warn("null trip found for entry=" + entry.toString());
      }
    }
    // sort RTEs by time
    for (List<RunTripEntry> entries : entriesByRun.values()) {
      Collections.sort(entries);
    }
  }

  private void processTripEntry(TripEntry trip, String run, int reliefTime,
      ReliefState relief) {

    // add to index by run
    List<RunTripEntry> entries = entriesByRun.get(run);
    if (entries == null) {
      entries = new ArrayList<RunTripEntry>();
      entriesByRun.put(run, entries);
    }
    RunTripEntry rte = new RunTripEntry(trip, run, reliefTime, relief);
    entries.add(rte);

    // add to index by trip
    AgencyAndId tripId = trip.getId();
    entries = entriesByTrip.get(tripId);
    if (entries == null) {
      entries = new ArrayList<RunTripEntry>(2);
      entriesByTrip.put(tripId, entries);
    }
    entries.add(rte);
  }

  @Override
  public String getInitialRunForTrip(AgencyAndId trip) {
    RunData runData = runDataByTrip.get(trip);
    if (runData == null) {
      return null;
    }
    return runData.initialRun;
  }

  @Override
  public String getReliefRunForTrip(AgencyAndId trip) {
    RunData runData = runDataByTrip.get(trip);
    if (runData == null) {
      return null;
    }
    return runData.reliefRun;
  }

  @Override
  public int getReliefTimeForTrip(AgencyAndId trip) {
    RunData runData = runDataByTrip.get(trip);
    return (runData == null || runData.reliefRun == null) ? 0
        : runData.reliefTime;
  }

  @Override
  public List<RunTripEntry> getRunTripEntriesForRun(String runId) {
    return entriesByRun.get(runId);
  }

  @Override
  public RunTripEntry getRunTripEntryForRunAndTime(AgencyAndId runAgencyAndId,
      long time) {

    String runId = runAgencyAndId.getId();
    for (RunTripEntry entry : entriesByRun.get(runId)) {
      // all the trips for this run
      BlockEntry block = entry.getTripEntry().getBlock();
      long timeFrom = time - 30 * 60 * 1000;
      long timeTo = time + 30 * 60 * 1000;
      List<BlockInstance> activeBlocks = blockCalendarService.getActiveBlocks(
          block.getId(), timeFrom, timeTo);
      for (BlockInstance blockInstance : activeBlocks) {
        long serviceDate = blockInstance.getServiceDate();
        int scheduleTime = (int) ((time - serviceDate) / 1000);
        BlockConfigurationEntry blockConfig = blockInstance.getBlock();

        ScheduledBlockLocation blockLocation = scheduledBlockLocationService
            .getScheduledBlockLocationFromScheduledTime(blockConfig,
                scheduleTime);

        if (blockLocation == null)
          continue;

        BlockTripEntry trip = blockLocation.getActiveTrip();
        List<RunTripEntry> bothTrips = entriesByTrip
            .get(trip.getTrip().getId());

        if (bothTrips == null || bothTrips.isEmpty())
          continue;
        RunTripEntry firstTrip = bothTrips.get(0);
        if (bothTrips.size() == 1) {
          return firstTrip;
        } else {
          RunTripEntry secondTrip = bothTrips.get(1);
          if (secondTrip.getStartTime() < scheduleTime) {
            return secondTrip;
          }
          return firstTrip;
        }
      }
    }
    return null;
  }

  @Override
  public List<RunTripEntry> getRunTripEntriesForTime(String agencyId, long time) {
    ArrayList<RunTripEntry> out = new ArrayList<RunTripEntry>();
    List<BlockInstance> activeBlocks = blockCalendarService
        .getActiveBlocksForAgencyInTimeRange(agencyId, time, time);
    for (BlockInstance blockInstance : activeBlocks) {
      long serviceDate = blockInstance.getServiceDate();
      int scheduleTime = (int) ((time - serviceDate) / 1000);
      BlockConfigurationEntry blockConfig = blockInstance.getBlock();

      ScheduledBlockLocation blockLocation = scheduledBlockLocationService
          .getScheduledBlockLocationFromScheduledTime(blockConfig, scheduleTime);

      BlockTripEntry trip = blockLocation.getActiveTrip();
      List<RunTripEntry> rtes = entriesByTrip.get(trip.getTrip().getId());

      if (rtes == null)
        continue;

      out.addAll(rtes);
    }

    return out;
  }

  @Override
  public RunTripEntry getPreviousEntry(RunTripEntry before) {
    List<RunTripEntry> entries = entriesByRun.get(before.getRun());
    RunTripEntry prev = null;
    for (RunTripEntry entry : entries) {
      if (entry == before) {
        return prev;
      }
      prev = entry;
    }
    return null;
  }

  @Override
  public RunTripEntry getNextEntry(RunTripEntry after) {
    List<RunTripEntry> entries = entriesByRun.get(after.getRun());
    boolean found = false;
    for (RunTripEntry entry : entries) {
      if (found) {
        return entry;
      }
      if (entry == after) {
        found = true;
      }
    }
    return null;
  }

  public void setRunDataByTrip(Map<AgencyAndId, RunData> runDataByTrip) {
    this.runDataByTrip = runDataByTrip;
  }

  @Override
  public RunTripEntry getRunTripEntryForBlockInstance(
      BlockInstance blockInstance, int scheduleTime) {

    long serviceDate = blockInstance.getServiceDate();

    ScheduledBlockLocation blockLocation = scheduledBlockLocationService
        .getScheduledBlockLocationFromScheduledTime(blockInstance.getBlock(),
            scheduleTime);
    
    if (blockLocation == null) {
      _log.error("no scheduled block location for block=" + blockInstance + ", scheduleTime="+scheduleTime);
      return null;
    }

    BlockTripEntry trip = blockLocation.getActiveTrip();

    AgencyAndId tripId = trip.getTrip().getId();

    String runId = getInitialRunForTrip(tripId);

    int reliefTime = getReliefTimeForTrip(tripId);

    // if there is a relief and our schedule time is past the relief
    // then use that run
    if (reliefTime > 0 && reliefTime <= scheduleTime)
      runId = getReliefRunForTrip(tripId);

    long timestamp = serviceDate + scheduleTime * 1000;

    RunTripEntry runTrip = getRunTripEntryForRunAndTime(
        new AgencyAndId(tripId.getAgencyId(), runId), timestamp);

    return runTrip;
  }

  // TODO FIXME these methods don't require this much effort. they can
  // be pre-computed in setup(), or earlier.
  @Override
  public List<ScheduledBlockLocation> getSchedBlockLocsForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp) {

    BlockEntry block = runTrip.getTripEntry().getBlock();
    long timeFrom = timestamp - 30 * 60 * 1000;
    long timeTo = timestamp + 30 * 60 * 1000;
    List<BlockInstance> activeBlocks = blockCalendarService.getActiveBlocks(
        block.getId(), timeFrom, timeTo);
    /*
     * currently, we add any matching schedBlockLocs, but there should only be
     * one for a given timestamp
     */
    List<ScheduledBlockLocation> matchingSchedBlockLocs = new ArrayList<ScheduledBlockLocation>();
    for (BlockInstance blockInstance : activeBlocks) {
      long serviceDate = blockInstance.getServiceDate();
      int scheduleTime = (int) ((timestamp - serviceDate) / 1000);
      BlockConfigurationEntry blockConfig = blockInstance.getBlock();

      ScheduledBlockLocation blockLocation = scheduledBlockLocationService
          .getScheduledBlockLocationFromScheduledTime(blockConfig, scheduleTime);

      if (blockLocation == null)
        continue;

      BlockTripEntry trip = blockLocation.getActiveTrip();
      List<RunTripEntry> bothTrips = entriesByTrip.get(trip.getTrip().getId());

      if (bothTrips == null || bothTrips.isEmpty())
        continue;
      RunTripEntry firstTrip = bothTrips.get(0);
      if (bothTrips.size() == 1) {
        if (firstTrip.equals(runTrip))
          matchingSchedBlockLocs.add(blockLocation);
      } else {
        RunTripEntry secondTrip = bothTrips.get(1);
        if (secondTrip.getStartTime() < scheduleTime) {
          if (secondTrip.equals(runTrip))
            matchingSchedBlockLocs.add(blockLocation);
        }

        if (firstTrip.equals(runTrip))
          matchingSchedBlockLocs.add(blockLocation);
      }
    }

    return matchingSchedBlockLocs;

  }

  @Override
  public List<BlockInstance> getBlockInstancesForRunTripEntry(RunTripEntry rte, long timestamp) {
    // TODO Auto-generated method stub
    BlockEntry block = rte.getTripEntry().getBlock();
    long timeFrom = timestamp - 30 * 60 * 1000;
    long timeTo = timestamp + 30 * 60 * 1000;
    List<BlockInstance> activeBlocks = blockCalendarService.getActiveBlocks(
        block.getId(), timeFrom, timeTo);
    return activeBlocks;
  }

}
