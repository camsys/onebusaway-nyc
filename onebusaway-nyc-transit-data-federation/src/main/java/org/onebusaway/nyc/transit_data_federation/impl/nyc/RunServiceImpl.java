package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
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
import org.onebusaway.transit_data_federation.services.transit_graph.RouteEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.ServiceIdActivation;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

/**
 * A service that maps trips to runs and vice-versa.
 * 
 * @author jmaki
 *
 */
@Component
public class RunServiceImpl implements RunService {
	
  private Logger _log = LoggerFactory.getLogger(RunServiceImpl.class);

  private NycFederatedTransitDataBundle _bundle;

  private Map<AgencyAndId, RunData> runDataByTrip;

  private Multimap<String, RunTripEntry> entriesByRun;
  
  private Multimap<String, AgencyAndId> runIdsToRoutes;
  
  private Multimap<String, AgencyAndId> runIdsToTripIds;

  private ListMultimap<String, RunTripEntry> entriesByRunNumber;

  private ListMultimap<AgencyAndId, RunTripEntry> entriesByTrip;
  
  private BlockCalendarService blockCalendarService;

  private TransitGraphDao transitGraph;
  
  private ScheduledBlockLocationService scheduledBlockLocationService;

  private ExtendedCalendarService extCalendarService;

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

  // public only for unit testing
  public void transformRunData() {
    entriesByRun = TreeMultimap.create();
    runIdsToRoutes = HashMultimap.create();
    runIdsToTripIds = HashMultimap.create();
    entriesByTrip = ArrayListMultimap.create();
    entriesByRunNumber = ArrayListMultimap.create();

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
  }

  private void processTripEntry(TripEntry trip, String runId, int reliefTime,
      ReliefState relief) {

    String[] runInfo = StringUtils.splitByWholeSeparator(runId, "-");
    String runNumber = null;
    String runRoute = null;

    if (runInfo != null && runInfo.length > 0) {
      runRoute = runInfo[0];
      if (runInfo.length > 1)
        runNumber = runInfo[1];
    }

    RunTripEntry rte = new RunTripEntry(trip, runNumber, runRoute, reliefTime,
        relief);
    entriesByRun.put(runId, rte);
    entriesByRunNumber.put(runNumber, rte);
    // this will fail for unit tests otherwise
    RouteEntry route = rte.getTripEntry().getRoute();
    if (route != null)
      runIdsToRoutes.put(rte.getRunId(), route.getParent().getId());
    runIdsToTripIds.put(rte.getRunId(), rte.getTripEntry().getId());

    // add to index by trip
    AgencyAndId tripId = trip.getId();
    entriesByTrip.put(tripId, rte);
  }

  public void setRunDataByTrip(Map<AgencyAndId, RunData> runDataByTrip) {
	    this.runDataByTrip = runDataByTrip;
  }
  
  /**
   * PUBLIC METHODS
   */
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
  public Collection<RunTripEntry> getRunTripEntriesForRun(String runId) {
    return entriesByRun.get(runId);
  }

  private final Pattern realRunRouteIdPattern = Pattern.compile("[a-zA-Z]+0*(\\d+)[a-zA-Z]*"); // X0102D
  private final Pattern realRunNumberPattern = Pattern.compile("[a-zA-Z]*0*(\\d+)"); // X0102
  private final Pattern reportedRunIdPattern = Pattern.compile("0*([0-9]+)(-[a-zA-Z]{2})?-0*(\\d+)"); // 003-001 or 999-YU-02

