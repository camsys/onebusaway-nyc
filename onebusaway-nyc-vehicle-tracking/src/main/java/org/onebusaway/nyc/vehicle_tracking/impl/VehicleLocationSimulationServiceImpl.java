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
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.simulator.SimulatorTask;
import org.onebusaway.nyc.vehicle_tracking.model.NycInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.csv.TabTokenizerStrategy;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationSimulationSummary;
import org.onebusaway.nyc.vehicle_tracking.services.RunService;
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
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.EOutOfRangeStrategy;
import org.onebusaway.utility.InterpolationLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehicleLocationSimulationServiceImpl implements
    VehicleLocationSimulationService {

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
    } else if (traceType.equals("NycInferredLocationRecord")) {
      reader.readEntities(NycInferredLocationRecord.class, traceInputStream);
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
  public VehicleLocationDetails getParticleDetails(int taskId,
      int particleId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getParticleDetails(particleId);
    return null;
  }

  @Override
  public List<NycInferredLocationRecord> getResultRecords(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getResults();
    return null;
  }

  @Override
  public List<NycInferredLocationRecord> getSimulationRecords(int taskId) {
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

  private int getRunTripLastTime(RunTripEntry runTrip) {
	  
    // this run could end before the last stop
    int lastTime = _runService.getReliefTimeForTrip(runTrip.getTripEntry().getId());

    if (lastTime <= 0) {
      List<StopTimeEntry> stopTimes = runTrip.getTripEntry().getStopTimes();
      StopTimeEntry lastStopTime = stopTimes.get(stopTimes.size() - 1);
      lastTime = lastStopTime.getDepartureTime();
    }
  
    return lastTime;
  }
  
  public void generateRunSim(Random random, 
      SimulatorTask task, 
      RunTripEntry runTrip, 
      long serviceDate, int scheduleTime, 
      int shiftStartTime, SortedMap<Double, Integer> scheduleDeviations, 
      double locationSigma,
      AgencyAndId vehicleId) {

    List<RunTripEntry> rtes = _runService.getRunTripEntriesForRun(runTrip.getRun());

    String agencyId = runTrip.getTripEntry().getId().getAgencyId();
    

    int firstTime = runTrip.getStartTime();

    RunTripEntry lastRunTrip = rtes.get(rtes.size()-1);
    
    int lastTime = getRunTripLastTime(lastRunTrip);
    
    // this is the last time of the current run-trip
    int runningLastTime = 0;

    /*
     *  We could go by run trip entry sequence or perturbed schedule-time
     *  evolution.  The latter is chosen, for now.
     */
    while (scheduleTime <= lastTime) { 

      NycInferredLocationRecord record = new NycInferredLocationRecord();

      long unperturbedTimestamp = serviceDate + (scheduleTime + shiftStartTime) * 1000;

      // FIXME use combination of serviceDate and scheduleTime?  either way, use this...
      //RunTripEntry rte = _runService.getRunTripEntryForRunAndTime(agencyId, runTrip.getRun(), unperturbedTimestamp);
      
      if (scheduleTime >= runningLastTime) {
	      runTrip = _runService.getNextEntry(runTrip);
	      runningLastTime = getRunTripLastTime(runTrip);
      }

      if (runTrip == null)
    	  break;
      
      TripEntry trip = runTrip.getTripEntry();
      
      
      AgencyAndId tripId = trip.getId();

      record.setActualTripId(AgencyAndIdLibrary.convertToString(tripId));
      
      // TODO simulate run changes

      String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(tripId);
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

      // FIXME is this level of indirection necessary for just the location?
      // we should use the geometry to make a TRULY run-based sim?
      BlockEntry blockEntry = trip.getBlock();

      // especially here, where we're just selecting the first configuration to
      // get a location.
      ScheduledBlockLocation blockLocation = null;
      for (BlockConfigurationEntry block : blockEntry.getConfigurations()) {
    	  
	      blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
	          block, scheduleTime);
	      
	      if (blockLocation != null)
	    	  break;
      }
      
      if (blockLocation == null)
    	  break;

      CoordinatePoint location = blockLocation.getLocation();
      
      record.setActualBlockId(AgencyAndIdLibrary.convertToString(blockEntry.getId()));
      record.setActualDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
      
      CoordinatePoint p = applyLocationNoise(location.getLat(),
          location.getLon(), locationSigma, random);

      record.setDsc(dsc);
      record.setLat(p.getLat());
      record.setLon(p.getLon());
      record.setTimestamp(perterbedTimestamp);
      record.setVehicleId(vehicleId);

      record.setActualServiceDate(serviceDate);
      
      
      int actualScheduleTime = blockLocation.getScheduledTime();
      record.setActualScheduleTime(actualScheduleTime);
      
      record.setActualDsc(dsc);
      record.setActualBlockLat(location.getLat());
      record.setActualBlockLon(location.getLon());
      // TODO setActualStatus?

      task.addRecord(record);

      // TODO confirm that this is reasonable for vehicle update intervals
      scheduleTime += 30 + random.nextGaussian() * 10;
    }
      

  }


  /*
   * We make the distinction between whether a block or a run is driving
   * this simulation.  However, we always initialize the simulation by a run that
   * is active for the "initialization" block, and thus trip, we were passed. 
   * Really, what we have is block-inspired run simulations...
   * 
   */
  @Override
  public int addSimulationForBlockInstance(AgencyAndId blockId,
      long serviceDate, long actualTime, boolean bypassInference, 
      boolean isRunBased, boolean fillActualProperties, Properties properties) {

    Random random = new Random();

    SimulatorTask task = new SimulatorTask();

    
    String tag = generateBlockServiceDateTag(blockId, serviceDate);
    task.addTag(tag);

    task.setPauseOnStart(false);
    task.setRunInRealtime(true);
    task.setBypassInference(bypassInference);
    task.setFillActualProperties(fillActualProperties);

    BlockInstance blockInstance = _blockCalendarService.getBlockInstance(
        blockId, serviceDate);
    BlockConfigurationEntry block = blockInstance.getBlock();

    int scheduleTime = (int) ((actualTime - serviceDate) / 1000);
    int shiftStartTime = getShiftStartTimeProperty(properties);
    scheduleTime -= shiftStartTime;

    AgencyAndId vehicleId = getVehicleIdProperty(random, properties, blockId.getAgencyId());

    double locationSigma = getLocationSigma(properties);
    SortedMap<Double, Integer> scheduleDeviations = getScheduleDeviations(properties);


    if (isRunBased) {
    
      ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          block, scheduleTime);

      BlockTripEntry trip = blockLocation.getActiveTrip();

      AgencyAndId tripId = trip.getTrip().getId();

      String runId = _runService.getInitialRunForTrip(tripId);

      long nowTime = System.currentTimeMillis();
	  
      long timestamp = serviceDate + (scheduleTime + shiftStartTime) * 1000;

      RunTripEntry runTrip = _runService.getRunTripEntryForRunAndTime(tripId.getAgencyId(), runId, timestamp);
      
      // FIXME if no scheduled run trip, take first available
      if (runTrip == null) {
        List<RunTripEntry> runTripEntries = _runService.getRunTripEntriesForTime(
        		tripId.getAgencyId(), timestamp);

        boolean resetTime = false;
        
        // so the time lookup has failed us.  start at a different time 
        if (runTripEntries == null || runTripEntries.isEmpty()) {
          runTripEntries = _runService.getRunTripEntriesForRun(runId);
          resetTime = true;
        }
        

        if (runTripEntries == null || runTripEntries.isEmpty())
          return -1;

        runTrip = runTripEntries.get(0);
        
        if(resetTime)
		    scheduleTime = runTrip.getStartTime();
      }

      // TODO pass these along to run-simulation method
      //List<Double> transitionParams = getRunTransitionParams(properties);
      
      generateRunSim(random, task, runTrip, serviceDate, 
          scheduleTime, shiftStartTime, scheduleDeviations, 
          locationSigma, vehicleId);

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
    	
      NycInferredLocationRecord record = new NycInferredLocationRecord();
		  
      ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          block, scheduleTime);

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

      String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(tripId);
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
   *  These correspond to the centrality of the current run.
   */
  private List<Double> getRunTransitionParams(Properties properties) {

    List<Double> transitionParams = new ArrayList<Double>();

    String raw = properties.getProperty(ARG_RUN_TRANSITION_PARAMS);

    for (String token : raw.split(",")) {

      String[] kvp = token.split("=");

      if (kvp.length != 1)
        throw new IllegalArgumentException("invalid run-transition params: " + raw);

      double param = Double.parseDouble(kvp[0]);

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

  private AgencyAndId getVehicleIdProperty(Random random, Properties properties, String agencyId) {

    AgencyAndId vehicleId = null;

    if (properties.containsKey(ARG_VEHICLE_ID))
      vehicleId = AgencyAndIdLibrary.convertFromString(properties.getProperty(ARG_VEHICLE_ID));

    if (vehicleId == null) {
      int vid = random.nextInt(9999);
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
