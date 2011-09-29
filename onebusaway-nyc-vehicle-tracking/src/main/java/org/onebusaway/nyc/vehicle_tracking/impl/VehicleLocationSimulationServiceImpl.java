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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.impl.nyc.RunServiceImpl;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.simulator.SimulatorTask;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.csv.TabTokenizerStrategy;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationSimulationSummary;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.EOutOfRangeStrategy;
import org.onebusaway.utility.InterpolationLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import umontreal.iro.lecuyer.rng.*;
import umontreal.iro.lecuyer.randvar.*;
import umontreal.iro.lecuyer.probdist.*;
import umontreal.iro.lecuyer.randvarmulti.*;
import umontreal.iro.lecuyer.probdistmulti.*;

@Component
public class VehicleLocationSimulationServiceImpl implements
    VehicleLocationSimulationService {

  private Logger _log = LoggerFactory.getLogger(RunServiceImpl.class);

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

  private Map<Integer, SimulatorTask> _tasks = new ConcurrentHashMap<Integer, SimulatorTask>();

  private ConcurrentMap<Object, List<SimulatorTask>> _tasksByTag = new ConcurrentHashMap<Object, List<SimulatorTask>>();

  private AtomicInteger _taskIndex = new AtomicInteger();

  private BlockCalendarService _blockCalendarService;

  private RandomStream streamArr = new MRG32k3a();

  // TODO consistent?
  private UniformGen ung = new UniformGen(streamArr);

  private BlockGeospatialService _blockGeospatialService;

  @Autowired
  public void setBlockGeospatiaalService(
      BlockGeospatialService blockGeospatialService) {
    _blockGeospatialService = blockGeospatialService;
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
  public int simulateLocationsFromTrace(String traceType,
      InputStream traceInputStream, boolean runInRealtime,
      boolean pauseOnStart, boolean shiftStartTime, int minimumRecordInterval,
      boolean bypassInference, boolean fillActualProperties, boolean loop)
      throws IOException {

    SimulatorTask task = new SimulatorTask();
    task.setPauseOnStart(pauseOnStart);
    task.setRunInRealtime(runInRealtime);
    task.setShiftStartTime(shiftStartTime);
    task.setMinimumRecordInterval(minimumRecordInterval);
    task.setBypassInference(bypassInference);
    task.setFillActualProperties(fillActualProperties);
    task.setLoop(loop);

    CsvEntityReader reader = new CsvEntityReader();
    reader.addEntityHandler(task);

    if (traceType.equals("NycRawLocationRecord")) {
      reader.setTokenizerStrategy(new TabTokenizerStrategy());
      reader.readEntities(NycRawLocationRecord.class, traceInputStream);
    } else if (traceType.equals("NycTestInferredLocationRecord")) {
      reader
          .readEntities(NycTestInferredLocationRecord.class, traceInputStream);
    }
    traceInputStream.close();

    return addTask(task);
  }

  @Override
  public List<VehicleLocationSimulationSummary> getSimulations() {

    List<VehicleLocationSimulationSummary> summaries = new ArrayList<VehicleLocationSimulationSummary>();

    for (SimulatorTask task : _tasks.values())
      summaries.add(task.getSummary());

    return summaries;
  }

  @Override
  public VehicleLocationSimulationSummary getSimulation(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getSummary();
    return null;
  }

  @Override
  public VehicleLocationDetails getSimulationDetails(int taskId,
      int historyOffset) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getDetails(historyOffset);
    return null;
  }

  @Override
  public VehicleLocationDetails getParticleDetails(int taskId, int particleId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getParticleDetails(particleId);
    return null;
  }

  @Override
  public List<NycTestInferredLocationRecord> getResultRecords(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getResults();
    return null;
  }

  @Override
  public List<NycTestInferredLocationRecord> getSimulationRecords(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getRecords();
    return Collections.emptyList();
  }

  @Override
  public void toggleSimulation(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      task.toggle();
  }

  @Override
  public void stepSimulation(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      task.step();
  }

  @Override
  public void stepSimulation(int taskId, int recordIndex) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      task.step(recordIndex);
  }

  @Override
  public void restartSimulation(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null) {
      task.restart();
      if (task.isComplete())
        _executor.execute(task);
    }
  }

  @Override
  public void cancelSimulation(int taskId) {
    SimulatorTask task = _tasks.remove(taskId);
    if (task != null) {
      Future<?> future = task.getFuture();
      future.cancel(true);
      task.resetAllVehiclesAppearingInRecordData();
      for (Object tag : task.getTags()) {
        List<SimulatorTask> list = getTasksForTag(tag, false);
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

    List<Integer> taskIds = new ArrayList<Integer>(_tasks.keySet());
    for (int taskId : taskIds)
      cancelSimulation(taskId);
  }

  @Override
  public List<VehicleLocationSimulationSummary> getSimulationsForBlockInstance(
      AgencyAndId blockId, long serviceDate) {

    String tag = generateBlockServiceDateTag(blockId, serviceDate);
    List<SimulatorTask> tasks = getTasksForTag(tag, false);

    List<VehicleLocationSimulationSummary> summaries = new ArrayList<VehicleLocationSimulationSummary>();

    if (tasks != null) {
      for (SimulatorTask task : tasks)
        summaries.add(task.getSummary());
    }

    return summaries;
  }

  public void generateRunSim(Random random, SimulatorTask task,
      RunTripEntry runTrip, long serviceDate, int scheduleTime,
      int shiftStartTime, int trueTimeOffset,
      SortedMap<Double, Integer> scheduleDeviations, double locationSigma,
      AgencyAndId vehicleId) {

    List<RunTripEntry> rtes = _runService.getRunTripEntriesForRun(runTrip
        .getRun());

    // String agencyId = runTrip.getTripEntry().getId().getAgencyId();

    int firstTime = runTrip.getStartTime();

    RunTripEntry lastRunTrip = rtes.get(rtes.size() - 1);

    int lastTime = lastRunTrip.getStopTime();

    // this is the last time of the current run-trip
    int runningLastTime = 0;

    ScheduledBlockLocation lastBlockLocation = null;

    // We could go by run trip entry sequence or perturbed schedule-time
    // evolution. The latter is chosen, for now.
    while (scheduleTime <= lastTime) {

      NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();

      long unperturbedTimestamp = serviceDate + (scheduleTime + shiftStartTime)
          * 1000;

      if (scheduleTime >= runningLastTime) {
        runTrip = _runService.getNextEntry(runTrip);
        runningLastTime = runTrip.getStopTime();
      }

      // This could mean that the run has ended. We could reassign the driver?
      // For now we'll terminate the simulation
      if (runTrip == null)
        break;

      TripEntry trip = runTrip.getTripEntry();

      AgencyAndId tripId = trip.getId();

      record.setActualTripId(AgencyAndIdLibrary.convertToString(tripId));

      // TODO dsc changes for new block/run?
      String dsc = _destinationSignCodeService
          .getDestinationSignCodeForTripId(tripId);

      if (dsc == null)
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
        double ratio = (scheduleTime - firstTime)
            / ((double) (lastTime - firstTime));
        scheduleDeviation = (int) InterpolationLibrary.interpolate(
            scheduleDeviations, ratio, EOutOfRangeStrategy.LAST_VALUE);
      }

      long perterbedTimestamp = unperturbedTimestamp + scheduleDeviation * 1000;

      // FIXME Is this level of indirection necessary for just the
      // location?
      // We should use the geometry to make a TRULY run-based sim?
      // Especially here, where we're just selecting the first
      // configuration to get a location.
      BlockEntry blockEntry = trip.getBlock();
      BlockInstance newBlock = null;
      if (lastBlockLocation != null) {

        CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
            lastBlockLocation.getClosestStop().getStopTime().getStop()
                .getStopLat(), lastBlockLocation.getClosestStop().getStopTime()
                .getStop().getStopLon(), 100);

        // TODO might want to ensure sane transfers...
        List<BlockInstance> activeBlocks = _blockGeospatialService
            .getActiveScheduledBlocksPassingThroughBounds(bounds,
                unperturbedTimestamp - 1 * 60 * 1000,
                unperturbedTimestamp + 1 * 60 * 1000);
        BlockInstance currentBlockInstance = _blockCalendarService
            .getBlockInstance(blockEntry.getId(), serviceDate);
        newBlock = sampleNearbyBlocks(currentBlockInstance, activeBlocks);
      }

      if (newBlock != null) {
        blockEntry = newBlock.getBlock().getBlock();
      }

      ScheduledBlockLocation blockLocation = null;
      for (BlockConfigurationEntry block : blockEntry.getConfigurations()) {

        blockLocation = _scheduledBlockLocationService
            .getScheduledBlockLocationFromScheduledTime(block, scheduleTime);

        if (blockLocation != null) {
          BlockTripEntry btrip = blockLocation.getActiveTrip();

          AgencyAndId newTripId = btrip.getTrip().getId();

          String runId = _runService.getInitialRunForTrip(newTripId);
          String reliefRunId = _runService.getReliefRunForTrip(newTripId);

          _log.info(scheduleTime + ":" + blockLocation.toString() + ", "
              + runId + "-" + reliefRunId);

          if (newBlock != null) {
            RunTripEntry newRunTrip = _runService.getRunTripEntryForRunAndTime(
                btrip.getTrip().getId().getAgencyId(), runId,
                unperturbedTimestamp);

            if (newRunTrip != null && newRunTrip.getRun() != runTrip.getRun()) {
              _log.info(scheduleTime + ":" + runTrip + " -> " + newRunTrip);
              runTrip = newRunTrip;
            }
          }
        }
      }

      if (blockLocation == null)
        break;

      lastBlockLocation = blockLocation;
      _log.info("sim blockLocation: " + blockLocation.toString());
      CoordinatePoint location = blockLocation.getLocation();

      record.setActualRunId(runTrip.getRun());

      record.setActualBlockId(AgencyAndIdLibrary.convertToString(blockEntry
          .getId()));
      record.setActualDistanceAlongBlock(blockLocation.getDistanceAlongBlock());

      CoordinatePoint p = applyLocationNoise(location.getLat(),
          location.getLon(), locationSigma, random);

      record.setDsc(dsc);
      record.setLat(p.getLat());
      record.setLon(p.getLon());
      record.setTimestamp(perterbedTimestamp + trueTimeOffset);
      record.setVehicleId(vehicleId);

      record.setActualServiceDate(serviceDate);

      int actualScheduleTime = blockLocation.getScheduledTime();
      record.setActualScheduleTime(actualScheduleTime + trueTimeOffset);

      record.setActualDsc(dsc);
      record.setActualBlockLat(location.getLat());
      record.setActualBlockLon(location.getLon());
      // TODO setActualStatus?

      task.addRecord(record);

      scheduleTime += 30 + random.nextGaussian() * 2;
    }

  }

  private BlockInstance sampleNearbyBlocks(BlockInstance currentBlock,
      List<BlockInstance> activeBlocks) {

    int bsize = activeBlocks.size();

    if (bsize < 1) {
      _log.warn("sampler: not enough blocks to sample");
      return null;
    }

    if (currentBlock == null) {
      _log.warn("sampler: current block is null");
      return null;
    }

    int currentIdx = activeBlocks.lastIndexOf(currentBlock);

    if (currentIdx < 0) {

      activeBlocks.add(currentBlock);
      currentIdx = activeBlocks.lastIndexOf(currentBlock);

    }

    double[] tmpd = new double[bsize];
    double[] obs = new double[bsize];
    for (int i = 0; i < bsize; ++i) {
      if (i == currentIdx) {
        // TODO Make this configurable, and explain
        tmpd[i] = 5;
      } else {
        tmpd[i] = 1;
      }
      obs[i] = i;

      if (bsize > 1)
        _log.info(i + ": " + activeBlocks.get(i).getBlock().getBlock().getId());
    }
    DirichletGen rdg = new DirichletGen(streamArr, tmpd);

    double[] probs = new double[bsize];
    rdg.nextPoint(probs);
    DiscreteDistribution emd = new DiscreteDistribution(obs, probs, bsize);

    double u = ung.nextDouble();
    double newIdx = emd.inverseF(u);

    if (newIdx != currentIdx)
      _log.info(u + ": sampled new idx=" + newIdx);

    return (int) newIdx != currentIdx ? activeBlocks.get((int) newIdx) : null;
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
      boolean isRunBased, boolean realtime, boolean fillActualProperties,
      Properties properties) {

    Random random = new Random();

    SimulatorTask task = new SimulatorTask();

    String tag = generateBlockServiceDateTag(blockId, serviceDate);
    task.addTag(tag);

    task.setPauseOnStart(false);
    task.setRunInRealtime(realtime);
    task.setBypassInference(bypassInference);
    task.setFillActualProperties(fillActualProperties);

    BlockInstance blockInstance = _blockCalendarService.getBlockInstance(
        blockId, serviceDate);
    BlockConfigurationEntry block = blockInstance.getBlock();

    int scheduleTime = (int) ((actualTime - serviceDate) / 1000);
    int shiftStartTime = getShiftStartTimeProperty(properties);
    scheduleTime -= shiftStartTime;

    AgencyAndId vehicleId = getVehicleIdProperty(random, properties,
        blockId.getAgencyId());

    double locationSigma = getLocationSigma(properties);
    SortedMap<Double, Integer> scheduleDeviations = getScheduleDeviations(properties);

    if (isRunBased) {

      ScheduledBlockLocation blockLocation = _scheduledBlockLocationService
          .getScheduledBlockLocationFromScheduledTime(block, scheduleTime);

      BlockTripEntry trip = blockLocation.getActiveTrip();

      AgencyAndId tripId = trip.getTrip().getId();

      String runId = _runService.getInitialRunForTrip(tripId);

      int reliefTime = _runService.getReliefTimeForTrip(tripId);

      // if there is a relief and our schedule time is past the relief
      // then use that run
      if (reliefTime > 0 && reliefTime <= scheduleTime)
        runId = _runService.getReliefRunForTrip(tripId);

      long timestamp = serviceDate + (scheduleTime + shiftStartTime) * 1000;

      RunTripEntry runTrip = _runService.getRunTripEntryForRunAndTime(
          tripId.getAgencyId(), runId, timestamp);

      int trueTimeOffset = 0;

      // FIXME if no scheduled run trip, take first available
      if (runTrip == null) {
        List<RunTripEntry> runTripEntries = _runService
            .getRunTripEntriesForTime(tripId.getAgencyId(), timestamp);

        boolean resetTime = false;

        // so the time lookup has failed us. start at a different time
        if (runTripEntries == null || runTripEntries.isEmpty()) {
          _log.warn("Couldn't get a run for time=" + timestamp
              + ".  Taking a run for this block and shifting the time...");
          runTripEntries = _runService.getRunTripEntriesForRun(runId);
          resetTime = true;
        }

        if (runTripEntries == null || runTripEntries.isEmpty())
          return -1;

        // we could find a runTrip nearby in time...
        runTrip = runTripEntries.get(0);

        if (resetTime) {
          // we'll use this later to make our run appear as scheduled
          // for the time we wanted
          trueTimeOffset = scheduleTime - runTrip.getStartTime();
          scheduleTime = runTrip.getStartTime();
        }

      }

      _log.info("Using runTrip=" + runTrip.toString());

      // TODO pass these along to run-simulation method
      // List<Double> transitionParams =
      // getRunTransitionParams(properties);

      generateRunSim(random, task, runTrip, serviceDate, scheduleTime,
          shiftStartTime, trueTimeOffset, scheduleDeviations, locationSigma,
          vehicleId);

      return addTask(task);
    }

    List<BlockStopTimeEntry> stopTimes = block.getStopTimes();

    BlockStopTimeEntry firstStopTime = stopTimes.get(0);
    BlockStopTimeEntry lastStopTime = stopTimes.get(stopTimes.size() - 1);
    int firstTime = firstStopTime.getStopTime().getArrivalTime();
    int lastTime = lastStopTime.getStopTime().getDepartureTime();

    if (isIncludeStartTime(properties))
      scheduleTime = firstTime;

    scheduleTime = Math.max(firstTime, scheduleTime);

    while (scheduleTime <= lastTime) {

      NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();

      ScheduledBlockLocation blockLocation = _scheduledBlockLocationService
          .getScheduledBlockLocationFromScheduledTime(block, scheduleTime);

      BlockTripEntry trip = blockLocation.getActiveTrip();

      AgencyAndId tripId = trip.getTrip().getId();

      /**
       * Not in service?
       */
      if (blockLocation == null)
        return -1;

      CoordinatePoint location = blockLocation.getLocation();
      record.setActualBlockId(AgencyAndIdLibrary.convertToString(blockId));
      record.setActualDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
      int actualScheduleTime = blockLocation.getScheduledTime();

      String dsc = _destinationSignCodeService
          .getDestinationSignCodeForTripId(tripId);
      if (dsc == null)
        dsc = "0";

      int scheduleDeviation = 0;

      if (!scheduleDeviations.isEmpty()) {
        double ratio = (scheduleTime - firstTime)
            / ((double) (lastTime - firstTime));
        scheduleDeviation = (int) InterpolationLibrary.interpolate(
            scheduleDeviations, ratio, EOutOfRangeStrategy.LAST_VALUE);
      }

      long timestamp = serviceDate
          + (scheduleTime + shiftStartTime + scheduleDeviation) * 1000;

      CoordinatePoint p = applyLocationNoise(location.getLat(),
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
    for (Object tag : task.getTags()) {
      List<SimulatorTask> tasks = getTasksForTag(tag, true);
      synchronized (tasks) {
        tasks.add(task);
      }
    }
    Future<?> future = _executor.submit(task);
    task.setFuture(future);

    return task.getId();
  }

  private List<SimulatorTask> getTasksForTag(Object tag, boolean create) {
    List<SimulatorTask> tasks = _tasksByTag.get(tag);
    if (tasks == null && create) {
      ArrayList<SimulatorTask> newTasks = new ArrayList<SimulatorTask>();
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

    List<Double> transitionParams = new ArrayList<Double>();

    String raw = properties.getProperty(ARG_RUN_TRANSITION_PARAMS);

    for (String token : raw.split(",")) {

      String[] kvp = token.split("=");

      if (kvp.length != 1)
        throw new IllegalArgumentException("invalid run-transition params: "
            + raw);

      double param = Double.parseDouble(kvp[0]);

      transitionParams.add(param);
    }

    return transitionParams;
  }

  private int getShiftStartTimeProperty(Properties properties) {
    int shiftStartTime = 0;
    if (properties.containsKey(ARG_SHIFT_START_TIME))
      shiftStartTime = Integer.parseInt(properties
          .getProperty(ARG_SHIFT_START_TIME)) * 60;
    return shiftStartTime;
  }

  private AgencyAndId getVehicleIdProperty(Random random,
      Properties properties, String agencyId) {

    AgencyAndId vehicleId = null;

    if (properties.containsKey(ARG_VEHICLE_ID))
      vehicleId = AgencyAndIdLibrary.convertFromString(properties
          .getProperty(ARG_VEHICLE_ID));

    if (vehicleId == null) {
      int vid = random.nextInt(9999);
      vehicleId = new AgencyAndId(agencyId, vid + "");
    }
    return vehicleId;
  }

  private double getLocationSigma(Properties properties) {
    double locationSigma = 0.0;
    if (properties.containsKey(ARG_LOCATION_SIGMA))
      locationSigma = Double.parseDouble(properties
          .getProperty(ARG_LOCATION_SIGMA));
    return locationSigma;
  }

  private CoordinatePoint applyLocationNoise(double lat, double lon,
      double locationSigma, Random random) {
    if (locationSigma == 0)
      return new CoordinatePoint(lat, lon);
    double latRadiusInMeters = random.nextGaussian() * locationSigma;
    double lonRadiusInMeters = random.nextGaussian() * locationSigma;
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lon,
        Math.abs(latRadiusInMeters), Math.abs(lonRadiusInMeters));
    double latRadius = (bounds.getMaxLat() - bounds.getMinLat()) / 2
        * (latRadiusInMeters / Math.abs(latRadiusInMeters));
    double lonRadius = (bounds.getMaxLon() - bounds.getMinLon()) / 2
        * (lonRadiusInMeters / Math.abs(lonRadiusInMeters));
    return new CoordinatePoint(lat + latRadius, lon + lonRadius);
  }

  private SortedMap<Double, Integer> getScheduleDeviations(Properties properties) {

    SortedMap<Double, Integer> scheduleDeviations = new TreeMap<Double, Integer>();

    if (properties.containsKey(ARG_SCHEDULE_DEVIATIONS)) {
      String raw = properties.getProperty(ARG_SCHEDULE_DEVIATIONS);
      for (String token : raw.split(",")) {
        String[] kvp = token.split("=");
        if (kvp.length != 2)
          throw new IllegalArgumentException(
              "invalid schedule deviations spec: " + raw);
        double ratio = Double.parseDouble(kvp[0]);
        int deviation = Integer.parseInt(kvp[1]) * 60;
        scheduleDeviations.put(ratio, deviation);
      }
    }

    return scheduleDeviations;
  }

  private boolean isIncludeStartTime(Properties properties) {
    if (!properties.containsKey(ARG_INCLUDE_START))
      return false;
    String value = properties.getProperty(ARG_INCLUDE_START).toLowerCase();
    return value.equals("true") || value.equals("yes") || value.equals("1");
  }
}
