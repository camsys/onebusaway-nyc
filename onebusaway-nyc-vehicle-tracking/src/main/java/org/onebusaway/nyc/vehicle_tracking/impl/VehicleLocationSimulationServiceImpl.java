package org.onebusaway.nyc.vehicle_tracking.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehicleLocationSimulationServiceImpl implements
    VehicleLocationSimulationService {

  private int _numberOfThreads = 1;

  private ExecutorService _executor;

  private VehicleLocationService _vehicleLocationService;

  private Map<Integer, SimulatorTask> _tasks = new ConcurrentHashMap<Integer, SimulatorTask>();

  private AtomicInteger _taskIndex = new AtomicInteger();

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

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Override
  public int simulateLocationsFromTrace(InputStream traceInputStream,
      boolean runInRealtime, boolean pauseOnStart) throws IOException {

    SimulatorTask task = new SimulatorTask(pauseOnStart);
    task.setRunInRealtime(runInRealtime);
    task.setId(_taskIndex.incrementAndGet());

    CsvEntityReader reader = new CsvEntityReader();
    reader.addEntityHandler(task);
    reader.readEntities(NycTestLocationRecord.class, traceInputStream);
    traceInputStream.close();

    _tasks.put(task.getId(), task);
    Future<?> future = _executor.submit(task);
    task.setFuture(future);
    return task.getId();
  }

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
    }
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

    public SimulatorTask(boolean pauseOnStart) {
      _paused = pauseOnStart;
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

    public void setRunInRealtime(boolean runInRealtime) {
      _runInRealtime = runInRealtime;
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
