package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationSimulationSummary;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulatorTask implements Runnable, EntityHandler {

  private static Logger _log = LoggerFactory.getLogger(SimulatorTask.class);

  private List<NycTestLocationRecord> _records = new ArrayList<NycTestLocationRecord>();

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

  public List<NycTestLocationRecord> getResults() {

    List<Particle> particles = _vehicleLocationService.getMostLikelyParticlesForVehicleId(_vehicleId);

    if (particles == null)
      return Collections.emptyList();

    List<NycTestLocationRecord> records = new ArrayList<NycTestLocationRecord>();

    for (Particle particle : particles) {

      VehicleState state = particle.getData();
      EdgeState edgeState = state.getEdgeState();
      ProjectedPoint p = edgeState.getPointOnEdge();
      JourneyState journeyState = state.getJourneyState();

      NycTestLocationRecord record = new NycTestLocationRecord();
      record.setTimestamp((long) particle.getTimestamp());

      record.setLat(p.getLat());
      record.setLon(p.getLon());

      BlockState blockState = state.getBlockState();

      if (blockState != null) {
        BlockInstance blockInstance = blockState.getBlockInstance();
        BlockConfigurationEntry block = blockInstance.getBlock();
        ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

        record.setDsc(blockState.getDestinationSignCode());
        record.setActualBlockId(AgencyAndIdLibrary.convertToString(block.getBlock().getId()));
        record.setActualServiceDate(blockInstance.getServiceDate());
        record.setActualDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
        record.setActualLat(blockLocation.getLocation().getLat());
        record.setActualLon(blockLocation.getLocation().getLon());
      }

      record.setActualPhase(journeyState.getPhase().toString());
      record.setVehicleId(_vehicleId);

      records.add(record);
    }

    return records;
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

          if (record.getActualBlockId() == null)
            throw new IllegalStateException(
                "expected actualBlockId to be set when running in inference-bypass mode");

          VehicleLocationRecord vlr = new VehicleLocationRecord();
          vlr.setTimeOfRecord(record.getTimestamp());
          vlr.setBlockId(AgencyAndIdLibrary.convertFromString(record.getActualBlockId()));
          vlr.setServiceDate(record.getActualServiceDate());
          vlr.setDistanceAlongBlock(record.getActualDistanceAlongBlock());
          vlr.setCurrentLocationLat(record.getActualLat());
          vlr.setCurrentLocationLon(record.getActualLon());
          vlr.setStatus(record.getActualPhase());
          vlr.setVehicleId(new AgencyAndId(
              _vehicleLocationService.getDefaultVehicleAgencyId(), _vehicleId));
          _vehicleLocationService.handleVehicleLocation(vlr);

        } else {
          _vehicleLocationService.handleVehicleLocation(record.getTimestamp(),
              record.getVehicleId(), record.getLat(), record.getLon(),
              record.getDsc(), true);

          if (shouledExitAfterWaitingForInferenceToComplete())
            return;
        }

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

  private boolean shouledExitAfterWaitingForInferenceToComplete() {

    for (int i = 0; i < 10; i++) {

      List<Particle> particles = _vehicleLocationService.getMostLikelyParticlesForVehicleId(_vehicleId);

      if (particles == null)
        particles = Collections.emptyList();

      int a = particles.size();
      int b = _recordsProcessed.get();

      if (a > b) {
        return false;
      }

      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        return true;
      }
    }

    _log.warn("vehicle location inference never completed");
    return true;
  }

}