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

import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationSimulationSummary;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultiset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatorTask implements Runnable, EntityHandler {

  private int _maxParticleHistorySize = Integer.MAX_VALUE;

  private static Logger _log = LoggerFactory.getLogger(SimulatorTask.class);

  private static DateFormat _format = DateFormat.getTimeInstance(DateFormat.SHORT);

  private final List<NycTestInferredLocationRecord> _records = new ArrayList<NycTestInferredLocationRecord>();

  private final List<NycTestInferredLocationRecord> _results = new ArrayList<NycTestInferredLocationRecord>();

  // private final Deque<VehicleLocationDetails> _details = new
  // ArrayDeque<VehicleLocationDetails>();
  private final List<VehicleLocationDetails> _details = new ArrayList<VehicleLocationDetails>();

  private final AtomicInteger _recordsProcessed = new AtomicInteger();

  private NycTestInferredLocationRecord _mostRecentRecord = null;

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

  private final Set<Object> _tags = new HashSet<Object>();

  private AgencyAndId _vehicleId = null;

  private boolean _complete = false;

  private boolean _loop;

  private String _filename = null;

  private int _particleParentSize = 2;

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

  public void addRecord(NycTestInferredLocationRecord record) {
    if (_shiftStartTime) {
      if (_records.isEmpty())
        _startTimeOffset = System.currentTimeMillis() - record.getTimestamp();
      record.setTimestamp(record.getTimestamp() + _startTimeOffset);
    }

    // Should we prune a record that comes too close behind a previous record?
    if (!_records.isEmpty()) {
      final NycTestInferredLocationRecord previous = _records.get(_records.size() - 1);
      if ((record.getTimestamp() - previous.getTimestamp()) / 1000 < _minimumRecordInterval)
        return;
    }

    if (_records.isEmpty() && record.locationDataIsMissing()) {
      _log.info("pruning initial record with no gps data");
      return;
    }

    _records.add(record);
    record.setRecordNumber(_records.size() - 1);

    final AgencyAndId vid = record.getVehicleId();
    if (_vehicleId != null) {
      if (!_vehicleId.equals(vid))
        throw new IllegalArgumentException(
            "simulation should only include records for same vehicleId: expected="
                + _vehicleId + " actual=" + vid);
    } else {
      _vehicleId = vid;
    }
  }

  public List<NycTestInferredLocationRecord> getRecords() {
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
    _details.clear();
    notify();
  }

  public VehicleLocationSimulationSummary getSummary() {
    final VehicleLocationSimulationSummary summary = new VehicleLocationSimulationSummary();
    summary.setFilename(_filename);
    summary.setId(_id);
    summary.setVehicleId(_vehicleId);
    summary.setNumberOfRecordsProcessed(_recordsProcessed.get());
    summary.setNumberOfRecordsTotal(_records.size());
    summary.setMostRecentRecord(_mostRecentRecord);
    summary.setPaused(_paused);
    return summary;
  }

  public VehicleLocationDetails getDetails(int recordNumber) {
    return recordNumber < 0 || recordNumber >= _details.size() ? null
        : _details.get(recordNumber);
    // int index = 0;
    // for (final Iterator<VehicleLocationDetails> it =
    // _details.descendingIterator(); it.hasNext();) {
    // final VehicleLocationDetails details = it.next();
    // if (index == historyOffset)
    // return details;
    // index++;
    // }
    // return null;
  }

  public VehicleLocationDetails getTransitionParticleDetails(
      int parentParticleId, int transParticleNumber, int recordIndex) {
    final VehicleLocationDetails details = new VehicleLocationDetails();
    details.setId(_id);

    final Collection<Multiset.Entry<Particle>> particles;
    if (recordIndex < 0) {
      details.setLastObservation(RecordLibrary.getNycTestInferredLocationRecordAsNycRawLocationRecord(_mostRecentRecord));
      particles = _vehicleLocationInferenceService.getCurrentParticlesForVehicleId(
          _vehicleId).entrySet();
    } else {
      details.setLastObservation(getDetails(recordIndex).getLastObservation());
      particles = getDetails(recordIndex).getParticles();
    }

    if (particles != null) {
      for (final Multiset.Entry<Particle> pEntry : particles) {
        final Particle p = pEntry.getElement();
        if (p.getIndex() == parentParticleId) {
          final Multiset<Particle> history = HashMultiset.create();
          history.add(Iterables.get(p.getTransitions().elementSet(),
              transParticleNumber));
          details.setParticles(history);
          details.setHistory(true);
          break;
        }
      }
    }
    return details;
  }

  public VehicleLocationDetails getParticleDetails(int particleId,
      int recordIndex) {
    final VehicleLocationDetails details = new VehicleLocationDetails();
    details.setId(_id);

    final Collection<Multiset.Entry<Particle>> particles;
    if (recordIndex < 0) {
      details.setLastObservation(RecordLibrary.getNycTestInferredLocationRecordAsNycRawLocationRecord(_mostRecentRecord));
      particles = _vehicleLocationInferenceService.getCurrentParticlesForVehicleId(
          _vehicleId).entrySet();
    } else {
      details.setLastObservation(getDetails(recordIndex).getLastObservation());
      particles = getDetails(recordIndex).getParticles();
    }

    if (particles != null) {
      for (final Multiset.Entry<Particle> pEntry : particles) {
        Particle p = pEntry.getElement();
        if (p.getIndex() == particleId) {
          final Multiset<Particle> history = TreeMultiset.create(Ordering.natural());
          while (p != null
              && history.elementSet().size() <= _particleParentSize) {
            history.add(p, pEntry.getCount());
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

  public List<NycTestInferredLocationRecord> getResults() {
    synchronized (_results) {
      return new ArrayList<NycTestInferredLocationRecord>(_results);
    }
  }

  public void resetAllVehiclesAppearingInRecordData() {
    final Set<AgencyAndId> vehicleIds = new HashSet<AgencyAndId>();

    for (final NycTestInferredLocationRecord record : _records)
      vehicleIds.add(record.getVehicleId());

    for (final AgencyAndId vehicleId : vehicleIds)
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

    } catch (final Throwable ex) {
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

      final NycTestInferredLocationRecord record = _records.get(nextRecordIndex);

      if (shouldExitAfterSimulatedWait(record))
        return;

      _log.info("sending record: index=" + nextRecordIndex + " "
          + _format.format(record.getTimestampAsDate()));

      if (_bypassInference) {
        _vehicleLocationInferenceService.handleBypassUpdateForNycTestInferredLocationRecord(record);
      } else {
        _vehicleLocationInferenceService.handleNycTestInferredLocationRecord(record);
      }

      if (shouldExitAfterWaitingForInferenceToComplete(record))
        ;
      // return;

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

      final NycRawLocationRecord vlr = (NycRawLocationRecord) bean;

      final long t = RecordLibrary.getBestTimestamp(vlr.getTime(),
          vlr.getTimeReceived());

      final NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();
      record.setDsc(vlr.getDestinationSignCode());
      record.setLat(vlr.getLatitude());
      record.setLon(vlr.getLongitude());
      record.setTimestamp(t);
      record.setVehicleId(vlr.getVehicleId());

      bean = record;
    }
    final NycTestInferredLocationRecord record = (NycTestInferredLocationRecord) bean;
    addRecord(record);
  }

  private boolean shouldExitAfterSimulatedWait(
      NycTestInferredLocationRecord record) {

    if (_runInRealtime && _mostRecentRecord != null) {

      final long time = record.getTimestamp()
          - _mostRecentRecord.getTimestamp();

      try {
        Thread.sleep(time);
      } catch (final InterruptedException e) {
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
      } catch (final InterruptedException e) {
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

  private boolean shouldExitAfterWaitingForInferenceToComplete(
      NycTestInferredLocationRecord record) {

    for (int i = 0; i < 20; i++) {

      if (processResultRecord(record))
        return false;

      try {
        Thread.sleep(10 * i);
      } catch (final InterruptedException ex) {
        return true;
      }
    }

    _log.warn("vehicle location inference never completed");
    return true;
  }

  private boolean processResultRecord(NycTestInferredLocationRecord record) {

    final NycTestInferredLocationRecord rr = _vehicleLocationInferenceService.getNycTestInferredLocationRecordForVehicle(record.getVehicleId());

    if (rr == null || rr.getTimestamp() < record.getTimestamp())
      return false;

    rr.setVehicleId(_vehicleId);

    if (_fillActualProperties) {
      rr.setActualRunId(rr.getInferredRunId());
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

    rr.setActualRunId(record.getActualRunId());
    rr.setActualBlockId(record.getActualBlockId());
    rr.setActualTripId(record.getActualTripId());
    rr.setActualBlockLat(record.getActualBlockLat());
    rr.setActualBlockLon(record.getActualBlockLon());
    rr.setActualDistanceAlongBlock(record.getActualDistanceAlongBlock());
    rr.setActualScheduleTime(record.getActualScheduleTime());
    rr.setActualDsc(record.getActualDsc());
    rr.setActualPhase(record.getActualPhase());
    rr.setActualServiceDate(record.getActualServiceDate());
    rr.setActualStatus(record.getActualStatus());

    synchronized (_results) {
      _results.add(rr);
      rr.setRecordNumber(_results.size() - 1);
    }

    final VehicleLocationDetails details = new VehicleLocationDetails();
    details.setId(_id);
    details.setVehicleId(_vehicleId);
    details.setLastObservation(RecordLibrary.getNycTestInferredLocationRecordAsNycRawLocationRecord(record));

    final Multiset<Particle> weightedParticles = TreeMultiset.create(Ordering.natural());
    weightedParticles.addAll(_vehicleLocationInferenceService.getCurrentParticlesForVehicleId(_vehicleId));
    if (!weightedParticles.isEmpty()) {
      details.setParticles(weightedParticles);
      details.setSampledParticles(weightedParticles);
    }

    // Multiset<Particle> sampledParticles =
    // TreeMultiset.create(ParticleComparator.INSTANCE);
    // sampledParticles.addAll(_vehicleLocationInferenceService.getCurrentParticlesForVehicleId(_vehicleId));
    // if (!sampledParticles.isEmpty()) {
    // details.setSampledParticles(sampledParticles);
    // }

    // List<JourneyPhaseSummary> summaries =
    // _vehicleLocationInferenceService.getCurrentJourneySummariesForVehicleId(_vehicleId);
    // details.setSummaries(summaries);

    if (_details.size() < _maxParticleHistorySize)
      _details.add(details);
    // while (_details.size() > _maxParticleHistorySize)
    // _details.removeFirst();

    return true;
  }

  public String getFilename() {
    return _filename;
  }

  public void setFilename(String filename) {
    _filename = filename;
  }

  public int getMaxParticleHistorySize() {
    return _maxParticleHistorySize;
  }

  public void setMaxParticleHistorySize(int maxParticleHistorySize) {
    _maxParticleHistorySize = maxParticleHistorySize;
  }

  public int getParticleParentSize() {
    return _particleParentSize;
  }

  public void setParticleParentSize(int particleParentSize) {
    _particleParentSize = particleParentSize;
  }
}
