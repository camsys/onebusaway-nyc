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
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockTripEntry;
import org.onebusaway.utility.EOutOfRangeStrategy;
import org.onebusaway.utility.InterpolationLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehicleLocationSimulationServiceImpl implements
    VehicleLocationSimulationService {

  private static final String ARG_VEHICLE_ID = "vehicle_id";

  private static final String ARG_SHIFT_START_TIME = "shift_start_time";

  private static final String ARG_LOCATION_SIGMA = "location_sigma";

  private static final String ARG_SCHEDULE_DEVIATIONS = "schedule_deviations";

  private static final String ARG_INCLUDE_START = "include_start";

  private int _numberOfThreads = 20;

  private ExecutorService _executor;

  private VehicleLocationService _vehicleLocationService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private DestinationSignCodeService _destinationSignCodeService;

  private Map<Integer, SimulatorTask> _tasks = new ConcurrentHashMap<Integer, SimulatorTask>();

  private ConcurrentMap<Object, List<SimulatorTask>> _tasksByTag = new ConcurrentHashMap<Object, List<SimulatorTask>>();

  private AtomicInteger _taskIndex = new AtomicInteger();

  private BlockCalendarService _blockCalendarService;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
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
  public int simulateLocationsFromTrace(InputStream traceInputStream,
      boolean runInRealtime, boolean pauseOnStart, boolean shiftStartTime)
      throws IOException {

    SimulatorTask task = new SimulatorTask();
    task.setPauseOnStart(pauseOnStart);
    task.setRunInRealtime(runInRealtime);
    task.setShiftStartTime(shiftStartTime);

    CsvEntityReader reader = new CsvEntityReader();
    reader.addEntityHandler(task);
    reader.readEntities(NycTestLocationRecord.class, traceInputStream);
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
  public VehicleLocationSimulationDetails getSimulationDetails(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getDetails();
    return null;
  }

  @Override
  public List<NycTestLocationRecord> getResultRecords(int taskId) {
    SimulatorTask task = _tasks.get(taskId);
    if (task != null)
      return task.getResults();
    return null;
  }

  @Override
  public List<NycTestLocationRecord> getSimulationRecords(int taskId) {
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

  @Override
  public int addSimulationForBlockInstance(AgencyAndId blockId,
      long serviceDate, long actualTime, Properties properties) {

    Random random = new Random();

    SimulatorTask task = new SimulatorTask();

    String tag = generateBlockServiceDateTag(blockId, serviceDate);
    task.addTag(tag);

    task.setPauseOnStart(false);
    task.setRunInRealtime(true);

    BlockInstance blockInstance = _blockCalendarService.getBlockInstance(
        blockId, serviceDate);
    BlockConfigurationEntry block = blockInstance.getBlock();
    List<BlockStopTimeEntry> stopTimes = block.getStopTimes();
    int scheduleTime = (int) ((actualTime - serviceDate) / 1000);

    int shiftStartTime = getShiftStartTimeProperty(properties);
    String vehicleId = getVehicleIdProperty(random, properties);
    double locationSigma = getLocationSigma(properties);
    SortedMap<Double, Integer> scheduleDeviations = getScheduleDeviations(properties);

    scheduleTime -= shiftStartTime;

    BlockStopTimeEntry firstStopTime = stopTimes.get(0);
    BlockStopTimeEntry lastStopTime = stopTimes.get(stopTimes.size() - 1);
    int firstTime = firstStopTime.getStopTime().getArrivalTime();
    int lastTime = lastStopTime.getStopTime().getDepartureTime();

    if (isIncludeStartTime(properties))
      scheduleTime = firstTime;

    scheduleTime = Math.max(firstTime, scheduleTime);

    while (scheduleTime <= lastTime) {

      ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          stopTimes, scheduleTime);

      /**
       * Not in service?
       */
      if (blockLocation == null)
        return -1;

      BlockTripEntry trip = blockLocation.getActiveTrip();
      String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(trip.getTrip().getId());
      if (dsc == null)
        dsc = "0";

      CoordinatePoint location = blockLocation.getLocation();

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

      NycTestLocationRecord record = new NycTestLocationRecord();
      record.setDsc(dsc);
      record.setLat(p.getLat());
      record.setLon(p.getLon());
      record.setTimestamp(timestamp);
      record.setVehicleId(vehicleId);

      record.setActualBlockId(AgencyAndIdLibrary.convertToString(blockId));
      record.setActualDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
      record.setActualDsc(dsc);
      record.setActualLat(location.getLat());
      record.setActualLon(location.getLon());

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
    task.setVehicleLocationService(_vehicleLocationService);
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

  private int getShiftStartTimeProperty(Properties properties) {
    int shiftStartTime = 0;
    if (properties.containsKey(ARG_SHIFT_START_TIME))
      shiftStartTime = Integer.parseInt(properties.getProperty(ARG_SHIFT_START_TIME)) * 60;
    return shiftStartTime;
  }

  private String getVehicleIdProperty(Random random, Properties properties) {

    String vehicleId = null;

    if (properties.containsKey(ARG_VEHICLE_ID))
      vehicleId = properties.getProperty(ARG_VEHICLE_ID);

    if (vehicleId == null) {
      int vid = random.nextInt(9999);
      vehicleId = Integer.toString(vid);
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
