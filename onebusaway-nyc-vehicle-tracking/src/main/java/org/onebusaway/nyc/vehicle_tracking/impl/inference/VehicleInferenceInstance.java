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

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BaseLocationService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
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
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.TreeMultiset;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

  private Multiset<Particle> _badParticles;
  
  private ParticleFilter<Observation> _particleFilter;

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
      final long delta = Math.abs(timestamp - _previousObservation.getTime());

      final NycRawLocationRecord lastRecord = _previousObservation.getRecord();

//      final boolean dscChange = !ObjectUtils.equals(
//          lastRecord.getDestinationSignCode(), record.getDestinationSignCode());

      reportedRunIdChange = !StringUtils.equals(lastRecord.getRunId(),
          record.getRunId());

      operatorIdChange = !StringUtils.equals(lastRecord.getOperatorId(),
          record.getOperatorId());

      if (delta > _automaticResetWindow) {
//          || (dscChange && delta > _optionalResetWindow)) {
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

      final NycRawLocationRecord previousRecord = _previousObservation.getRecord();
      record.setLatitude(previousRecord.getLatitude());
      record.setLongitude(previousRecord.getLongitude());
    }

    final CoordinatePoint location = new CoordinatePoint(record.getLatitude(),
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
    boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(lastValidDestinationSignCode);
    boolean hasValidDsc = !_destinationSignCodeService.isMissingDestinationSignCode(lastValidDestinationSignCode)
        && !_destinationSignCodeService.isUnknownDestinationSignCode(lastValidDestinationSignCode);

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

    final Observation observation = new Observation(timestamp, record,
        lastValidDestinationSignCode, atBase, atTerminal, outOfService, hasValidDsc,
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
    } catch (final ZeroProbabilityParticleFilterException ex) {
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
      } catch (final ParticleFilterException ex2) {
        _log.warn("particle filter crashed again: time=" + record.getTime()
            + " timeReceived=" + record.getTimeReceived() + " vehicleId="
            + record.getVehicleId());
        throw new IllegalStateException(ex2);
      }
    } catch (final ParticleFilterException ex) {
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
  public synchronized Multiset<Particle> getPreviousParticles() {
    return HashMultiset.create(_particleFilter.getWeightedParticles());
  }

  public synchronized Multiset<Particle> getCurrentParticles() {
    return HashMultiset.create(_particleFilter.getWeightedParticles());
  }

  public synchronized Multiset<Particle> getCurrentSampledParticles() {
    return HashMultiset.create(_particleFilter.getSampledParticles());
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

    final Multiset<Particle> particles = TreeMultiset.create();
    particles.addAll(getCurrentParticles());
    details.setParticles(particles);

    return details;
  }

  public VehicleLocationDetails getBadParticleDetails() {
    VehicleLocationDetails details = new VehicleLocationDetails();

    setLastRecordForDetails(details);

    if (_badParticles != null)
      details.setParticleFilterFailure(true);

    final Multiset<Particle> particles = HashMultiset.create();
    if (_badParticles != null)
      particles.addAll(_badParticles);
    details.setParticles(particles);

    return details;
  }
  
  /****
   * Service methods
   */

  public synchronized NycQueuedInferredLocationBean getCurrentStateAsNycQueuedInferredLocationBean() {
    final NycTestInferredLocationRecord tilr = getCurrentState();
    final NycQueuedInferredLocationBean record = RecordLibrary.getNycTestInferredLocationRecordAsNycQueuedInferredLocationBean(tilr);

    final Particle particle = _particleFilter.getMostLikelyParticle();
    if (particle == null)
      return null;

    final VehicleState state = particle.getData();
    final BlockStateObservation blockState = state.getBlockStateObservation();
    final Observation obs = state.getObservation();
    final NycRawLocationRecord nycRawRecord = obs.getRecord();

    record.setBearing(nycRawRecord.getBearing());

    if (blockState != null) {
      final ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();

      // set sched. dev. if we have a match in UTS and are therefore comfortable
      // saying that this schedule deviation is a true match to the schedule.
      if (isInferencFormal(blockState)) {
        int deviation = //blockState.getScheduleDeviation(); 
            (int)((record.getRecordTimestamp() - record.getServiceDate()) / 1000 - blockLocation.getScheduledTime());

        record.setScheduleDeviation(deviation);
      } else {
        record.setScheduleDeviation(null);
      }
      
      // distance along trip
      BlockTripEntry activeTrip = blockLocation.getActiveTrip();
      double distanceAlongTrip = blockLocation.getDistanceAlongBlock() - activeTrip.getDistanceAlongBlock();
      record.setDistanceAlongTrip(distanceAlongTrip);
    }

    return record;
  }
  

  public synchronized NycVehicleManagementStatusBean getCurrentManagementState() {
    final NycVehicleManagementStatusBean record = new NycVehicleManagementStatusBean();

    final Particle particle = _particleFilter.getMostLikelyParticle();
    if (particle == null)
      return null;

    VehicleState state = particle.getData();
    Observation obs = state.getObservation();
    NycRawLocationRecord nycRawRecord = obs.getRecord();
    BlockStateObservation blockState = state.getBlockStateObservation();
    
    record.setUUID(nycRawRecord.getUuid());
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
      record.setInferenceIsFormal(isInferencFormal(blockState));
    }

    return record;
  }

  public static boolean isInferencFormal(BlockStateObservation blockState) {
    return blockState.getOpAssigned() == null ? false : blockState.getOpAssigned();
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

    Set<AgencyAndId> routeIds = Sets.newHashSet();
    if (!noOperatorIdGiven) {
      try {
        OperatorAssignmentItem oai = _operatorAssignmentService.getOperatorAssignmentItemForServiceDate(
            new ServiceDate(obsDate), new AgencyAndId(observation.getVehicleId().getAgencyId(), operatorId));

        if (oai != null) {
          if (_runService.isValidRunId(oai.getRunId())) {
            opAssignedRunId = oai.getRunId();
            routeIds.addAll(_runService.getRoutesForRunId(opAssignedRunId));
          }
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
          for (String runId : fuzzyMatches) {
            routeIds.addAll(_runService.getRoutesForRunId(runId));
          }
        }
      } catch (IllegalArgumentException ex) {
        _log.warn(ex.getMessage());
      }
    }

    return new RunResults(opAssignedRunId, fuzzyMatches, bestFuzzyDistance, routeIds);
  }

 
  private NycTestInferredLocationRecord getMostRecentParticleAsNycTestInferredLocationRecord() {
    Particle particle = _particleFilter.getMostLikelyParticle();
    if (particle == null)
      return null;

    final VehicleState state = particle.getData();
    final MotionState motionState = state.getMotionState();
    final JourneyState journeyState = state.getJourneyState();
    final BlockStateObservation blockState = state.getBlockStateObservation();
    final Observation obs = state.getObservation();

    final CoordinatePoint location = obs.getLocation();
    final NycRawLocationRecord nycRecord = obs.getRecord();

    final NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();
    record.setLat(location.getLat());
    record.setLon(location.getLon());
    record.setOperatorId(nycRecord.getOperatorId());
    record.setReportedRunId(
        RunTripEntry.createId(nycRecord.getRunRouteId(),nycRecord.getRunNumber()));

    record.setTimestamp((long) particle.getTimestamp());
    record.setDsc(nycRecord.getDestinationSignCode());

    final EVehiclePhase phase = journeyState.getPhase();
    if (phase != null)
      record.setInferredPhase(phase.name());
    else
      record.setInferredPhase(EVehiclePhase.UNKNOWN.name());

    final Set<String> statusFields = new HashSet<String>();

    if (blockState != null) {
      record.setInferredRunId(blockState.getBlockState().getRunId());

      final BlockInstance blockInstance = blockState.getBlockState().getBlockInstance();
      final BlockConfigurationEntry blockConfig = blockInstance.getBlock();
      final BlockEntry block = blockConfig.getBlock();

      record.setInferredBlockId(AgencyAndIdLibrary.convertToString(block.getId()));
      record.setInferredServiceDate(blockInstance.getServiceDate());

      final ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();
      record.setInferredDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
      record.setInferredScheduleTime(blockLocation.getScheduledTime());

      final BlockTripEntry activeTrip = blockLocation.getActiveTrip();
      if (activeTrip != null) {
        final TripEntry trip = activeTrip.getTrip();
        record.setInferredTripId(AgencyAndIdLibrary.convertToString(trip.getId()));
      }

      final CoordinatePoint locationAlongBlock = blockLocation.getLocation();
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
      final StringBuilder b = new StringBuilder();
      for (final String status : statusFields) {
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
