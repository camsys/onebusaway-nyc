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
package org.onebusaway.nyc.vehicle_tracking.impl.simulator;

import java.text.DateFormat;
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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.ParticleComparator;
import org.onebusaway.nyc.vehicle_tracking.model.NycInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationSimulationSummary;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatorTask implements Runnable, EntityHandler {

  private static Logger _log = LoggerFactory.getLogger(SimulatorTask.class);

  private static DateFormat _format = DateFormat.getTimeInstance(DateFormat.SHORT);

  private List<NycInferredLocationRecord> _records = new ArrayList<NycInferredLocationRecord>();

  private List<NycInferredLocationRecord> _results = new ArrayList<NycInferredLocationRecord>();

  private Deque<VehicleLocationDetails> _details = new ArrayDeque<VehicleLocationDetails>();

  private AtomicInteger _recordsProcessed = new AtomicInteger();

  private NycInferredLocationRecord _mostRecentRecord = null;

  private VehicleLocationInferenceService _vehicleLocationInferenceService;

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

  private AgencyAndId _vehicleId = null;

  private boolean _complete = false;

  private boolean _loop;

  public SimulatorTask() {
  }

  public void setVehicleLocationService(
      VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationInferenceService = vehicleLocationService;
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

  public void addRecord(NycInferredLocationRecord record) {
    if (_shiftStartTime) {
      if (_records.isEmpty())
        _startTimeOffset = System.currentTimeMillis() - record.getTimestamp();
      record.setTimestamp(record.getTimestamp() + _startTimeOffset);
    }

    // Should we prune a record that comes too close behind a previous record?
    if (!_records.isEmpty()) {
      NycInferredLocationRecord previous = _records.get(_records.size() - 1);
      if ((record.getTimestamp() - previous.getTimestamp()) / 1000 < _minimumRecordInterval)
        return;
    }

    if (_records.isEmpty() && record.locationDataIsMissing()) {
      _log.info("pruning initial record with no gps data");
      return;
    }

    _records.add(record);
    AgencyAndId vid = record.getVehicleId();
    if (_vehicleId != null) {
      if (!_vehicleId.equals(vid))
        throw new IllegalArgumentException(
            "simulation should only include records for same vehicleId: expected="
                + _vehicleId + " actual=" + vid);
    } else {
      _vehicleId = vid;
    }
  }

  public List<NycInferredLocationRecord> getRecords() {
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
    summary.setVehicleId(_vehicleId);
    summary.setNumberOfRecordsProcessed(_recordsProcessed.get());
    summary.setNumberOfRecordsTotal(_records.size());
    summary.setMostRecentRecord(_mostRecentRecord);
    summary.setPaused(_paused);
    return summary;
  }

  public VehicleLocationDetails getDetails(int historyOffset) {
    int index = 0;
    for (Iterator<VehicleLocationDetails> it = _details.descendingIterator(); it.hasNext();) {
      VehicleLocationDetails details = it.next();
      if (index == historyOffset)
        return details;
      index++;
    }
    return null;
  }

  public VehicleLocationDetails getParticleDetails(int particleId) {
    VehicleLocationDetails details = new VehicleLocationDetails();
    details.setId(_id);
    details.setLastObservation(RecordLibrary.getNycInferredLocationRecordAsNycRawLocationRecord(_mostRecentRecord));
    List<Particle> particles = _vehicleLocationInferenceService.getCurrentParticlesForVehicleId(_vehicleId);
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

  public List<NycInferredLocationRecord> getResults() {
    synchronized (_results) {
      return new ArrayList<NycInferredLocationRecord>(_results);
    }
  }

  public void resetAllVehiclesAppearingInRecordData() {
    Set<AgencyAndId> vehicleIds = new HashSet<AgencyAndId>();

    for (NycInferredLocationRecord record : _records)
      vehicleIds.add(record.getVehicleId());

    for (AgencyAndId vehicleId : vehicleIds)
      _vehicleLocationInferenceService.resetVehicleLocation(vehicleId);
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

      NycInferredLocationRecord record = _records.get(nextRecordIndex);

      if (shouldExitAfterSimulatedWait(record))
        return;

      _log.info("sending record: index=" + nextRecordIndex + " "
          + _format.format(record.getTimestampAsDate()));

      if (_bypassInference) {
        _vehicleLocationInferenceService.handleNycInferredLocationRecord(record);
      } else {
    	NycRawLocationRecord vlr = 
    			RecordLibrary.getNycInferredLocationRecordAsNycRawLocationRecord(record);
    	_vehicleLocationInferenceService.handleNycRawLocationRecord(vlr);
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
    if (bean instanceof NycRawLocationRecord) {

      NycRawLocationRecord vlr = (NycRawLocationRecord) bean;

      long t = RecordLibrary.getBestTimestamp(vlr.getTime(),
          vlr.getTimeReceived());

      NycInferredLocationRecord record = new NycInferredLocationRecord();
      record.setDsc(vlr.getDestinationSignCode());
      record.setLat(vlr.getLatitude());
      record.setLon(vlr.getLongitude());
      record.setTimestamp(t);
      record.setVehicleId(vlr.getVehicleId());

      bean = record;
    }
    NycInferredLocationRecord record = (NycInferredLocationRecord) bean;
    addRecord(record);
  }

  private boolean shouldExitAfterSimulatedWait(NycInferredLocationRecord record) {

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
      NycInferredLocationRecord record) {

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

  private boolean processResultRecord(NycInferredLocationRecord record) {

    NycInferredLocationRecord rr = _vehicleLocationInferenceService.getVehicleLocationForVehicle(record.getVehicleId());

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
      rr.setActualPhase(rr.getInferredPhase());
      rr.setActualServiceDate(rr.getInferredServiceDate());
      rr.setActualStatus(rr.getInferredStatus());

      rr.clearInferredValues();
    }

    synchronized (_results) {
      _results.add(rr);
    }

    VehicleLocationDetails details = new VehicleLocationDetails();
    details.setId(_id);
    details.setVehicleId(_vehicleId);
    details.setLastObservation(RecordLibrary.getNycInferredLocationRecordAsNycRawLocationRecord(
        record));

    List<Particle> weightedParticles = _vehicleLocationInferenceService.getCurrentParticlesForVehicleId(_vehicleId);
    if (weightedParticles != null) {
      Collections.sort(weightedParticles, ParticleComparator.INSTANCE);
      details.setParticles(weightedParticles);
    }

    List<Particle> sampledParticles = _vehicleLocationInferenceService.getCurrentSampledParticlesForVehicleId(_vehicleId);
    if (sampledParticles != null) {
      Collections.sort(sampledParticles, ParticleComparator.INSTANCE);
      details.setSampledParticles(sampledParticles);
    }

    List<JourneyPhaseSummary> summaries = _vehicleLocationInferenceService.getCurrentJourneySummariesForVehicleId(_vehicleId);
    details.setSummaries(summaries);

    _details.add(details);
    while (_details.size() > 5)
      _details.removeFirst();

    return true;
  }
}