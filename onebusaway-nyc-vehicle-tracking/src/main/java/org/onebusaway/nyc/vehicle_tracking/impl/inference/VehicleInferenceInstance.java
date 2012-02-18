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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BaseLocationService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ZeroProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.ParticleComparator;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.TreeMultimap;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VehicleInferenceInstance {

  private static Logger _log = LoggerFactory.getLogger(VehicleInferenceInstance.class);

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  private ConfigurationService _configurationService;

  private DestinationSignCodeService _destinationSignCodeService;

  private BaseLocationService _baseLocationService;

  private OperatorAssignmentService _operatorAssignmentService;

  private RunService _runService;

  private long _optionalResetWindow = 10 * 60 * 1000;

  private long _automaticResetWindow = 20 * 60 * 1000;

  private boolean _enabled = true;

  private Observation _previousObservation = null;

  private String _lastValidDestinationSignCode = null;

  private long _lastUpdateTime = 0;

  private long _lastLocationUpdateTime = 0;

  private NycTestInferredLocationRecord _nycTestInferredLocationRecord;

  private ParticleFilter<Observation> _particleFilter;

  private List<Particle> _badParticles;

  public void setModel(ParticleFilterModel<Observation> model) {
    _particleFilter = new ParticleFilter<Observation>(model);
  }

  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  @Autowired
  public void setOperatorAssignmentService(
      OperatorAssignmentService operatorAssignmentService) {
    _operatorAssignmentService = operatorAssignmentService;
  }

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
  }

  /**
   * If we haven't received a GPS update in the specified window, the inference
   * engine is reset only if the DSC has changed since the last update
   * 
   * @param optionalResetWindow
   */
  public void setOptionalResetWindow(long optionalResetWindow) {
    _optionalResetWindow = optionalResetWindow;
  }

  /**
   * If we haven't received a GPS update in the specified window, the inference
   * engine is reset no matter what
   * 
   * @param automaticResetWindow
   */
  public void setAutomaticResetWindow(long automaticResetWindow) {
    _automaticResetWindow = automaticResetWindow;
  }

  /**
   * Process a NycRawLocationRecord, usually from a bus or the simulator. 
   * 
   * @param record
   * @return true if the resulting inferred location record should be passed on,
   *         otherwise false
   */
  public synchronized boolean handleUpdate(NycRawLocationRecord record) {

    /**
     * Choose the best timestamp based on device timestamp and received
     * timestamp
     */
    long timestamp = 
        RecordLibrary.getBestTimestamp(record.getTime(), record.getTimeReceived());

    _lastUpdateTime = timestamp;

    /**
     * If this record occurs BEFORE the most recent update, we take special
     * action
     */
    if (timestamp < _particleFilter.getTimeOfLastUpdated()) {
      long backInTime = (long) (_particleFilter.getTimeOfLastUpdated() - timestamp);

      /**
       * If the difference is large, we reset the particle filter. Otherwise, we
       * simply ignore the out-of-order record
       */
      if (backInTime > 5 * 60 * 1000) {
        _previousObservation = null;
        _particleFilter.reset();
        _log.info("resetting filter: time diff");
      } else {
        _log.info("out-of-order record.  skipping update.");
        return false;
      }
    }

    Boolean reportedRunIdChange = null;
    Boolean operatorIdChange = null;
    /**
     * If it's been a while since we've last seen a record, reset the particle
     * filter and forget the previous observation
     */
    if (_previousObservation != null) {
      /**
       * We use an absolute value here, since we also want to reset if we go
       * back in time as well
       */
      long delta = Math.abs(timestamp - _previousObservation.getTime());

      NycRawLocationRecord lastRecord = _previousObservation.getRecord();

      boolean dscChange = !ObjectUtils.equals(
          lastRecord.getDestinationSignCode(), record.getDestinationSignCode());

      reportedRunIdChange = !StringUtils.equals(lastRecord.getRunId(),
          record.getRunId());

      operatorIdChange = !StringUtils.equals(lastRecord.getOperatorId(),
          record.getOperatorId());

      /**
       * If we observe that either operatorId or runId has changed, then we need
       * to resample. Especially for the case in which we obtain more reliable
       * run info (e.g. reported runId + UTS match).
       */
      if (reportedRunIdChange || operatorIdChange) {
        _log.info("resetting inference for vid=" + record.getVehicleId()
            + ": operatorId or reported runId has changed");
        _previousObservation = null;
        _particleFilter.reset();
      } else if (delta > _automaticResetWindow
          || (dscChange && delta > _optionalResetWindow)) {
        _log.info("resetting inference for vid=" + record.getVehicleId()
            + " since it's been " + (delta / 1000)
            + " seconds since the previous update");
        _previousObservation = null;
        _particleFilter.reset();
      }
    }

    /**
     * Recall that a vehicle might send a location update with missing lat-lon
     * if it's sitting at the curb with the engine turned off.
     */
    boolean latlonMissing = record.locationDataIsMissing();
    if (latlonMissing) {
      /**
       * If we don't have a previous record, we can't use the previous lat-lon
       * to replace the missing values
       */
      if (_previousObservation == null) {
        _log.info("missing previous observation and current lat/lon:"
            + record.getVehicleId() + ", skipping update.");
        return false;
      }

      NycRawLocationRecord previousRecord = _previousObservation.getRecord();
      record.setLatitude(previousRecord.getLatitude());
      record.setLongitude(previousRecord.getLongitude());
    }

    CoordinatePoint location = new CoordinatePoint(record.getLatitude(),
        record.getLongitude());

    /**
     * Sometimes, DSCs take the form "  11", where there is whitespace in there.
     * Let's clean it up.
     */
    String dsc = record.getDestinationSignCode();
    if (dsc != null) {
      dsc = dsc.trim();
      record.setDestinationSignCode(dsc);
    }

    String lastValidDestinationSignCode = null;

    if (dsc != null && !_destinationSignCodeService.isMissingDestinationSignCode(dsc)) {
      lastValidDestinationSignCode = dsc;
    } else if (_previousObservation != null) {
      lastValidDestinationSignCode = _lastValidDestinationSignCode;
    }

    boolean atBase = _baseLocationService.getBaseNameForLocation(location) != null;
    boolean atTerminal = _vehicleStateLibrary.isAtPotentialBlockTerminal(record);
    boolean outOfService = lastValidDestinationSignCode == null
        || _destinationSignCodeService.isOutOfServiceDestinationSignCode(lastValidDestinationSignCode)
        || _destinationSignCodeService.isUnknownDestinationSignCode(lastValidDestinationSignCode);

    Set<AgencyAndId> routeIds = new HashSet<AgencyAndId>();
    if (_previousObservation == null
        || !StringUtils.equals(_lastValidDestinationSignCode, lastValidDestinationSignCode)) {
      routeIds = _destinationSignCodeService.getRouteCollectionIdsForDestinationSignCode(lastValidDestinationSignCode);
    } else {
      routeIds = _previousObservation.getDscImpliedRouteCollections();
    }

    RunResults runResults = null;
    if (_previousObservation == null || operatorIdChange == Boolean.TRUE || reportedRunIdChange == Boolean.TRUE) {
      runResults = findRunIdMatches(record);
    } else {
      runResults = _previousObservation.getRunResults();
    }

    Observation observation = new Observation(timestamp, record,
        lastValidDestinationSignCode, atBase, atTerminal, outOfService,
        _previousObservation, routeIds, runResults);

    if (_previousObservation != null)
      _previousObservation.clearPreviousObservation();

    _previousObservation = observation;
    _nycTestInferredLocationRecord = null;
    _lastValidDestinationSignCode = lastValidDestinationSignCode;

    if (!latlonMissing)
      _lastLocationUpdateTime = timestamp;

    try {
      _particleFilter.updateFilter(timestamp, observation);
    } catch (ZeroProbabilityParticleFilterException ex) {
      /**
       * If the particle filter hangs, we try one hard reset to see if that will
       * fix it
       */
      _log.warn("particle filter crashed for record - attempting reset: time="
          + record.getTime() + " timeReceived=" + record.getTimeReceived()
          + " vehicleId=" + record.getVehicleId());

      if (_badParticles == null)
        _badParticles = _particleFilter.getWeightedParticles();

      _particleFilter.reset();
      
      try {
        _particleFilter.updateFilter(timestamp, observation);
      } catch (ParticleFilterException ex2) {
        _log.warn("particle filter crashed again: time=" + record.getTime()
            + " timeReceived=" + record.getTimeReceived() + " vehicleId="
            + record.getVehicleId());
        throw new IllegalStateException(ex2);
      }
    } catch (ParticleFilterException ex) {
      throw new IllegalStateException(ex);
    }

    return _enabled;
  }

  /**
   * Pass a previous inference result as if it was ours, from the simulator.
   * @param record
   * @return true if the resulting inferred location record should be passed on,
   *         otherwise false.
   */
  public synchronized boolean handleBypassUpdate(NycTestInferredLocationRecord record) {
    _previousObservation = null;
    _nycTestInferredLocationRecord = record;
    _lastUpdateTime = record.getTimestamp();

    if (!record.locationDataIsMissing())
      _lastLocationUpdateTime = record.getTimestamp();

    return _enabled;
  }

  /****
   * Simulator/debugging methods
   */
  public synchronized List<Particle> getPreviousParticles() {
    return new ArrayList<Particle>(_particleFilter.getWeightedParticles());
  }

  public synchronized List<Particle> getCurrentParticles() {
    return new ArrayList<Particle>(_particleFilter.getWeightedParticles());
  }

  public synchronized List<Particle> getCurrentSampledParticles() {
    return new ArrayList<Particle>(_particleFilter.getSampledParticles());
  }

  public synchronized List<JourneyPhaseSummary> getJourneySummaries() {
    Particle particle = _particleFilter.getMostLikelyParticle();
    if (particle == null)
      return Collections.emptyList();
    VehicleState state = particle.getData();
    return state.getJourneySummaries();
  }

  public synchronized NycTestInferredLocationRecord getCurrentState() {
    if (_previousObservation != null)
      return getMostRecentParticleAsNycTestInferredLocationRecord();
    else if (_nycTestInferredLocationRecord != null)
      return _nycTestInferredLocationRecord;
    else
      return null;
  }

  public ParticleFilter<Observation> getFilter() {
    return _particleFilter;
  }
  
  public VehicleLocationDetails getDetails() {
    VehicleLocationDetails details = new VehicleLocationDetails();

    setLastRecordForDetails(details);

    if (_badParticles != null)
      details.setParticleFilterFailure(true);

    List<Particle> particles = getCurrentParticles();
    Collections.sort(particles, ParticleComparator.INSTANCE);
    details.setParticles(particles);

    return details;
  }

  public VehicleLocationDetails getBadParticleDetails() {
    VehicleLocationDetails details = new VehicleLocationDetails();

    setLastRecordForDetails(details);

    if (_badParticles != null)
      details.setParticleFilterFailure(true);

    List<Particle> particles = new ArrayList<Particle>();
    if (_badParticles != null)
      particles.addAll(_badParticles);
    details.setParticles(particles);

    return details;
  }
  
  /****
   * Service methods
   */

  public synchronized NycQueuedInferredLocationBean getCurrentStateAsNycQueuedInferredLocationBean() {
    NycTestInferredLocationRecord tilr = getCurrentState();
    NycQueuedInferredLocationBean record = RecordLibrary.getNycTestInferredLocationRecordAsNycQueuedInferredLocationBean(tilr);

    Particle particle = _particleFilter.getMostLikelyParticle();
    if (particle == null)
      return null;

    VehicleState state = particle.getData();
    BlockStateObservation blockState = state.getBlockStateObservation();
    Observation obs = state.getObservation();
    NycRawLocationRecord nycRawRecord = obs.getRecord();

    record.setBearing(nycRawRecord.getBearing());

    if (blockState != null) {
      ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();

      // set sched. dev. if we have a match in UTS and are therefore comfortable
      // saying that this schedule deviation is a true match to the schedule.
      Boolean opAssigned = blockState.getOpAssigned();
      if (opAssigned != null && opAssigned) {
        int deviation = 
            (int)((record.getRecordTimestamp() - record.getServiceDate()) / 1000 - blockLocation.getScheduledTime());

        record.setScheduleDeviation(deviation);
      } else {
        record.setScheduleDeviation(null);
      }

      /*
       * If we're in layover at the end of a trip, report the
       * next trip in the block (if one exists).
       */
      if (EVehiclePhase.LAYOVER_DURING.equals(record.getPhase())) {
        BlockStopTimeEntry nextStop = blockLocation.getNextStop();
        if (nextStop != null
            && nextStop.getTrip() != null
            && nextStop.getTrip().getTrip().getId().toString().equals(record.getTripId())) {
          record.setTripId(nextStop.getTrip().getTrip().getId().toString());
          record.setDistanceAlongBlock(nextStop.getDistanceAlongBlock());
          record.setDistanceAlongTrip(0.0);
        }
      }
      // distance along trip
      BlockTripEntry activeTrip = blockLocation.getActiveTrip();
      double distanceAlongTrip = blockLocation.getDistanceAlongBlock() - activeTrip.getDistanceAlongBlock();
      record.setDistanceAlongTrip(distanceAlongTrip);
    }

    return record;
  }
  

  public synchronized NycVehicleManagementStatusBean getCurrentManagementState() {
    NycVehicleManagementStatusBean record = new NycVehicleManagementStatusBean();

    Particle particle = _particleFilter.getMostLikelyParticle();
    if (particle == null)
      return null;

    VehicleState state = particle.getData();
    Observation obs = state.getObservation();
    NycRawLocationRecord nycRawRecord = obs.getRecord();
    BlockStateObservation blockState = state.getBlockStateObservation();
    
    record.setUUID(nycRawRecord.getUUID());
    record.setInferenceIsEnabled(_enabled);
    record.setLastUpdateTime(_lastUpdateTime);
    record.setLastLocationUpdateTime(_lastLocationUpdateTime);
    record.setAssignedRunId(obs.getOpAssignedRunId());
    record.setMostRecentObservedDestinationSignCode(nycRawRecord.getDestinationSignCode());
    record.setLastObservedLatitude(nycRawRecord.getLatitude());
    record.setLastObservedLongitude(nycRawRecord.getLongitude());
    record.setEmergencyFlag(nycRawRecord.isEmergencyFlag());

    if (blockState != null) {
      record.setLastInferredDestinationSignCode(blockState.getBlockState().getDestinationSignCode());
      record.setInferredRunId(blockState.getBlockState().getRunId());
      record.setInferenceIsFormal(blockState.getOpAssigned() == null ? false : blockState.getOpAssigned());
    }

    return record;
  }

  public synchronized void setVehicleStatus(boolean enabled) {
    _enabled = enabled;
  }

  /****
   * Private Methods
   ****/
  
  private void setLastRecordForDetails(VehicleLocationDetails details) {
    NycRawLocationRecord lastRecord = null;
    if (_previousObservation != null)
      lastRecord = _previousObservation.getRecord();

    if (lastRecord == null && _nycTestInferredLocationRecord != null) {
      lastRecord = new NycRawLocationRecord();
      lastRecord.setDestinationSignCode(_nycTestInferredLocationRecord.getDsc());
      lastRecord.setTime(_nycTestInferredLocationRecord.getTimestamp());
      lastRecord.setTimeReceived(_nycTestInferredLocationRecord.getTimestamp());
      lastRecord.setLatitude(_nycTestInferredLocationRecord.getLat());
      lastRecord.setLongitude(_nycTestInferredLocationRecord.getLon());
      lastRecord.setOperatorId(_nycTestInferredLocationRecord.getOperatorId());
      String[] runInfo = 
          StringUtils.splitByWholeSeparator(_nycTestInferredLocationRecord.getReportedRunId(), "-");

      if (runInfo != null && runInfo.length > 0) {
        lastRecord.setRunRouteId(runInfo[0]);
        if (runInfo.length > 1)
          lastRecord.setRunNumber(runInfo[1]);
      }
    }

    details.setLastObservation(lastRecord);
  }
  
  private RunResults findRunIdMatches(NycRawLocationRecord observation) {
    Date obsDate = new Date(observation.getTime());

    String opAssignedRunId = null;
    String operatorId = observation.getOperatorId();

    boolean noOperatorIdGiven = 
        StringUtils.isEmpty(operatorId) || StringUtils.containsOnly(operatorId, "0");

    if (!noOperatorIdGiven) {
      try {
        OperatorAssignmentItem oai = _operatorAssignmentService.getOperatorAssignmentItemForServiceDate(
            new ServiceDate(obsDate), operatorId);

        if (oai != null) {
          if (_runService.isValidRunNumber(oai.getRunId()))
            opAssignedRunId = oai.getRunId();
        }
      } catch (Exception e) {
        _log.warn(e.getMessage());
      }
    }

    Set<String> fuzzyMatches = Collections.emptySet();
    String reportedRunId = observation.getRunId();
    Integer bestFuzzyDistance = null;
    if (StringUtils.isEmpty(opAssignedRunId) && !noOperatorIdGiven) {
      _log.info("no assigned run found for operator=" + operatorId);
    }

    if (StringUtils.isNotEmpty(reportedRunId)
        && !StringUtils.containsOnly(reportedRunId, new char[] {'0', '-'})) {

      try {
        TreeMultimap<Integer, String> fuzzyReportedMatches = _runService.getBestRunIdsForFuzzyId(reportedRunId);
        if (fuzzyReportedMatches.isEmpty()) {
          _log.info("couldn't find a fuzzy match for reported runId="
              + reportedRunId);
        } else {
          bestFuzzyDistance = fuzzyReportedMatches.keySet().first();
          fuzzyMatches = fuzzyReportedMatches.get(bestFuzzyDistance);
        }
      } catch (IllegalArgumentException ex) {
        _log.warn(ex.getMessage());
      }
    }

    return new RunResults(opAssignedRunId, fuzzyMatches, bestFuzzyDistance);
  }

 
  private NycTestInferredLocationRecord getMostRecentParticleAsNycTestInferredLocationRecord() {
    Particle particle = _particleFilter.getMostLikelyParticle();
    if (particle == null)
      return null;

    VehicleState state = particle.getData();
    MotionState motionState = state.getMotionState();
    JourneyState journeyState = state.getJourneyState();
    BlockStateObservation blockState = state.getBlockStateObservation();
    Observation obs = state.getObservation();

    CoordinatePoint location = obs.getLocation();
    NycRawLocationRecord nycRecord = obs.getRecord();

    NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();
    record.setLat(location.getLat());
    record.setLon(location.getLon());
    record.setOperatorId(nycRecord.getOperatorId());
    record.setReportedRunId(
        RunTripEntry.createId(nycRecord.getRunRouteId(),nycRecord.getRunNumber()));

    record.setTimestamp((long) particle.getTimestamp());
    record.setDsc(nycRecord.getDestinationSignCode());

    EVehiclePhase phase = journeyState.getPhase();
    if (phase != null)
      record.setInferredPhase(phase.name());
    else
      record.setInferredPhase(EVehiclePhase.UNKNOWN.name());

    Set<String> statusFields = new HashSet<String>();

    if (blockState != null) {
      record.setInferredRunId(blockState.getBlockState().getRunId());

      BlockInstance blockInstance = blockState.getBlockState().getBlockInstance();
      BlockConfigurationEntry blockConfig = blockInstance.getBlock();
      BlockEntry block = blockConfig.getBlock();

      record.setInferredBlockId(AgencyAndIdLibrary.convertToString(block.getId()));
      record.setInferredServiceDate(blockInstance.getServiceDate());

      ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();
      record.setInferredDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
      record.setInferredScheduleTime(blockLocation.getScheduledTime());

      BlockTripEntry activeTrip = blockLocation.getActiveTrip();
      if (activeTrip != null) {
        TripEntry trip = activeTrip.getTrip();
        record.setInferredTripId(AgencyAndIdLibrary.convertToString(trip.getId()));
      }

      CoordinatePoint locationAlongBlock = blockLocation.getLocation();
      if (locationAlongBlock != null) {
        record.setInferredBlockLat(locationAlongBlock.getLat());
        record.setInferredBlockLon(locationAlongBlock.getLon());
      }

      if (EVehiclePhase.IN_PROGRESS.equals(phase)) {
        double d = SphericalGeometryLibrary.distance(location, blockLocation.getLocation());
        if (d > (double)_configurationService.getConfigurationValueAsInteger("display.offRouteDistance", 200))
          statusFields.add("deviated");

        int secondsSinceLastMotion = (int) ((particle.getTimestamp() - motionState.getLastInMotionTime()) / 1000);
        if (secondsSinceLastMotion > _configurationService.getConfigurationValueAsInteger("display.stalledTimeout", 900))
          statusFields.add("stalled");
      }

      record.setInferredDsc(blockState.getBlockState().getDestinationSignCode());
    }

    // Set the status field
    if (!statusFields.isEmpty()) {
      StringBuilder b = new StringBuilder();
      for (String status : statusFields) {
        if (b.length() > 0)
          b.append(',');
        b.append(status);
      }
      record.setInferredStatus(b.toString());
    } else
      record.setInferredStatus("default");

    if (StringUtils.isBlank(record.getInferredDsc()))
      record.setInferredDsc("0000");

    return record;
  }
}
