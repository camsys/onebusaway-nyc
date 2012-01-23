package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.onebusaway.transit_data_federation.services.transit_graph.RouteCollectionEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.ObjectSerializationLibrary;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

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
  
  private Multimap<String, AgencyAndId> runIdsToRoutes;
  
  private Multimap<String, AgencyAndId> runIdsToTripIds;

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
  public void setExtendedCalendarService(
      ExtendedCalendarService extCalendarService) {
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
    runIdsToRoutes = HashMultimap.create();
    runIdsToTripIds = HashMultimap.create();
    entriesByTrip = new HashMap<AgencyAndId, List<RunTripEntry>>();
    entriesByRunNumber = new HashMap<String, List<RunTripEntry>>();

    for (Map.Entry<AgencyAndId, RunData> entry : runDataByTrip.entrySet()) {
      TripEntry trip = transitGraph.getTripEntryForId(entry.getKey());
      if (trip != null) {
        RunData runData = entry.getValue();

        ReliefState initialReliefState = runData.hasRelief()
            ? ReliefState.BEFORE_RELIEF : ReliefState.NO_RELIEF;
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
    // this will fail for unit tests otherwise
    RouteEntry route = rte.getTripEntry().getRoute();
    runIdsToRoutes.put(rte.getRunId(), (route != null) ? route.getParent().getId() : null);
    runIdsToTripIds.put(rte.getRunId(), rte.getTripEntry().getId());

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

  private final Pattern realRunRouteIdPattern = Pattern.compile("([a-zA-Z]+)(\\d*)[a-zA-Z]*");
  private final Pattern reportedRunIdPattern = Pattern.compile("\\d(\\d+)-0*(\\d+)");
  private final Pattern routeIdPattern = Pattern.compile("[a-zA-Z]+(\\d{2})+");
  private final Pattern multiRoutePattern = Pattern.compile("(?:0([1-9]{1}))|([1-9]{1}0)|([1-9]{2})");

  @Override
  public TreeMultimap<Integer, String> getBestRunIdsForFuzzyId(
      String runAgencyAndId)
      throws IllegalArgumentException {
    
    TreeMultimap<Integer, String> matchedRTEs = TreeMultimap.create();
    Matcher reportedIdMatcher = reportedRunIdPattern.matcher(runAgencyAndId);

    if (!reportedIdMatcher.matches()) {
      throw new IllegalArgumentException(
          "reported-id does not have required format");
    }

    /*
     * Get run-trips for nearby runTrips
     */
    String fuzzyRunId = RunTripEntry.createId(reportedIdMatcher.group(1),
        reportedIdMatcher.group(2));
    
    /*
     * In the following we strip the runEntry's id down to the format of the
     * reported id's, as best we can. We allow matching to double-route
     * route-id's, e.g. X0109 (match either 01 or 09 routes). Also, for MISC
     * routes, we guess that they enter 00 or the route number of the trip.
     */
    for (String runId : entriesByRun.keySet()) {
      String[] runPieces= runId.split("-");
      String runRoute = runPieces[0];
      String runNumber = runPieces[1];
      Matcher routeIdMatcher = realRunRouteIdPattern.matcher(runRoute);
      List<String> runIdsToTry = new ArrayList<String>();
      if (routeIdMatcher.matches()) {
        if (StringUtils.isNotEmpty(routeIdMatcher.group(2))) {
          String thisRunRouteNumber = routeIdMatcher.group(2);

          Matcher multiRouteMatcher = multiRoutePattern.matcher(thisRunRouteNumber);
          while (multiRouteMatcher.find()) {
            for (int i = 1; i <= multiRouteMatcher.groupCount(); ++i) {
              if (multiRouteMatcher.group(i) != null)
                runIdsToTry.add(RunTripEntry.createId(multiRouteMatcher.group(i),
                    runNumber));
            }
          } 
          if (runIdsToTry.isEmpty()){
            runIdsToTry.add(RunTripEntry.createId(thisRunRouteNumber,
                runNumber));
          }

        } else {
          /*
           * there is no numeric part to the routeId. check for MISC value
           */
          if (StringUtils.equals(runRoute, "MISC")) {
            /*
             * they're allowed to enter anything, so we put this in there
             */
            runIdsToTry.add(RunTripEntry.createId("00", runNumber));

            /*
             * however, they often use the actual route number
             */
            Collection<AgencyAndId> routeIds = runIdsToRoutes.get(runId);
            Matcher actualRouteIdMatcher;
            for (AgencyAndId routeId : routeIds) {
              actualRouteIdMatcher = routeIdPattern.matcher(routeId.getId());
              if (actualRouteIdMatcher.matches()) {
                for (int i = 1; i <= actualRouteIdMatcher.groupCount(); ++i) {
                  runIdsToTry.add(RunTripEntry.createId(
                      actualRouteIdMatcher.group(i), runNumber));
                }
              }
            }
          }
        }
      }

      if (runIdsToTry.isEmpty()) {
        _log.error("bundle-data runId does not have the required format:"
            + runId);
        continue;
      }

      int bestDist = Integer.MAX_VALUE;
      for (String tryRunId : runIdsToTry) {
        int ldist = StringUtils.getLevenshteinDistance(fuzzyRunId, tryRunId);
        if (ldist <= bestDist) {
          bestDist = ldist;
        }
      }

      matchedRTEs.put(bestDist, runId);
    }
    
    return matchedRTEs;
  }
  
  @Override
  public TreeMap<Integer, List<RunTripEntry>> getRunTripEntriesForFuzzyIdAndTime(
      AgencyAndId runAgencyAndId, Set<BlockInstance> nearbyBlocks, long time)
      throws IllegalArgumentException {

    /*
     * FIXME TODO really need to cache these search results...
     */
    TreeMap<Integer, List<RunTripEntry>> matchedRTEs = new TreeMap<Integer, List<RunTripEntry>>();

    Matcher reportedIdMatcher = reportedRunIdPattern.matcher(runAgencyAndId.getId());

    if (!reportedIdMatcher.matches()) {
      throw new IllegalArgumentException(
          "reported-id does not have required format");
    }

    /*
     * Get run-trips for nearby runTrips
     */
    String fuzzyRunId = RunTripEntry.createId(reportedIdMatcher.group(1),
        reportedIdMatcher.group(2));

    Set<RunTripEntry> runsToTry = new HashSet<RunTripEntry>();
    for (BlockInstance binst : nearbyBlocks) {
      long serviceDate = binst.getServiceDate();
      int scheduleTime = (int) ((time - serviceDate) / 1000);
      RunTripEntry rte = getActiveRunTripEntryForBlockInstance(binst,
          scheduleTime);
      if (rte == null || StringUtils.isEmpty(rte.getRunId()))
        continue;
      runsToTry.add(rte);
    }

    /*
     * Now, add all the runs with the same run-number
     */
//    for (RunTripEntry rte : entriesByRunNumber.get(reportedIdMatcher.group(2))) {
//      RunTripEntry runAtTime = getActiveRunTripEntryForRunAndTime(
//          new AgencyAndId(rte.getTripEntry().getId().getAgencyId(),
//              rte.getRunId()), time);
//      if (runAtTime != null)
//        runsToTry.add(runAtTime);
//    }


    return matchedRTEs;
  }

  /**
   * If a schedule time is between the last stop time of a trip and the first
   * start time of the next, we return the next trip.
   */
  @Override
  public RunTripEntry getActiveRunTripEntryForRunAndTime(
      AgencyAndId runAgencyAndId, long time) {

    String runId = runAgencyAndId.getId();
    if (!entriesByRun.containsKey(runId)) {
      _log.warn("Run id " + runId + " was not found.");
      return null;
    }

    Date serviceDate = getTimestampAsDate(time);
    int scheduleTime = (int) (time - serviceDate.getTime()) / 1000;

    for (RunTripEntry entry : entriesByRun.get(runId)) {
      if (!calendarService.isLocalizedServiceIdActiveOnDate(
          entry.getTripEntry().getServiceId(), serviceDate))
        continue;

      boolean activeInThisTrip = scheduleTime >= entry.getStartTime()
          && scheduleTime < entry.getStopTime();

      if (activeInThisTrip)
        return entry;

      RunTripEntry nextTrip = getNextEntry(entry, serviceDate.getTime());
      if (nextTrip != null && scheduleTime <= nextTrip.getStartTime()) {
        return nextTrip;
      }
    }
    return null;
  }

  @Override
  public List<RunTripEntry> getActiveRunTripEntriesForAgencyAndTime(
      String agencyId, long time) {
    ArrayList<RunTripEntry> out = new ArrayList<RunTripEntry>();
    List<BlockInstance> activeBlocks = blockCalendarService.getActiveBlocksForAgencyInTimeRange(
        agencyId, time, time);
    for (BlockInstance blockInstance : activeBlocks) {
      long serviceDate = blockInstance.getServiceDate();
      int scheduleTime = (int) ((time - serviceDate) / 1000);
      BlockConfigurationEntry blockConfig = blockInstance.getBlock();

      ScheduledBlockLocation blockLocation = scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          blockConfig, scheduleTime);

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
    ListIterator<RunTripEntry> listIterator = entries.listIterator(entries.size());

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

  /**
   * This follows the same general contract of
   * ScheduledBlockLocation.getActiveTrip, i.e. if we go past the end of the
   * block, return the last run-trip
   */
  @Override
  public RunTripEntry getActiveRunTripEntryForBlockInstance(
      BlockInstance blockInstance, int scheduleTime) {

    ScheduledBlockLocation blockLocation = scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        blockInstance.getBlock(), scheduleTime);

    /*
     * according to getScheduledBlockLocationFromScheduledTime, we get null when
     * we've gone past the end of the block.
     */
    if (blockLocation == null) {
      blockLocation = scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
          blockInstance.getBlock(),
          blockInstance.getBlock().getTotalBlockDistance());
    }

    BlockTripEntry trip = blockLocation.getActiveTrip();

    return getRunTripEntryForTripAndTime(trip.getTrip(),
        blockLocation.getScheduledTime());
  }

  private Date getTimestampAsDate(long timestamp) {
    Calendar cd = Calendar.getInstance();
    cd.setTimeInMillis(timestamp);
    cd.set(Calendar.HOUR_OF_DAY, 0);
    cd.set(Calendar.MINUTE, 0);
    cd.set(Calendar.SECOND, 0);
    cd.set(Calendar.MILLISECOND, 0);
    return cd.getTime();
  }

  // TODO FIXME these methods don't require this much effort. they can
  // be pre-computed in setup(), or earlier.
  @Override
  public ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp) {

    Date serviceDate = getTimestampAsDate(timestamp);
    BlockInstance activeBlock = getBlockInstanceForRunTripEntry(runTrip,
        serviceDate);
    int scheduleTime = (int) ((timestamp - serviceDate.getTime()) / 1000);
    BlockTripEntry bte = blockCalendarService.getTargetBlockTrip(activeBlock,
        runTrip.getTripEntry());
    ScheduledBlockLocation sbl = scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        bte.getBlockConfiguration(), scheduleTime);

    return sbl;

  }

  @Override
  public BlockInstance getBlockInstanceForRunTripEntry(RunTripEntry rte,
      Date serviceDate) {

    Date trunDate = getTimestampAsDate(serviceDate.getTime());
    AgencyAndId blockId = rte.getTripEntry().getBlock().getId();
    BlockInstance bli = blockCalendarService.getBlockInstance(blockId,
        trunDate.getTime());

    if (bli == null) {
      // FIXME just a hack, mostly for time issues
      List<BlockInstance> tmpBli = blockCalendarService.getClosestActiveBlocks(
          blockId, serviceDate.getTime());
      if (tmpBli != null && !tmpBli.isEmpty())
        return tmpBli.get(0);
    }
    return bli;
  }

  /**
   * Notice that this will only match times EXACTLY within the ACTIVE schedule
   * times (based on stops times).
   */
  @Override
  public RunTripEntry getRunTripEntryForTripAndTime(TripEntry trip,
      int scheduleTime) {

    List<RunTripEntry> bothTrips = entriesByTrip.get(trip.getId());

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

  @Override
  public boolean isValidRunNumber(String runNumber) {
    return this.entriesByRunNumber.containsKey(runNumber);
  }

  @Override
  public Collection<? extends AgencyAndId> getTripIdsForRunId(String runId) {
    return runIdsToTripIds.get(runId);
  }

}
