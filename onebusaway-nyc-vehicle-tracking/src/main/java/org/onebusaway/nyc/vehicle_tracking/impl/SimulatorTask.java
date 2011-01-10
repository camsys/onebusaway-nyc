package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulatorTask implements Runnable, EntityHandler {

  private static Logger _log = LoggerFactory.getLogger(SimulatorTask.class);

  private List<NycTestLocationRecord> _records = new ArrayList<NycTestLocationRecord>();

  private List<NycTestLocationRecord> _results = new ArrayList<NycTestLocationRecord>();

  private Deque<VehicleLocationSimulationDetails> _details = new ArrayDeque<VehicleLocationSimulationDetails>();

  private AtomicInteger _recordsProcessed = new AtomicInteger();

  private NycTestLocationRecord _mostRecentRecord = null;

  private VehicleLocationService _vehicleLocationService;

  private int _id;

  private Future<?> _future;

  private volatile int _recordIndex = 0;

  private volatile boolean _paused;

  private volatile boolean _stepForOne = false;

  private volatile boolean _resetRecordIndex = true;

  private int _stepRecordIndex = -1;

  private boolean _runInRealtime = false;

  private boolean _shiftStartTime = false;

  private boolean _bypassInference = false;

  private boolean _fillActualProperties = false;

  private int _minimumRecordInterval = 0;

  private long _startTimeOffset = 0;

  private Set<Object> _tags = new HashSet<Object>();

  private String _vehicleId = null;

  private boolean _complete = false;

  private boolean _loop;

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

  public void setFillActualProperties(boolean fillActualProperties) {
    _fillActualProperties = fillActualProperties;
  }

  public void setLoop(boolean loop) {
    _loop = loop;
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

    if (_records.isEmpty() && record.locationDataIsMissing()) {
      _log.info("pruning initial record with no gps data");
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

  public synchronized boolean isComplete() {
    return _complete;
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

  public synchronized void restart() {
    _paused = true;
    _stepForOne = false;
    _stepRecordIndex = -1;
    _resetRecordIndex = true;
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

  public VehicleLocationSimulationDetails getDetails(int historyOffset) {
    int index = 0;
    for (Iterator<VehicleLocationSimulationDetails> it = _details.descendingIterator(); it.hasNext();) {
      VehicleLocationSimulationDetails details = it.next();
      if (index == historyOffset)
        return details;
      index++;
    }
    return null;
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
          details.setHistory(true);
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
      synchronized (this) {
        _complete = false;
      }

      runInternal();

    } catch (Throwable ex) {
      _log.error("error in simulation run", ex);
    }

    synchronized (this) {
      _complete = true;
    }
  }

  private void runInternal() {

    while (true) {

      if (Thread.interrupted())
        return;

      if (shouldExitAfterPossiblePause())
        return;

      int nextRecordIndex = getNextRecordIndex();

      if (nextRecordIndex >= _records.size()) {
        if (!_loop)
          return;

        restart();
        checkReset();
        _paused = false;
        nextRecordIndex = getNextRecordIndex();
      }

      NycTestLocationRecord record = _records.get(nextRecordIndex);

      if (shouldExitAfterSimulatedWait(record))
        return;

      _log.info("sending record: index=" + nextRecordIndex);

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

    checkReset();

    while (_paused && !_stepForOne) {
      try {
        wait();
      } catch (InterruptedException e) {
        return true;
      }

      checkReset();
    }

    if (_stepRecordIndex == -1
        || _stepRecordIndex < _recordsProcessed.intValue()) {
      _stepForOne = false;
      _stepRecordIndex = -1;
    }

    return false;
  }

  private synchronized int getNextRecordIndex() {
    checkReset();
    return _recordIndex++;
  }

  private void checkReset() {
    if (_resetRecordIndex) {
      resetAllVehiclesAppearingInRecordData();
      _recordIndex = 0;
      _resetRecordIndex = false;
      _results.clear();
      _recordsProcessed.set(0);
    }
  }

  private boolean shouledExitAfterWaitingForInferenceToComplete(
      NycTestLocationRecord record) {

    for (int i = 0; i < 20; i++) {

      if (processResultRecord(record))
        return false;

      try {
        Thread.sleep(10 * i);
      } catch (InterruptedException ex) {
        return true;
      }
    }

    _log.warn("vehicle location inference never completed");
    return true;
  }

  private boolean processResultRecord(NycTestLocationRecord record) {

    NycTestLocationRecord rr = _vehicleLocationService.getVehicleLocationForVehicle(record.getVehicleId());

    if (rr == null || rr.getTimestamp() < record.getTimestamp())
      return false;

    rr.setVehicleId(_vehicleId);

    if (_fillActualProperties) {
      rr.setActualBlockId(rr.getInferredBlockId());
      rr.setActualTripId(rr.getInferredTripId());
      rr.setActualBlockLat(rr.getInferredBlockLat());
      rr.setActualBlockLon(rr.getInferredBlockLon());
      rr.setActualDistanceAlongBlock(rr.getInferredDistanceAlongBlock());
      rr.setActualScheduleTime(rr.getInferredScheduleTime());
      rr.setActualDsc(rr.getInferredDsc());
      rr.setActualLat(rr.getInferredLat());
      rr.setActualLon(rr.getInferredLon());
      rr.setActualPhase(rr.getInferredPhase());
      rr.setActualServiceDate(rr.getInferredServiceDate());
      rr.setActualStatus(rr.getInferredStatus());

      rr.clearInferredValues();
    }

    synchronized (_results) {
      _results.add(rr);
    }

    VehicleLocationSimulationDetails details = new VehicleLocationSimulationDetails();
    details.setId(_id);
    details.setLastObservation(record);

    List<Particle> weightedParticles = _vehicleLocationService.getCurrentParticlesForVehicleId(_vehicleId);
    if (weightedParticles != null) {
      Collections.sort(weightedParticles, ParticleComparator.INSTANCE);
      details.setParticles(weightedParticles);
    }

    List<Particle> sampledParticles = _vehicleLocationService.getCurrentSampledParticlesForVehicleId(_vehicleId);
    if (sampledParticles != null) {
      Collections.sort(sampledParticles, ParticleComparator.INSTANCE);
      details.setSampledParticles(sampledParticles);
    }
    
    List<JourneyPhaseSummary> summaries = _vehicleLocationService.getCurrentJourneySummariesForVehicleId(_vehicleId);
    details.setSummaries(summaries);

    _details.add(details);
    while (_details.size() > 5)
      _details.removeFirst();

    return true;
  }
}