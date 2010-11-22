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
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulatorTask implements Runnable, EntityHandler {

  private static Logger _log = LoggerFactory.getLogger(SimulatorTask.class);

  private List<NycTestLocationRecord> _records = new ArrayList<NycTestLocationRecord>();

  private List<NycTestLocationRecord> _results = new ArrayList<NycTestLocationRecord>();

  private AtomicInteger _recordsProcessed = new AtomicInteger();

  private NycTestLocationRecord _mostRecentRecord = null;

  private VehicleLocationService _vehicleLocationService;

  private int _id;

  private Future<?> _future;

  private volatile boolean _paused;

  private volatile boolean _stepForOne = false;

  private int _stepRecordIndex = -1;

  private boolean _runInRealtime = false;

  private boolean _shiftStartTime = false;

  private boolean _bypassInference = false;

  private int _minimumRecordInterval = 0;

  private long _startTimeOffset = 0;

  private Set<Object> _tags = new HashSet<Object>();

  private String _vehicleId = null;

  public SimulatorTask() {

  }

  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
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

  public void setMinimumRecordInterval(int minimumRecordInterval) {
    _minimumRecordInterval = minimumRecordInterval;
  }

  public void setBypassInference(boolean bypassInference) {
    _bypassInference = bypassInference;
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

    // Should we prune a record that comes too close behind a previous record?
    if (!_records.isEmpty()) {
      NycTestLocationRecord previous = _records.get(_records.size() - 1);
      if ((record.getTimestamp() - previous.getTimestamp()) / 1000 < _minimumRecordInterval)
        return;
    }

    _records.add(record);
    String vid = record.getVehicleId();
    if (_vehicleId != null) {
      if (!_vehicleId.equals(vid))
        throw new IllegalArgumentException(
            "simulation should only include records for same vehicleId: expected="
                + _vehicleId + " actual=" + vid);
    } else {
      _vehicleId = vid;
    }
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

  public synchronized void step(int recordIndex) {
    _paused = true;
    _stepForOne = true;
    _stepRecordIndex = recordIndex;
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
    List<Particle> particles = _vehicleLocationService.getCurrentParticlesForVehicleId(_vehicleId);
    if (particles != null) {
      Collections.sort(particles);
      details.setParticles(particles);
    }
    return details;
  }

  public VehicleLocationSimulationDetails getParticleDetails(int particleId) {
    VehicleLocationSimulationDetails details = new VehicleLocationSimulationDetails();
    details.setId(_id);
    details.setLastObservation(_mostRecentRecord);
    List<Particle> particles = _vehicleLocationService.getCurrentParticlesForVehicleId(_vehicleId);
    if (particles != null) {
      for (Particle p : particles) {
        if (p.getIndex() == particleId) {
          List<Particle> history = new ArrayList<Particle>();
          while (p != null) {
            history.add(p);
            p = p.getParent();
          }
          details.setParticles(history);
          break;
        }
      }
    }
    return details;
  }

  public List<NycTestLocationRecord> getResults() {
    synchronized (_results) {
      return new ArrayList<NycTestLocationRecord>(_results);
    }
  }

  public void resetAllVehiclesAppearingInRecordData() {
    Set<String> vehicleIds = new HashSet<String>();

    for (NycTestLocationRecord record : _records)
      vehicleIds.add(record.getVehicleId());

    for (String vehicleId : vehicleIds)
      _vehicleLocationService.resetVehicleLocation(vehicleId);
  }

  /****
   * {@link Runnable} Interface
   ****/

  @Override
  public void run() {

    try {
      resetAllVehiclesAppearingInRecordData();

      int recordIndex = 0;

      for (NycTestLocationRecord record : _records) {

        if (Thread.interrupted())
          return;

        if (shouldExitAfterSimulatedWait(record))
          return;

        if (shouldExitAfterPossiblePause())
          return;

        _log.info("sending record: index=" + recordIndex);
        recordIndex++;

        if (_bypassInference) {
          _vehicleLocationService.handleNycTestLocationRecord(record);
        } else {
          _vehicleLocationService.handleVehicleLocation(record.getTimestamp(),
              record.getVehicleId(), record.getLat(), record.getLon(),
              record.getDsc(), true);
        }

        if (shouledExitAfterWaitingForInferenceToComplete(record))
          return;

        _mostRecentRecord = record;
        _recordsProcessed.incrementAndGet();
      }
    } catch (Throwable ex) {
      _log.error("error in simulation run", ex);
    }
  }

  /****
   * {@link EntityHandler} Interface
   ****/

  @Override
  public void handleEntity(Object bean) {
    if (bean instanceof NycVehicleLocationRecord) {
      NycVehicleLocationRecord vlr = (NycVehicleLocationRecord) bean;
      NycTestLocationRecord record = new NycTestLocationRecord();
      record.setDsc(vlr.getDestinationSignCode());
      record.setLat(vlr.getLatitude());
      record.setLon(vlr.getLongitude());
      record.setTimestamp(vlr.getTimeReceived());
      record.setVehicleId(vlr.getVehicleId().getId());
      bean = record;
    }
    NycTestLocationRecord record = (NycTestLocationRecord) bean;
    addRecord(record);
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

    if (_stepRecordIndex == -1
        || _stepRecordIndex < _recordsProcessed.intValue()) {
      _stepForOne = false;
      _stepRecordIndex = -1;
    }

    return false;
  }

  private boolean shouledExitAfterWaitingForInferenceToComplete(
      NycTestLocationRecord record) {

    for (int i = 0; i < 10; i++) {

      if (processResultRecord(record))
        return false;

      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        return true;
      }
    }

    _log.warn("vehicle location inference never completed");
    return true;
  }

  private boolean processResultRecord(NycTestLocationRecord record) {

    VehicleLocationRecord result = _vehicleLocationService.getVehicleLocationForVehicle(record.getVehicleId());

    if (result == null || result.getTimeOfRecord() < record.getTimestamp())
      return false;

    NycTestLocationRecord rr = new NycTestLocationRecord();

    if (result.getBlockId() != null)
      rr.setActualBlockId(result.getBlockId().toString());

    rr.setActualDistanceAlongBlock(result.getDistanceAlongBlock());

    // Is this right?
    rr.setActualDsc(record.getDsc());

    rr.setActualLat(result.getCurrentLocationLat());
    rr.setActualLon(result.getCurrentLocationLon());

    if (result.getPhase() != null)
      rr.setActualPhase(result.getPhase().toString());

    rr.setActualStatus(result.getStatus());
    rr.setActualServiceDate(result.getServiceDate());

    rr.setDsc(record.getDsc());
    rr.setLat(record.getLat());
    rr.setLon(record.getLon());

    rr.setTimestamp(result.getTimeOfRecord());
    rr.setVehicleId(record.getVehicleId());

    synchronized (_results) {
      _results.add(rr);
    }

    return true;
  }

}