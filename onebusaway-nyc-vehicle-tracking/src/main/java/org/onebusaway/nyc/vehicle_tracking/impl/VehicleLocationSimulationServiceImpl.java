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
package org.onebusaway.nyc.vehicle_tracking.impl;

import org.onebusaway.csv_entities.CsvEntityReader;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.simulator.SimulatorTask;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.csv.TabTokenizerStrategy;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationSimulationSummary;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.EOutOfRangeStrategy;
import org.onebusaway.utility.InterpolationLibrary;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.DiscreteDistribution;
import umontreal.iro.lecuyer.randvar.UniformGen;
import umontreal.iro.lecuyer.randvarmulti.DirichletGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class VehicleLocationSimulationServiceImpl implements
    VehicleLocationSimulationService {

  private final Logger _log = LoggerFactory.getLogger(VehicleLocationSimulationServiceImpl.class);

  private static final String ARG_VEHICLE_ID = "vehicle_id";

  private static final String ARG_RUN_TRANSITION_PARAMS = "run_transition_params";

  private static final String ARG_SHIFT_START_TIME = "shift_start_time";

  private static final String ARG_LOCATION_SIGMA = "location_sigma";

  private static final String ARG_SCHEDULE_DEVIATIONS = "schedule_deviations";

  private static final String ARG_INCLUDE_START = "include_start";

  private int _numberOfThreads = 20;

  private ExecutorService _executor;

  private VehicleLocationInferenceService _vehicleLocationInferenceService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private RunService _runService;

  private DestinationSignCodeService _destinationSignCodeService;

  private final Map<Integer, SimulatorTask> _tasks = new ConcurrentHashMap<Integer, SimulatorTask>();

  private final ConcurrentMap<Object, List<SimulatorTask>> _tasksByTag = new ConcurrentHashMap<Object, List<SimulatorTask>>();

  private final AtomicInteger _taskIndex = new AtomicInteger();

  private BlockCalendarService _blockCalendarService;

  private CalendarService _calendarService;

  private final RandomStream streamArr = new MRG32k3a();

  // TODO consistent?
  private final UniformGen ung = new UniformGen(streamArr);

  @Autowired
  public void setCalendarService(CalendarService calendarService) {
    _calendarService = calendarService;
  }

  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationInferenceService = vehicleLocationService;
  }

  @Autowired
  public void setScheduledBlockLocationService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  public void setNumberOfThread(int numberOfThreads) {
    _numberOfThreads = numberOfThreads;
  }

  @PostConstruct
  public void start() {
    _executor = Executors.newFixedThreadPool(_numberOfThreads);
  }

  @PreDestroy
  public void stop() {
    _executor.shutdownNow();
  }

  @Override
  public int simulateLocationsFromTrace(String filename, String traceType,
      InputStream traceInputStream, boolean runInRealtime,
      boolean pauseOnStart, boolean shiftStartTime, int minimumRecordInterval,
      boolean bypassInference, boolean fillActualProperties, boolean loop)
      throws IOException {

    final SimulatorTask task = new SimulatorTask();
    task.setFilename(filename);
    task.setPauseOnStart(pauseOnStart);
    task.setRunInRealtime(runInRealtime);
    task.setShiftStartTime(shiftStartTime);
    task.setMinimumRecordInterval(minimumRecordInterval);
    task.setBypassInference(bypassInference);
    task.setFillActualProperties(fillActualProperties);
    task.setLoop(loop);

    final CsvEntityReader reader = new CsvEntityReader();
    reader.addEntityHandler(task);

    if (traceType.equals("NycRawLocationRecord")) {
      reader.setTokenizerStrategy(new TabTokenizerStrategy());
      reader.readEntities(NycRawLocationRecord.class, traceInputStream);
    } else if (traceType.equals("NycTestInferredLocationRecord")) {
      reader.readEntities(NycTestInferredLocationRecord.class, traceInputStream);
    }
    traceInputStream.close();

    return addTask(task);
  }

  @Override
  public List<VehicleLocationSimulationSummary> getSimulations() {

    final List<VehicleLocationSimulationSummary> summaries = new ArrayList<VehicleLocationSimulationSummary>();

    for (final SimulatorTask task : _tasks.values())
      summaries.add(task.getSummary());

    return summaries;
  }

  @Override
  public VehicleLocationSimulationSummary getSimulation(int taskId) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getSummary();
    return null;
  }

  @Override
  public VehicleLocationDetails getSimulationDetails(int taskId,
      int historyOffset) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getDetails(historyOffset);
    return null;
  }

  @Override
  public VehicleLocationDetails getParticleDetails(int taskId, int particleId) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getParticleDetails(particleId);
    return null;
  }

  @Override
  public List<NycTestInferredLocationRecord> getResultRecords(int taskId) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getResults();
    return null;
  }

  @Override
  public List<NycTestInferredLocationRecord> getSimulationRecords(int taskId) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getRecords();
    return Collections.emptyList();
  }

  @Override
  public void toggleSimulation(int taskId) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      task.toggle();
  }

  @Override
  public void stepSimulation(int taskId) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      task.step();
  }

  @Override
  public void stepSimulation(int taskId, int recordIndex) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      task.step(recordIndex);
  }

  @Override
  public void restartSimulation(int taskId) {
    final SimulatorTask task = _tasks.get(taskId);
    if (task != null) {
      task.restart();
      if (task.isComplete())
        _executor.execute(task);
    }
  }

  @Override
  public void cancelSimulation(int taskId) {
    final SimulatorTask task = _tasks.remove(taskId);
    if (task != null) {
      final Future<?> future = task.getFuture();
      future.cancel(true);
      task.resetAllVehiclesAppearingInRecordData();
      for (final Object tag : task.getTags()) {
        final List<SimulatorTask> list = getTasksForTag(tag, false);
        if (list != null) {
          synchronized (list) {
            list.remove(task);
          }
        }
      }
    }
  }

  @Override
  public void cancelAllSimulations() {

    final List<Integer> taskIds = new ArrayList<Integer>(_tasks.keySet());
    for (final int taskId : taskIds)
      cancelSimulation(taskId);
  }

  @Override
  public List<VehicleLocationSimulationSummary> getSimulationsForBlockInstance(
      AgencyAndId blockId, long serviceDate) {

    final String tag = generateBlockServiceDateTag(blockId, serviceDate);
    final List<SimulatorTask> tasks = getTasksForTag(tag, false);

    final List<VehicleLocationSimulationSummary> summaries = new ArrayList<VehicleLocationSimulationSummary>();

    if (tasks != null) {
      for (final SimulatorTask task : tasks)
        summaries.add(task.getSummary());
    }

    return summaries;
  }

  class RunTripComparator implements Comparator<RunTripEntry> {
    long _sd;
    BlockCalendarService _bcs;

    public RunTripComparator(long sd, BlockCalendarService bcs) {
      _sd = sd;
      _bcs = bcs;
    }

    @Override
    public int compare(RunTripEntry rte1, RunTripEntry rte2) {
      final TripEntry t1 = rte1.getTripEntry();
      final TripEntry t2 = rte2.getTripEntry();
      long sd1 = _bcs.getBlockInstance(t1.getBlock().getId(), _sd).getServiceDate();
      long sd2 = _bcs.getBlockInstance(t2.getBlock().getId(), _sd).getServiceDate();
      sd1 += rte1.getStartTime() * 1000;
      sd2 += rte2.getStartTime() * 1000;

      return sd1 < sd2 ? -1 : 1;
    }

  }

  public void generateRunSim(Random random, SimulatorTask task,
      RunTripEntry runTrip, long serviceDate, int scheduleTime,
      int shiftStartTime, boolean reportsOperatorId, boolean reportsRunId,
      boolean allowRunTransitions,
      SortedMap<Double, Integer> scheduleDeviations, double locationSigma,
      AgencyAndId vehicleId) {

    /*
     * here we format the runId to have a run-route that looks similar to what
     * an operator would enter.
     */
    final String runNumber = runTrip.getRunNumber();
    String runRoute = runTrip.getRunRoute();

    if (StringUtils.equals(runRoute, "MISC")) {
      // runRoute = "0" + random.nextInt(9) + random.nextInt(9);
      runRoute = "000";
    } else if (runRoute.length() >= 5) {
      final String firstPart = runRoute.substring(1, 3);
      final String lastPart = runRoute.substring(3);
      runRoute = "0" + (random.nextBoolean() ? firstPart : lastPart);
    } else {
      final String firstPart = runRoute.substring(1, 3);
      runRoute = "0" + firstPart;
    }

    final String reportedRunId = RunTripEntry.createId(runRoute, runNumber);

    if (reportsRunId)
      _log.info("using reported runId=" + reportedRunId);

    String lastBlockId = null;

    final List<RunTripEntry> rtes = new ArrayList<RunTripEntry>();
    for (final RunTripEntry rte : _runService.getRunTripEntriesForRun(runTrip.getRunId())) {
      if (_calendarService.isLocalizedServiceIdActiveOnDate(
          rte.getTripEntry().getServiceId(), new Date(serviceDate)))
        rtes.add(rte);
    }

    if (rtes.isEmpty()) {
      _log.error("no active runTrips for service date=" + new Date(serviceDate));
    }
    // TODO necessary?
    Collections.sort(rtes, new RunTripComparator(serviceDate,
        _blockCalendarService));

    // String agencyId = runTrip.getTripEntry().getId().getAgencyId();

    CoordinatePoint lastLocation = null;
    final RunTripEntry lastRunTrip = rtes.get(rtes.size() - 1);
    final int firstTime = runTrip.getStartTime();
    final int lastTime = lastRunTrip.getStopTime();
    int runningLastTime = runTrip.getStopTime();
    int currRunIdx = rtes.indexOf(runTrip);

    // We could go by run trip entry sequence or perturbed schedule-time
    // evolution. The latter is chosen, for now.
    while (scheduleTime <= lastTime) {

      final NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();

      final long unperturbedTimestamp = serviceDate
          + (scheduleTime + shiftStartTime) * 1000;

      if (scheduleTime >= runningLastTime) {
        if (currRunIdx == rtes.size() - 1)
          break;
        currRunIdx += 1;
        runTrip = rtes.get(currRunIdx);
        // runTrip = _runService.getNextEntry(runTrip);

        runningLastTime = runTrip.getStopTime();

        // FIXME saw weird run setups like this. are these in error?
        if (scheduleTime >= runningLastTime) {
          _log.error("runs are ordered oddly:" + rtes.get(currRunIdx - 1)
              + " -> " + runTrip);
          break;
        }
      }

      /*
       * this could mean that the run has ended. We could reassign the driver?
       * for now we'll terminate the simulation
       */
      if (runTrip == null)
        break;

      final TripEntry trip = runTrip.getTripEntry();

      final AgencyAndId tripId = trip.getId();

      record.setActualTripId(AgencyAndIdLibrary.convertToString(tripId));

      // TODO dsc changes for new block/run?
      String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(tripId);

      if (StringUtils.isEmpty(dsc))
        dsc = "0";

      // FIXME straighten these out...
      if (scheduleTime < runTrip.getStartTime()) {
        record.setActualPhase(EVehiclePhase.DEADHEAD_BEFORE.toString());
        dsc = "0";
      } else {
        record.setActualPhase(EVehiclePhase.IN_PROGRESS.toString());
      }

      int scheduleDeviation = 0;

      if (!scheduleDeviations.isEmpty()) {
        final double ratio = (scheduleTime - firstTime)
            / ((double) (lastTime - firstTime));
        scheduleDeviation = (int) InterpolationLibrary.interpolate(
            scheduleDeviations, ratio, EOutOfRangeStrategy.LAST_VALUE);
      }

      final long perterbedTimestamp = unperturbedTimestamp + scheduleDeviation
          * 1000;

      /*
       * sample runTrips active 30 minutes from now. TODO make the time range
       * less arbitrary?
       */
      if (allowRunTransitions) {
        final RunTripEntry newRun = sampleNearbyRunTrips(runTrip,
            unperturbedTimestamp + 30 * 60 * 1000);

        if (newRun != null) {
          // FIXME can we get to this trip in 30, or whatever minutes?
          // FIXME what geometry do we follow to get there?

          /*
           * we need to simulate the dsc/headsign: could set it to 0 the new
           * trip the current trip ... TODO should this be a user option?
           */
          record.setActualPhase(EVehiclePhase.DEADHEAD_BEFORE.toString());
          dsc = "0";

          runTrip = newRun;

          /*
           * similarly, do we reset the reportedRunId? TODO also a user option?
           */
          // reportedRunId = runTrip.getRun();

        }
      }

      // TODO when are there multiples and which do we choose when there are?
      final ScheduledBlockLocation blockLocation = _runService.getSchedBlockLocForRunTripEntryAndTime(
          runTrip, unperturbedTimestamp);

      if (blockLocation == null)
        break;

      final BlockEntry blockEntry = blockLocation.getActiveTrip().getBlockConfiguration().getBlock();

      _log.debug("sim blockLocation: " + blockLocation.toString());
      CoordinatePoint location = blockLocation.getLocation();

      record.setActualRunId(runTrip.getRunId());

      final String currentBlockId = AgencyAndIdLibrary.convertToString(blockEntry.getId());
      // if (_log.isDebugEnabled())
      if (lastBlockId != null
          && !StringUtils.equals(currentBlockId, lastBlockId)) {
        _log.info("changed blocks: " + lastBlockId + " -> " + currentBlockId);
      }
      record.setActualBlockId(currentBlockId);
      lastBlockId = currentBlockId;
      record.setActualDistanceAlongBlock(blockLocation.getDistanceAlongBlock());

      /*
       * during block changes we get a weird null location. this is a
       * "work-around"...
       */
      if (location == null) {
        location = lastLocation;
      } else {
        lastLocation = location;
      }
      final CoordinatePoint p = applyLocationNoise(location.getLat(),
          location.getLon(), locationSigma, random);

      record.setDsc(dsc);
      record.setLat(p.getLat());
      record.setLon(p.getLon());
      record.setTimestamp(perterbedTimestamp);
      record.setVehicleId(vehicleId);
      // TODO options for whether these are reported or not?
      if (reportsOperatorId)
        record.setOperatorId("0000");

      if (reportsRunId) {
        record.setReportedRunId(reportedRunId);
      }

      record.setActualServiceDate(serviceDate);

      final int actualScheduleTime = blockLocation.getScheduledTime();
      record.setActualScheduleTime(actualScheduleTime);

      record.setActualDsc(dsc);
      record.setActualBlockLat(location.getLat());
      record.setActualBlockLon(location.getLon());
      // TODO setActualStatus?

      task.addRecord(record);

      scheduleTime += 30 + random.nextGaussian() * 2;
    }

  }

  /**
   * This method samples runTrips that are active at the given time
   * 
   * @param currentRunTrip
   * @param timestamp
   * @return
   */
  private RunTripEntry sampleNearbyRunTrips(RunTripEntry currentRunTrip,
      long timestamp) {

    final List<RunTripEntry> activeRunTrips = _runService.getActiveRunTripEntriesForAgencyAndTime(
        currentRunTrip.getTripEntry().getId().getAgencyId(), timestamp);

    int bsize = activeRunTrips.size();

    if (bsize < 1) {
      _log.warn("sampler: not enough runs to sample");
      return null;
    }

    int currentIdx = activeRunTrips.lastIndexOf(currentRunTrip);

    if (currentIdx < 0) {

      activeRunTrips.add(currentRunTrip);
      currentIdx = activeRunTrips.lastIndexOf(currentRunTrip);
      bsize += 1;

    }

    final double[] probs = new double[bsize];
    final double[] obs = new double[bsize];
    for (int i = 0; i < bsize; ++i) {
      if (i == currentIdx) {
        // TODO Make this configurable, and explain
        probs[i] = 100 * bsize;
      } else {
        probs[i] = 1;
      }
      obs[i] = i;

      _log.debug(i + ": " + activeRunTrips.get(i));
    }
    final DirichletGen rdg = new DirichletGen(streamArr, probs);

    rdg.nextPoint(probs);
    final DiscreteDistribution emd = new DiscreteDistribution(obs, probs, bsize);

    final double u = ung.nextDouble();
    final int newIdx = (int) emd.inverseF(u);

    if (newIdx != currentIdx)
      _log.info(probs[newIdx] + ": sampled new idx=" + newIdx);

    return newIdx != currentIdx ? activeRunTrips.get(newIdx) : null;
  }

  /*
   * We make the distinction between whether a block or a run is driving this
   * simulation. However, we always initialize the simulation by a run that is
   * active for the "initialization" block, and thus trip, we were passed.
   * Really, what we have is block-inspired run simulations...
   */
  @Override
  public int addSimulationForBlockInstance(AgencyAndId blockId,
      long serviceDate, long actualTime, boolean bypassInference,
      boolean isRunBased, boolean realtime, boolean fillActual,
      boolean reportsOperatorId, boolean reportsRunId,
      boolean allowRunTransitions, Properties properties) {

    final Random random = new Random();

    final SimulatorTask task = new SimulatorTask();

    final String tag = generateBlockServiceDateTag(blockId, serviceDate);
    task.addTag(tag);

    task.setPauseOnStart(false);
    task.setRunInRealtime(realtime);
    task.setBypassInference(bypassInference);
    task.setFillActualProperties(fillActual);

    final BlockInstance blockInstance = _blockCalendarService.getBlockInstance(
        blockId, serviceDate);
    final BlockConfigurationEntry block = blockInstance.getBlock();

    int scheduleTime = (int) ((actualTime - serviceDate) / 1000);
    final int shiftStartTime = getShiftStartTimeProperty(properties);
    scheduleTime -= shiftStartTime;

    final AgencyAndId vehicleId = getVehicleIdProperty(random, properties,
        blockId.getAgencyId());

    final double locationSigma = getLocationSigma(properties);
    final SortedMap<Double, Integer> scheduleDeviations = getScheduleDeviations(properties);

    if (isRunBased) {

      final RunTripEntry runTrip = _runService.getActiveRunTripEntryForBlockInstance(
          blockInstance, scheduleTime);

      if (runTrip == null) {
        _log.warn("no runTrip for blockInstance=" + blockInstance
            + " and scheduleTime=" + scheduleTime);
        return -1;
      }

      _log.info("Using runTrip=" + runTrip.toString());

      // TODO pass these along to run-simulation method
      // List<Double> transitionParams =
      // getRunTransitionParams(properties);

      // TODO create "config" object to hold this mess?
      generateRunSim(random, task, runTrip, serviceDate, scheduleTime,
          shiftStartTime, reportsOperatorId, reportsRunId, allowRunTransitions,
          scheduleDeviations, locationSigma, vehicleId);

      return addTask(task);
    }

    final List<BlockStopTimeEntry> stopTimes = block.getStopTimes();

    final BlockStopTimeEntry firstStopTime = stopTimes.get(0);
    final BlockStopTimeEntry lastStopTime = stopTimes.get(stopTimes.size() - 1);
    final int firstTime = firstStopTime.getStopTime().getArrivalTime();
    final int lastTime = lastStopTime.getStopTime().getDepartureTime();

    if (isIncludeStartTime(properties))
      scheduleTime = firstTime;

    scheduleTime = Math.max(firstTime, scheduleTime);

    while (scheduleTime <= lastTime) {

      final NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();

      final ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          block, scheduleTime);

      /**
       * Not in service?
       */
      if (blockLocation == null)
        return -1;

      final BlockTripEntry trip = blockLocation.getActiveTrip();

      final AgencyAndId tripId = trip.getTrip().getId();

      final CoordinatePoint location = blockLocation.getLocation();
      record.setActualBlockId(AgencyAndIdLibrary.convertToString(blockId));
      record.setActualDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
      final int actualScheduleTime = blockLocation.getScheduledTime();

      String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(tripId);
      if (dsc == null)
        dsc = "0";

      int scheduleDeviation = 0;

      if (!scheduleDeviations.isEmpty()) {
        final double ratio = (scheduleTime - firstTime)
            / ((double) (lastTime - firstTime));
        scheduleDeviation = (int) InterpolationLibrary.interpolate(
            scheduleDeviations, ratio, EOutOfRangeStrategy.LAST_VALUE);
      }

      final long timestamp = serviceDate
          + (scheduleTime + shiftStartTime + scheduleDeviation) * 1000;

      final CoordinatePoint p = applyLocationNoise(location.getLat(),
          location.getLon(), locationSigma, random);

      record.setDsc(dsc);
      record.setLat(p.getLat());
      record.setLon(p.getLon());
      record.setTimestamp(timestamp);
      record.setVehicleId(vehicleId);
      record.setActualServiceDate(serviceDate);
      record.setActualScheduleTime(actualScheduleTime);
      record.setActualDsc(dsc);
      record.setActualPhase(EVehiclePhase.IN_PROGRESS.toString());

      task.addRecord(record);

      scheduleTime += 60 + random.nextGaussian() * 10;
    }

    return addTask(task);
  }

  /****
   * Private Methods
   ****/

  private int addTask(SimulatorTask task) {
    task.setId(_taskIndex.incrementAndGet());
    task.setVehicleLocationService(_vehicleLocationInferenceService);
    _tasks.put(task.getId(), task);
    for (final Object tag : task.getTags()) {
      final List<SimulatorTask> tasks = getTasksForTag(tag, true);
      synchronized (tasks) {
        tasks.add(task);
      }
    }
    final Future<?> future = _executor.submit(task);
    task.setFuture(future);

    return task.getId();
  }

  private List<SimulatorTask> getTasksForTag(Object tag, boolean create) {
    List<SimulatorTask> tasks = _tasksByTag.get(tag);
    if (tasks == null && create) {
      final ArrayList<SimulatorTask> newTasks = new ArrayList<SimulatorTask>();
      tasks = _tasksByTag.putIfAbsent(tag, newTasks);
      if (tasks == null)
        tasks = newTasks;
    }
    return tasks;
  }

  private String generateBlockServiceDateTag(AgencyAndId blockId,
      long serviceDate) {
    return blockId.toString() + "-" + serviceDate;
  }

  /****
   * Simulation Property Methods
   ****/
  /*
   * These correspond to the centrality of the current run.
   */
  private List<Double> getRunTransitionParams(Properties properties) {

    final List<Double> transitionParams = new ArrayList<Double>();

    final String raw = properties.getProperty(ARG_RUN_TRANSITION_PARAMS);

    for (final String token : raw.split(",")) {

      final String[] kvp = token.split("=");

      if (kvp.length != 1)
        throw new IllegalArgumentException("invalid run-transition params: "
            + raw);

      final double param = Double.parseDouble(kvp[0]);

      transitionParams.add(param);
    }

    return transitionParams;
  }

  private int getShiftStartTimeProperty(Properties properties) {
    int shiftStartTime = 0;
    if (properties.containsKey(ARG_SHIFT_START_TIME))
      shiftStartTime = Integer.parseInt(properties.getProperty(ARG_SHIFT_START_TIME)) * 60;
    return shiftStartTime;
  }

  private AgencyAndId getVehicleIdProperty(Random random,
      Properties properties, String agencyId) {

    AgencyAndId vehicleId = null;

    if (properties.containsKey(ARG_VEHICLE_ID))
      vehicleId = AgencyAndIdLibrary.convertFromString(properties.getProperty(ARG_VEHICLE_ID));

    if (vehicleId == null) {
      final int vid = random.nextInt(9999);
      vehicleId = new AgencyAndId(agencyId, vid + "");
    }
    return vehicleId;
  }

  private double getLocationSigma(Properties properties) {
    double locationSigma = 0.0;
    if (properties.containsKey(ARG_LOCATION_SIGMA))
      locationSigma = Double.parseDouble(properties.getProperty(ARG_LOCATION_SIGMA));
    return locationSigma;
  }

  private CoordinatePoint applyLocationNoise(double lat, double lon,
      double locationSigma, Random random) {
    if (locationSigma == 0)
      return new CoordinatePoint(lat, lon);
    final double latRadiusInMeters = random.nextGaussian() * locationSigma;
    final double lonRadiusInMeters = random.nextGaussian() * locationSigma;
    final CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lon,
        Math.abs(latRadiusInMeters), Math.abs(lonRadiusInMeters));
    final double latRadius = (bounds.getMaxLat() - bounds.getMinLat()) / 2
        * (latRadiusInMeters / Math.abs(latRadiusInMeters));
    final double lonRadius = (bounds.getMaxLon() - bounds.getMinLon()) / 2
        * (lonRadiusInMeters / Math.abs(lonRadiusInMeters));
    return new CoordinatePoint(lat + latRadius, lon + lonRadius);
  }

  private SortedMap<Double, Integer> getScheduleDeviations(Properties properties) {

    final SortedMap<Double, Integer> scheduleDeviations = new TreeMap<Double, Integer>();

    if (properties.containsKey(ARG_SCHEDULE_DEVIATIONS)) {
      final String raw = properties.getProperty(ARG_SCHEDULE_DEVIATIONS);
      for (final String token : raw.split(",")) {
        final String[] kvp = token.split("=");
        if (kvp.length != 2)
          throw new IllegalArgumentException(
              "invalid schedule deviations spec: " + raw);
        final double ratio = Double.parseDouble(kvp[0]);
        final int deviation = Integer.parseInt(kvp[1]) * 60;
        scheduleDeviations.put(ratio, deviation);
      }
    }

    return scheduleDeviations;
  }

  private boolean isIncludeStartTime(Properties properties) {
    if (!properties.containsKey(ARG_INCLUDE_START))
      return false;
    final String value = properties.getProperty(ARG_INCLUDE_START).toLowerCase();
    return value.equals("true") || value.equals("yes") || value.equals("1");
  }
}
