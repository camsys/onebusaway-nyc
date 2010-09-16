package org.onebusaway.nyc.vehicle_tracking.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.onebusaway.transit_data_federation.services.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.realtime.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.realtime.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehicleLocationSimulationServiceImpl implements
    VehicleLocationSimulationService {

  private int _numberOfThreads = 1;

  private ExecutorService _executor;

  private VehicleLocationService _vehicleLocationService;

  private TransitGraphDao _transitGraphDao;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private DestinationSignCodeService _destinationSignCodeService;

  private Map<Integer, SimulatorTask> _tasks = new ConcurrentHashMap<Integer, SimulatorTask>();

  private ConcurrentMap<Object, List<SimulatorTask>> _tasksByTag = new ConcurrentHashMap<Object, List<SimulatorTask>>();

  private AtomicInteger _taskIndex = new AtomicInteger();

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
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
      boolean runInRealtime, boolean pauseOnStart) throws IOException {

    SimulatorTask task = new SimulatorTask();
    task.setPauseOnStart(pauseOnStart);
    task.setRunInRealtime(runInRealtime);

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
      long serviceDate, String parameters) {

    Random random = new Random();

    SimulatorTask task = new SimulatorTask();

    String tag = generateBlockServiceDateTag(blockId, serviceDate);
    task.addTag(tag);

    task.setPauseOnStart(false);
    task.setRunInRealtime(true);

    BlockEntry blockEntry = _transitGraphDao.getBlockEntryForId(blockId);
    List<StopTimeEntry> stopTimes = blockEntry.getStopTimes();
    int scheduleTime = (int) ((System.currentTimeMillis() - serviceDate) / 1000);

    String vehicleId = null;

    if (vehicleId == null) {
      int vid = random.nextInt(9999);
      vehicleId = Integer.toString(vid);
    }

    StopTimeEntry lastStopTime = stopTimes.get(stopTimes.size() - 1);
    int lastTime = lastStopTime.getDepartureTime();

    while (scheduleTime <= lastTime) {

      ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockPositionFromScheduledTime(
          stopTimes, scheduleTime);

      /**
       * Not in service?
       */
      if (blockLocation == null)
        return -1;

      TripEntry trip = blockLocation.getActiveTrip();
      String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(trip.getId());
      if (dsc == null)
        dsc = "0";

      CoordinatePoint location = blockLocation.getLocation();

      long timestamp = serviceDate + scheduleTime * 1000;

      NycTestLocationRecord record = new NycTestLocationRecord();
      record.setDsc(dsc);
      record.setLat(location.getLat());
      record.setLon(location.getLon());
      record.setTimestamp(timestamp);
      record.setVehicleId(vehicleId);

      task.addRecord(record);

      scheduleTime += 90 + random.nextGaussian() * 30;
    }

    return addTask(task);
  }

  /****
   * Private Methods
   ****/

  private int addTask(SimulatorTask task) {
    task.setId(_taskIndex.incrementAndGet());
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
      ArrayList<SimulatorTask> newTasks = new ArrayList<VehicleLocationSimulationServiceImpl.SimulatorTask>();
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

  private class SimulatorTask implements Runnable, EntityHandler {

    private List<NycTestLocationRecord> _records = new ArrayList<NycTestLocationRecord>();

    private AtomicInteger _recordsProcessed = new AtomicInteger();

    private NycTestLocationRecord _mostRecentRecord = null;

    private int _id;

    private Future<?> _future;

    private volatile boolean _paused;

    private volatile boolean _stepForOne = false;

    private boolean _runInRealtime = false;

    private Set<Object> _tags = new HashSet<Object>();

    public SimulatorTask() {

    }

    public void setId(int id) {
      _id = id;
    }

    public int getId() {
      return _id;
    }

    public void setFuture(Future<?> future) {
      _future = future;
    }

    public Future<?> getFuture() {
      return _future;
    }

    public void setPauseOnStart(boolean pauseOnStart) {
      _paused = pauseOnStart;
    }

    public void setRunInRealtime(boolean runInRealtime) {
      _runInRealtime = runInRealtime;
    }

    public void addTag(Object tag) {
      _tags.add(tag);
    }

    public Set<Object> getTags() {
      return _tags;
    }

    public void addRecord(NycTestLocationRecord record) {
      _records.add(record);
    }

    public synchronized void toggle() {
      _paused = !_paused;
      notify();
    }

    public synchronized void step() {
      _paused = true;
      _stepForOne = true;
      notify();
    }

    public VehicleLocationSimulationSummary getSummary() {
      VehicleLocationSimulationSummary summary = new VehicleLocationSimulationSummary();
      summary.setId(_id);
      summary.setNumberOfRecordsProcessed(_recordsProcessed.get());
      summary.setNumberOfRecordsTotal(_records.size());
      summary.setMostRecentRecord(_mostRecentRecord);
      summary.setPaused(_paused);
      return summary;
    }

    public VehicleLocationSimulationDetails getDetails() {
      VehicleLocationSimulationDetails details = new VehicleLocationSimulationDetails();
      details.setId(_id);
      details.setLastObservation(_mostRecentRecord);
      List<Particle> particles = _vehicleLocationService.getParticlesForVehicleId(_mostRecentRecord.getVehicleId());
      if( particles != null) {
        Collections.sort(particles);
        details.setParticles(particles);
      }
      return details;
    }

    /****
     * {@link Runnable} Interface
     ****/

    @Override
    public void run() {

      resetAllVehiclesAppearingInRecordData();

      for (NycTestLocationRecord record : _records) {

        if (Thread.interrupted())
          return;

        if (shouldExitAfterSimulatedWait(record))
          return;

        if (shouldExitAfterPossiblePause())
          return;

        _vehicleLocationService.handleVehicleLocation(record.getTimestamp(),
            record.getVehicleId(), record.getLat(), record.getLon(),
            record.getDsc());

        _recordsProcessed.incrementAndGet();

        _mostRecentRecord = record;
      }
    }

    /****
     * {@link EntityHandler} Interface
     ****/

    @Override
    public void handleEntity(Object bean) {

      NycTestLocationRecord record = (NycTestLocationRecord) bean;

      // If the gps hasn't changed, ignore the record
      if (!_records.isEmpty()) {
        NycTestLocationRecord prev = _records.get(_records.size() - 1);
        if (prev.getLat() == record.getLat()
            && prev.getLon() == record.getLon())
          return;
      }

      _records.add(record);
    }

    private void resetAllVehiclesAppearingInRecordData() {
      Set<String> vehicleIds = new HashSet<String>();

      for (NycTestLocationRecord record : _records)
        vehicleIds.add(record.getVehicleId());

      for (String vehicleId : vehicleIds)
        _vehicleLocationService.resetVehicleLocation(vehicleId);
    }

    private boolean shouldExitAfterSimulatedWait(NycTestLocationRecord record) {

      if (_runInRealtime && _mostRecentRecord != null) {

        long time = record.getTimestamp() - _mostRecentRecord.getTimestamp();

        try {
          Thread.sleep(time);
        } catch (InterruptedException e) {
          return true;
        }
      }

      return false;
    }

    private synchronized boolean shouldExitAfterPossiblePause() {
      while (_paused && !_stepForOne) {
        try {
          wait();
        } catch (InterruptedException e) {
          return true;
        }
      }
      _stepForOne = false;
      return false;
    }
  }

}
