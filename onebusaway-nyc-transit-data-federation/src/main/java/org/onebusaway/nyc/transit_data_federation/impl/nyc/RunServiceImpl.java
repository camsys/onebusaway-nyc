package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ReliefState;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.impl.bundle.NycRefreshableResources;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RunData;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
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

  private Map<String, List<RunTripEntry>> entriesByRunNumber;

  private BlockCalendarService blockCalendarService;

  private CalendarService calendarService;

  private ScheduledBlockLocationService scheduledBlockLocationService;

  private Map<AgencyAndId, List<RunTripEntry>> entriesByTrip;

  private ExtendedCalendarService extCalendarService;

  @Autowired
  public void setCalendarService(CalendarService calendarService) {
    this.calendarService = calendarService;
  }
  
  public ExtendedCalendarService getCalendarService() {
    return extCalendarService;
  }

  @Autowired
  public void setExtendedCalendarService(ExtendedCalendarService extCalendarService) {
    this.extCalendarService = extCalendarService;
  }

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
    entriesByRunNumber = new HashMap<String, List<RunTripEntry>>();

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

  private void processTripEntry(TripEntry trip, String runId, int reliefTime,
      ReliefState relief) {

    String[] runInfo = StringUtils.splitByWholeSeparator(runId, "-");
    String runNumber = null;
    String runRoute = null;

    // TODO FIXME we should be using RunID objects
    if (runInfo != null && runInfo.length > 0) {
      runRoute = runInfo[0];
      if (runInfo.length > 1)
        runNumber = runInfo[1];
    }

    // add to index by run
    List<RunTripEntry> entries = entriesByRun.get(runId);
    if (entries == null) {
      entries = new ArrayList<RunTripEntry>();
      entriesByRun.put(runId, entries);
    }
    List<RunTripEntry> entriesNum = entriesByRunNumber.get(runNumber);
    if (entriesNum == null) {
      entriesNum = new ArrayList<RunTripEntry>();
      entriesByRunNumber.put(runNumber, entriesNum);
    }
    RunTripEntry rte = new RunTripEntry(trip, runNumber, runRoute, reliefTime,
        relief);
    entries.add(rte);
    entriesNum.add(rte);

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

  /*
   * Try the reference string against two other strings and return
   * the first one with minimum Levenshtein distance.
   * NOTE: returns second argument by default!
   */
  private String getMinLevenshteinDist(String ref, String try1, String try2) {
    int firstTry = StringUtils.getLevenshteinDistance(try1, ref);
    int lastTry = StringUtils.getLevenshteinDistance(try2, ref);
    int minPart = Math.min(firstTry, lastTry);
    return (minPart == firstTry) ? try1 : try2;
  }
  
  @Override
  public TreeMap<Integer, List<RunTripEntry>> getRunTripEntriesForFuzzyIdAndTime(
      AgencyAndId runAgencyAndId, Set<BlockInstance> nearbyBlocks, long time) {

    /*
     * FIXME TODO really need to cache these search results...
     */
    TreeMap<Integer, List<RunTripEntry>> matchedRTEs = new TreeMap<Integer, List<RunTripEntry>>();

    /*
     * 1. check for exact match
     */
    RunTripEntry exactMatch = getActiveRunTripEntryForRunAndTime(runAgencyAndId, time);
    if (exactMatch != null) {
      List<RunTripEntry> lrtes = new ArrayList<RunTripEntry>();
      lrtes.add(exactMatch);
      matchedRTEs.put(0, lrtes);
      return matchedRTEs;
    }

    String[] runInfo = StringUtils.splitByWholeSeparator(
        runAgencyAndId.getId(), "-");
    String runNumber = null;
    String runRoute = null;
    // TODO FIXME we should be using RunID objects
    // so we don't do all this string work
    if (runInfo != null && runInfo.length > 0) {
      runRoute = runInfo[0];
      if (runRoute.length() != 3) {
        throw new IllegalArgumentException(
            "runRoute must be a three character numeric code: " + runRoute);
      }
      if (runInfo.length > 1) {
        runNumber = runInfo[1];
      }
    }

    String cutRunRoute = runRoute.substring(1);
    String fuzzyRunId = RunTripEntry.createId(runRoute, runNumber);

    /*
     * 2. rank route id levenshtein distance for nearby runTrips FIXME this is
     * for EXACT schedule time
     */
    for (BlockInstance binst : nearbyBlocks) {
      long serviceDate = binst.getServiceDate();
      int scheduleTime = (int) ((time - serviceDate) / 1000);
      RunTripEntry rte = getActiveRunTripEntryForBlockInstance(binst, scheduleTime);
      if (rte == null)
        continue;
      
      /*
       * we check and setup for the following cases:
       * 1. misc: the operator entered values are nearly arbitrary
       *  except for the cases of 000 & 111, which states as "common".
       * 2. route id - borough code > 2 characters: operator could
       *  enter any two numbers from the numeric part of the actual id
       * 3. route id - borough == 2: expected case; simply get dist. 
       */
      String thisRunId = null;
      if (StringUtils.equals(rte.getRunRoute(), "MISC")) {
        // TODO check consistency of this
        thisRunId = RunTripEntry.createId("0" + getMinLevenshteinDist(cutRunRoute, "00", "11"), rte.getRunNumber());
      } else if (rte.getRunRoute().length() == 5) {
        // FIXME this is a hack; it will favor 4 digit matches by default
        String borId = rte.getRunRoute().substring(0,1);
        String firstPart = rte.getRunRoute().substring(1, 3);
        String lastPart = rte.getRunRoute().substring(3);
        String minRes = getMinLevenshteinDist(cutRunRoute, firstPart, lastPart);
        thisRunId = RunTripEntry.createId(borId + minRes, rte.getRunNumber());
      } else {
        thisRunId = rte.getRunId();
      }

      int ldist = StringUtils.getLevenshteinDistance(fuzzyRunId, thisRunId);
      List<RunTripEntry> lrtes = matchedRTEs.get(ldist);
      if (lrtes != null) {
        lrtes.add(rte);
        matchedRTEs.put(ldist, lrtes);
      } else {
        lrtes = new ArrayList<RunTripEntry>();
        lrtes.add(rte);
        matchedRTEs.put(ldist, lrtes);
      }
    }

    return matchedRTEs;
  }

  @Override
  public RunTripEntry getActiveRunTripEntryForRunAndTime(AgencyAndId runAgencyAndId,
      long time) {

    String runId = runAgencyAndId.getId();
    if (!entriesByRun.containsKey(runId)) {
      _log.debug("Run id " + runId + " was not found.");
      return null;
    }

    Date serviceDate = getTimestampAsDate(runAgencyAndId.getAgencyId(), time);
    int scheduleTime = (int)(time - serviceDate.getTime())/1000;

    for (RunTripEntry entry : entriesByRun.get(runId)) {
      if (calendarService.isLocalizedServiceIdActiveOnDate(entry.getTripEntry().getServiceId(), serviceDate)
          && scheduleTime >= entry.getStartTime()
          && scheduleTime < entry.getStopTime())
        return entry;
    }
    return null;
  }

  @Override
  public List<RunTripEntry> getActiveRunTripEntriesForAgencyAndTime(String agencyId, long time) {
    ArrayList<RunTripEntry> out = new ArrayList<RunTripEntry>();
    List<BlockInstance> activeBlocks = blockCalendarService
        .getActiveBlocksForAgencyInTimeRange(agencyId, time, time);
    for (BlockInstance blockInstance : activeBlocks) {
      long serviceDate = blockInstance.getServiceDate();
      int scheduleTime = (int) ((time - serviceDate) / 1000);
      BlockConfigurationEntry blockConfig = blockInstance.getBlock();

      ScheduledBlockLocation blockLocation = scheduledBlockLocationService
          .getScheduledBlockLocationFromScheduledTime(blockConfig, scheduleTime);

      if (blockLocation != null) {
        BlockTripEntry trip = blockLocation.getActiveTrip();
        List<RunTripEntry> rtes = entriesByTrip.get(trip.getTrip().getId());

        if (rtes != null)
          out.addAll(rtes);
      }
    }

    return out;
  }

  @Override
  public RunTripEntry getPreviousEntry(RunTripEntry before, long serviceDate) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(serviceDate);
    Date date = calendar.getTime();

    List<RunTripEntry> entries = entriesByRun.get(before.getRunId());
    ListIterator<RunTripEntry> listIterator = entries.listIterator(entries
        .size());

    boolean found = false;
    while (listIterator.hasPrevious()) {
      RunTripEntry entry = listIterator.previous();
      if (found) {
        TripEntry tripEntry = entry.getTripEntry();
        ServiceIdActivation serviceIds = new ServiceIdActivation(
            tripEntry.getServiceId());
        if (extCalendarService.areServiceIdsActiveOnServiceDate(serviceIds,
            date)) {
          return entry;
        }
      }
      if (entry == before) {
        found = true;
      }
    }

    // try yesterday's trips
    calendar.add(Calendar.DATE, -1);
    date = calendar.getTime();
    listIterator = entries.listIterator(entries.size());
    while (listIterator.hasPrevious()) {
      RunTripEntry entry = listIterator.previous();
      TripEntry tripEntry = entry.getTripEntry();
      ServiceIdActivation serviceIds = new ServiceIdActivation(
          tripEntry.getServiceId());
      if (extCalendarService.areServiceIdsActiveOnServiceDate(serviceIds, date)) {
        return entry;
      }
    }
    return null;
  }

  @Override
  public RunTripEntry getNextEntry(RunTripEntry after, long serviceDate) {
    List<RunTripEntry> entries = entriesByRun.get(after.getRunId());
    boolean found = false;
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(serviceDate);
    Date date = calendar.getTime();
    for (RunTripEntry entry : entries) {
      if (found) {
        TripEntry tripEntry = entry.getTripEntry();
        ServiceIdActivation serviceIds = new ServiceIdActivation(
            tripEntry.getServiceId());
        if (extCalendarService.areServiceIdsActiveOnServiceDate(serviceIds,
            date)) {
          return entry;
        }
      }
      if (entry == after) {
        found = true;
      }
    }

    // try tomorrow's trips
    calendar.add(Calendar.DATE, 1);
    date = calendar.getTime();
    for (RunTripEntry entry : entries) {
      TripEntry tripEntry = entry.getTripEntry();
      ServiceIdActivation serviceIds = new ServiceIdActivation(
          tripEntry.getServiceId());
      if (extCalendarService.areServiceIdsActiveOnServiceDate(serviceIds, date)) {
        return entry;
      }
    }
    return null;
  }

  public void setRunDataByTrip(Map<AgencyAndId, RunData> runDataByTrip) {
    this.runDataByTrip = runDataByTrip;
  }

  @Override
  public RunTripEntry getActiveRunTripEntryForBlockInstance(
      BlockInstance blockInstance, int scheduleTime) {

    ScheduledBlockLocation blockLocation = scheduledBlockLocationService
        .getScheduledBlockLocationFromScheduledTime(blockInstance.getBlock(),
            scheduleTime);

    if (blockLocation == null) {
      _log.error("no scheduled block location for block=" + blockInstance
          + ", scheduleTime=" + scheduleTime);
      return null;
    }

    BlockTripEntry trip = blockLocation.getActiveTrip();

    List<RunTripEntry> bothTrips = entriesByTrip.get(trip.getTrip().getId());

    if (bothTrips != null && !bothTrips.isEmpty()) {

      RunTripEntry firstTrip = bothTrips.get(0);
      if (bothTrips.size() == 1) {
        return firstTrip;
      } else {
        RunTripEntry secondTrip = bothTrips.get(1);
        if (secondTrip.getStartTime() <= scheduleTime)
          return secondTrip;
      }
      return firstTrip;
    }

    return null;
  }

  private Date getTimestampAsDate(String agencyId, long timestamp) {
    Calendar cd = Calendar.getInstance();
    cd.setTimeInMillis(timestamp);
    cd.set(Calendar.HOUR_OF_DAY, 0);
    cd.set(Calendar.MINUTE, 0);
    cd.set(Calendar.SECOND, 0);
    cd.set(Calendar.MILLISECOND, 0);
    // TODO are these timezone changes necessary?
    cd.setTimeZone(calendarService.getTimeZoneForAgencyId(agencyId));
    return cd.getTime();
  }

  // TODO FIXME these methods don't require this much effort. they can
  // be pre-computed in setup(), or earlier.
  @Override
  public ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp) {

    Date serviceDate = getTimestampAsDate(runTrip.getTripEntry().getId()
        .getAgencyId(), timestamp);
    BlockInstance activeBlock = getBlockInstanceForRunTripEntry(runTrip,
        serviceDate);
    int scheduleTime = (int) ((timestamp - serviceDate.getTime()) / 1000);
    BlockTripEntry bte = blockCalendarService.getTargetBlockTrip(activeBlock,
        runTrip.getTripEntry());
    ScheduledBlockLocation sbl = scheduledBlockLocationService
        .getScheduledBlockLocationFromScheduledTime(
            bte.getBlockConfiguration(), scheduleTime);

    return sbl;

  }

  @Override
  public BlockInstance getBlockInstanceForRunTripEntry(RunTripEntry rte,
      Date serviceDate) {
    Calendar cd = Calendar.getInstance();
    cd.setTime(serviceDate);
    // TODO are these timezone changes necessary?
    cd.setTimeZone(calendarService.getTimeZoneForAgencyId(rte.getTripEntry()
        .getId().getAgencyId()));
    AgencyAndId blockId = rte.getTripEntry().getBlock().getId();
    BlockInstance bli = blockCalendarService.getBlockInstance(blockId,
        cd.getTimeInMillis());
    if (bli == null) {
      // FIXME just a hack, mostly for time issues
      List<BlockInstance> tmpBli = blockCalendarService.getActiveBlocks(
          blockId, serviceDate.getTime(), serviceDate.getTime());
      if (tmpBli != null && !tmpBli.isEmpty())
        return tmpBli.get(0);
    }
    return bli;
  }

}
