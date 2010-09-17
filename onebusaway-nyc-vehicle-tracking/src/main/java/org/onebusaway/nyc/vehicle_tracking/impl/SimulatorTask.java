package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;

class SimulatorTask implements Runnable, EntityHandler {

  private List<NycTestLocationRecord> _records = new ArrayList<NycTestLocationRecord>();

  private AtomicInteger _recordsProcessed = new AtomicInteger();

  private NycTestLocationRecord _mostRecentRecord = null;
  
  private VehicleLocationService _vehicleLocationService;

  private int _id;

  private Future<?> _future;

  private volatile boolean _paused;

  private volatile boolean _stepForOne = false;

  private boolean _runInRealtime = false;

  private boolean _shiftStartTime = false;

  private long _startTimeOffset = 0;

  private Set<Object> _tags = new HashSet<Object>();

  public SimulatorTask() {

  }
  
  public void setVehicleLocationService(VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
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

  public void setShiftStartTime(boolean shiftStartTime) {
    _shiftStartTime = shiftStartTime;
  }

  public void addTag(Object tag) {
    _tags.add(tag);
  }

  public Set<Object> getTags() {
    return _tags;
  }

  public void addRecord(NycTestLocationRecord record) {
    if (_shiftStartTime) {
      if (_records.isEmpty())
        _startTimeOffset = System.currentTimeMillis() - record.getTimestamp();
      record.setTimestamp(record.getTimestamp() + _startTimeOffset);
    }
    _records.add(record);
  }

  public List<NycTestLocationRecord> getRecords() {
    return _records;
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
    if (particles != null) {
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

    addRecord(record);
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