  @Override
  public TreeMultimap<Integer, String> getBestRunIdsForFuzzyId(
      String reportedRunId)
      throws IllegalArgumentException {
    
    TreeMultimap<Integer, String> matchedRTEs = TreeMultimap.create();
    Matcher reportedIdMatcher = reportedRunIdPattern.matcher(reportedRunId);

    if (!reportedIdMatcher.matches()) {
      throw new IllegalArgumentException(
          "reported-id does not have required format:" + reportedRunId);
    }

    /*
     * Get run-trips for nearby runTrips
     */
    String fuzzyRunId = null;
    if (StringUtils.isNotEmpty(reportedIdMatcher.group(2))) {
      System.err.println("group2=|" + reportedIdMatcher.group(2) + "|");
      System.err.println("group3=|" + reportedIdMatcher.group(3) + "|");
      System.err.println("fuzzy1(" + reportedIdMatcher.group(1) + "-" + reportedIdMatcher.group(2) + "-" + reportedIdMatcher.group(3) + ")");
      fuzzyRunId = RunTripEntry.createId(reportedIdMatcher.group(1),
          reportedIdMatcher.group(2) + "-" + reportedIdMatcher.group(3));
    } else {
      System.err.println("fuzzy2(" + reportedIdMatcher.group(1) + "-" + reportedIdMatcher.group(3) + ")");
      fuzzyRunId = RunTripEntry.createId(reportedIdMatcher.group(1),
          reportedIdMatcher.group(2));
    }
    
    /*
     * In the following we strip the runEntry's id down to the format of the
     * reported id's, as best we can.  For MISC routes, we use the 
     * convention that they enter 999 as the run route.
     */
    for (String runId : entriesByRun.keySet()) {
      String[] runPieces= runId.split("-");
      String runRoute = runPieces[0];
      String runDepot = null; 
      String runNumber = null;
      if (runPieces.length == 3) {
    	  runDepot = runPieces[1];
    	  runNumber = runPieces[2];
      } else {
    	  runNumber = runPieces[1];
      }
      Matcher runNumberMatcher = realRunNumberPattern.matcher(runNumber);
      if (runNumberMatcher.matches()) {
        runNumber = runNumberMatcher.group(1);
      }
      List<String> runIdsToTry = Lists.newArrayList();
      if (runRoute.equals("MISC")) {
    	  String runIdToTry = null;
    	  if (runDepot == null) {
    	     runIdToTry = RunTripEntry.createId("999", runNumber);
    	  } else {
    	    runIdToTry = RunTripEntry.createId("999", runDepot + "-" + runNumber);
    	  }
        runIdsToTry.add(runIdToTry);
      } else {
        Matcher routeIdMatcher = realRunRouteIdPattern.matcher(runRoute);
        if (routeIdMatcher.matches()) {
          String thisRunRouteNumber = routeIdMatcher.group(1);
          String runIdToTry = RunTripEntry.createId(thisRunRouteNumber,
              runNumber);
          runIdsToTry.add(runIdToTry);
        }
      }

      if (runIdsToTry.isEmpty()) {
        _log.debug("bundle-data runId does not have the required format:"
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
  public List<RunTripEntry> getActiveRunTripEntriesForAgencyAndTime(
      String agencyId, long time) {
    ArrayList<RunTripEntry> out = Lists.newArrayList();
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
        out.addAll(entriesByTrip.get(trip.getTrip().getId()));
      }
    }

    return out;
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

  // TODO these methods don't require this much effort. they can
  // be pre-computed in setup(), or earlier.
  @Override
  public ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp) {

    Date serviceDate = getTimestampAsDate(timestamp);
    BlockInstance activeBlock = getBlockInstanceForRunTripEntry(runTrip,
        serviceDate);
    int scheduleTime = (int) ((timestamp - serviceDate.getTime()) / 1000);
    BlockTripEntry bte = getTargetBlockTrip(activeBlock, runTrip.getTripEntry());
    ScheduledBlockLocation sbl = scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        bte.getBlockConfiguration(), scheduleTime);

    return sbl;

  }

  /**
   * Notice that this will only match times EXACTLY within the ACTIVE schedule
   * times (based on stops times).
   */
  @Override
  public RunTripEntry getRunTripEntryForTripAndTime(TripEntry trip,
      int scheduleTime) {

    List<RunTripEntry> bothTrips = entriesByTrip.get(trip.getId());

    if (!bothTrips.isEmpty()) {
      RunTripEntry firstTrip = bothTrips.get(0);
      if (bothTrips.size() == 1) {
        return firstTrip;
      } else {
        RunTripEntry secondTrip = bothTrips.get(1);
        if (secondTrip.getStartTime() <= scheduleTime 
            && secondTrip.getStartTime() >= firstTrip.getStartTime())
          return secondTrip;
      }
      return firstTrip;
    }

    return null;
  }
  
  @Override
  public Set<String> getRunIdsForTrip(TripEntry trip) {
    Set<String> ids = Sets.newHashSet();
    for (RunTripEntry rte : entriesByTrip.get(trip.getId())) {
      ids.add(rte.getRunId());
    }
    return ids;
  }

  @Override
  public boolean isValidRunId(String runId) {
    return this.entriesByRun.containsKey(runId);
  }

  @Override
  public Collection<? extends AgencyAndId> getTripIdsForRunId(String runId) {
    return runIdsToTripIds.get(runId);
  }

  @Override
  public Set<AgencyAndId> getRoutesForRunId(String runId) {
    Collection<AgencyAndId> routeIds = runIdsToRoutes.get(runId);
    return Objects.firstNonNull(Sets.newHashSet(routeIds), Collections.<AgencyAndId>emptySet());
  }
  
  @Override
  public RunTripEntry getPreviousEntry(RunTripEntry before, long serviceDate) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(serviceDate);
    Date date = calendar.getTime();

    List<RunTripEntry> entries = Lists.newArrayList(entriesByRun.get(before.getRunId()));
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
    Collection<RunTripEntry> entries = entriesByRun.get(after.getRunId());
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
 
  /**
   * PRIVATE METHODS
   */
  private static Date getTimestampAsDate(long timestamp) {
    Calendar cd = Calendar.getInstance();
    cd.setTimeInMillis(timestamp);
    cd.set(Calendar.HOUR_OF_DAY, 0);
    cd.set(Calendar.MINUTE, 0);
    cd.set(Calendar.SECOND, 0);
    cd.set(Calendar.MILLISECOND, 0);
    return cd.getTime();
  }

  private static BlockTripEntry getTargetBlockTrip(BlockInstance blockInstance,
      TripEntry trip) {

    BlockConfigurationEntry blockConfig = blockInstance.getBlock();

    for (BlockTripEntry blockTrip : blockConfig.getTrips()) {
      if (blockTrip.getTrip().equals(trip))
        return blockTrip;
    }
    return null;
  }
  
  private BlockInstance getBlockInstanceForRunTripEntry(RunTripEntry rte,
      Date serviceDate) {

    Date trunDate = getTimestampAsDate(serviceDate.getTime());
    AgencyAndId blockId = rte.getTripEntry().getBlock().getId();
    BlockInstance bli = blockCalendarService.getBlockInstance(blockId,
        trunDate.getTime());

    if (bli == null) {
      // FIXME a hack, mostly for time issues
      List<BlockInstance> tmpBli = blockCalendarService.getClosestActiveBlocks(
          blockId, serviceDate.getTime());
      if (tmpBli != null && !tmpBli.isEmpty())
        return tmpBli.get(0);
    }
    return bli;
  }
}
