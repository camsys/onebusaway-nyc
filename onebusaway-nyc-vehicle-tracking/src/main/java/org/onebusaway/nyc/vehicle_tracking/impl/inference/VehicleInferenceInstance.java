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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycTrackingGraph;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycVehicleStateDistribution;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycVehicleStatePLFilter;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.RunState;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.RunState.RunStateEdgePredictiveResults;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import gov.sandia.cognition.math.matrix.VectorFactory;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.statistics.distribution.DefaultDataDistribution;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.TreeMultiset;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opentrackingtools.VehicleStateInitialParameters;
import org.opentrackingtools.distributions.PathStateDistribution;
import org.opentrackingtools.model.VehicleStateDistribution;
import org.opentrackingtools.util.GeoUtils;
import org.opentrackingtools.util.model.MutableDoubleCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class VehicleInferenceInstance {

  private static Logger _log = LoggerFactory.getLogger(VehicleInferenceInstance.class);

  static {
    ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private NycTrackingGraph _trackingGraph;

  private final VehicleStateInitialParameters _initialParams = new VehicleStateInitialParameters(
      null, VectorFactory.getDefault().createVector2D(600d, 600d), 10,
      VectorFactory.getDefault().createVector1D(6.25e-3), 20,
      VectorFactory.getDefault().createVector2D(6.25e-3, 6.25e-3), 20, 
      VectorFactory.getDefault().createVector2D(1d, 1d),
      VectorFactory.getDefault().createVector2D(1d, 1d), 25, 30, 0l);

  private DestinationSignCodeService _destinationSignCodeService;

  private BaseLocationService _baseLocationService;

  private OperatorAssignmentService _operatorAssignmentService;

  private RunService _runService;

  private long _automaticResetWindow = 20 * 60 * 1000;

  private Observation _previousObservation = null;

  private String _lastValidDestinationSignCode = null;

  private long _lastUpdateTime = 0;

  private long _lastLocationUpdateTime = 0;

  private NycTestInferredLocationRecord _nycTestInferredLocationRecord;

  private DataDistribution<VehicleStateDistribution<Observation>> _particles;

  private NycVehicleStatePLFilter _particleFilter;

  private NycVehicleStateDistribution currentAvgVehicleState;
  
  private boolean _debug = true;
  public void setDebug(String debug) {
    this._debug = Boolean.TRUE == Boolean.parseBoolean(debug);
  }

  public void setModel(ParticleFilterModel<Observation> model) {
    // TODO remove.
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

  public OperatorAssignmentService getOperatorAssignmentService() {
    return _operatorAssignmentService;
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
   * @return true if the resulting inferred location record was successfully
   *         processed, otherwise false
   * @throws TransformException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws NoSuchMethodException
   * @throws ClassNotFoundException
   * @throws IllegalArgumentException
   * @throws SecurityException
   */
  public boolean handleUpdate(NycRawLocationRecord record)
      throws TransformException {

    /**
     * Choose the best timestamp based on device timestamp and received
     * timestamp
     */
    final long timestamp = RecordLibrary.getBestTimestamp(record.getTime(),
        record.getTimeReceived());
    _lastUpdateTime = timestamp;

    /**
     * If this record occurs BEFORE, or at the same time as, the most recent
     * update, we take special action
     */
    if (_particleFilter != null
        && _particleFilter.getLastProcessedTime() != null
        && timestamp <= _particleFilter.getLastProcessedTime()) {
      final long backInTime = _particleFilter.getLastProcessedTime()
          - timestamp;

      /**
       * If the difference is large, we reset the particle filter. Otherwise, we
       * simply ignore the out-of-order record
       */
      if (backInTime > 5 * 60 * 1000) {
        _log.info("resetting filter: time diff");
        _previousObservation = null;
        _particleFilter = null;
      } else {
        _log.info("out-of-order record.  skipping update.");
        return false;
      }
    }

    /**
     * If it's been a while since we've last seen a record, reset the particle
     * filter and forget the previous observation
     */
    Boolean reportedRunIdChange = null;
    Boolean operatorIdChange = null;

    if (_previousObservation != null) {
      /**
       * We use an absolute value here, since we also want to reset if we go
       * back in time as well
       */
      final long delta = Math.abs(timestamp - _previousObservation.getTime());
      final NycRawLocationRecord lastRecord = _previousObservation.getRecord();

      reportedRunIdChange = !StringUtils.equals(lastRecord.getRunId(),
          record.getRunId());
      operatorIdChange = !StringUtils.equals(lastRecord.getOperatorId(),
          record.getOperatorId());

      if (delta > _automaticResetWindow) {
        _log.info("resetting inference for vid=" + record.getVehicleId()
            + " since it's been " + (delta / 1000)
            + " seconds since the previous update");
        _previousObservation = null;
        if (_particleFilter != null) {
          _particleFilter = null;
        }
      }
    }

    /**
     * Recall that a vehicle might send a location update with missing lat-lon
     * if it's sitting at the curb with the engine turned off.
     */
    final boolean latlonMissing = record.locationDataIsMissing();
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

    if (dsc != null
        && !_destinationSignCodeService.isMissingDestinationSignCode(dsc)) {
      lastValidDestinationSignCode = dsc;
    } else if (_previousObservation != null) {
      lastValidDestinationSignCode = _lastValidDestinationSignCode;
    }

    final boolean atBase = _baseLocationService.getBaseNameForLocation(location) != null;
    final boolean atTerminal = false;
    final boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(lastValidDestinationSignCode);
    final boolean hasValidDsc = !_destinationSignCodeService.isMissingDestinationSignCode(lastValidDestinationSignCode)
        && !_destinationSignCodeService.isUnknownDestinationSignCode(lastValidDestinationSignCode);

    Set<AgencyAndId> routeIds = new HashSet<AgencyAndId>();
    if (_previousObservation == null
        || !StringUtils.equals(_lastValidDestinationSignCode,
            lastValidDestinationSignCode)) {
      routeIds = _destinationSignCodeService.getRouteCollectionIdsForDestinationSignCode(lastValidDestinationSignCode);
    } else {
      routeIds = _previousObservation.getDscImpliedRouteCollections();
    }

    RunResults runResults = null;
    if (_previousObservation == null || operatorIdChange == Boolean.TRUE
        || reportedRunIdChange == Boolean.TRUE) {
      runResults = findRunIdMatches(record);
    } else {
      runResults = updateRunIdMatches(record,
          _previousObservation.getRunResults());
    }

    final Observation observation = new Observation(timestamp, record,
        lastValidDestinationSignCode, atBase, atTerminal, outOfService,
        hasValidDsc, _previousObservation, routeIds, runResults);

    final boolean hasPrevPrevObs;
    /*
     * Make sure we don't hold on to all of the data we see.
     */
    if (_previousObservation != null) {
      if (_previousObservation.getPreviousObservation() != null) {
        hasPrevPrevObs = true;
      } else {
        hasPrevPrevObs = false;
      }
      _previousObservation.clearPreviousObservation();
    } else {
      hasPrevPrevObs = false;
    }

    _previousObservation = observation;
    _nycTestInferredLocationRecord = null;
    _lastValidDestinationSignCode = lastValidDestinationSignCode;

    if (!latlonMissing)
      _lastLocationUpdateTime = timestamp;

    if (_particleFilter == null) {
      final Random rng = new Random();
      _particleFilter = new NycVehicleStatePLFilter(observation,
          _trackingGraph, _initialParams, _debug, rng);
      _trackingGraph.setRng(rng);
    }

    currentAvgVehicleState = null;
    /*
     * Wait until we have two observation. That way we can make valid initial
     * states and comparisons.
     */
    if (observation.getPreviousObservation() != null) {
      if (_particles == null || _particles.isEmpty() || !hasPrevPrevObs) {
        _particleFilter.setInitialObservation(observation);
        _particles = _particleFilter.createInitialLearnedObject();
      } else {
        _particleFilter.update(_particles, observation);
      }
    } else {
      /*
       * Just create dummy initial states, then reinitialize later.
       */
      _particles = _particleFilter.createInitialLearnedObject();
    }

    return true;
  }

  /**
   * Pass a previous inference result as if it was ours, from the simulator.
   * 
   * @param record
   * @return true if the resulting inferred location record should be passed on,
   *         otherwise false.
   */
  public synchronized boolean handleBypassUpdate(
      NycTestInferredLocationRecord record) {
    _previousObservation = null;
    _nycTestInferredLocationRecord = record;
    _lastUpdateTime = record.getTimestamp();

    if (!record.locationDataIsMissing())
      _lastLocationUpdateTime = record.getTimestamp();

    return true;
  }

  public synchronized DataDistribution<VehicleStateDistribution<Observation>> getCurrentParticles() {
    return _particles;
  }

  public NycTestInferredLocationRecord getCurrentState() {
    if (_previousObservation != null)
      return getMostRecentParticleAsNycTestInferredLocationRecord();
    else if (_nycTestInferredLocationRecord != null)
      return _nycTestInferredLocationRecord;
    else
      return null;
  }

  public NycVehicleStatePLFilter getFilter() {
    return _particleFilter;
  }

  public synchronized Multiset<Particle> getCurrentParticlesOld() {
    final Multiset<Particle> particles = getParticleSet(this._particles, true);
    return particles;
  }

  private static Multiset<Particle> getParticleSet(
      DataDistribution<? extends VehicleStateDistribution<Observation>> distribution,
      boolean computeTransitions) {
    int i = 0;
    final Multiset<Particle> weightedParticles = TreeMultiset.create(Ordering.natural());
    for (final java.util.Map.Entry<? extends VehicleStateDistribution<Observation>, ? extends Number> entry : distribution.asMap().entrySet()) {
      final NycVehicleStateDistribution nycVehicleState = (NycVehicleStateDistribution) entry.getKey();
      final Particle particle = VehicleInferenceInstance.createParticleForVehicleState(nycVehicleState);
      if (computeTransitions
          && nycVehicleState.getTransitionStateDistribution() != null) {
        particle.setTransitions(getParticleSet(
            nycVehicleState.getTransitionStateDistribution(), false));
      }
      final MutableDoubleCount value = (MutableDoubleCount) entry.getValue();
      try {
        particle.setLogWeight(value.doubleValue() - distribution.getTotal());
      } catch (final BadProbabilityParticleFilterException e) {
        e.printStackTrace();
      }
      particle.setIndex(i);
      if (nycVehicleState.getParentState() != null) {
        particle.setParent(VehicleInferenceInstance.createParticleForVehicleState(nycVehicleState.getParentState()));
      }
      weightedParticles.add(particle, value.getCount());
      i++;
    }
    return weightedParticles;
  }

  public VehicleLocationDetails getDetails() {
    final VehicleLocationDetails details = new VehicleLocationDetails();

    setLastRecordForDetails(details);

    final Multiset<Particle> particles = TreeMultiset.create();
    particles.addAll(getCurrentParticlesOld());
    details.setParticles(particles);

    return details;
  }

  /****
   * Service methods
   */
  public NycQueuedInferredLocationBean getCurrentStateAsNycQueuedInferredLocationBean() {
    final NycTestInferredLocationRecord tilr = getCurrentState();
    final NycQueuedInferredLocationBean record = RecordLibrary.getNycTestInferredLocationRecordAsNycQueuedInferredLocationBean(tilr);

    if (_particles == null)
      return null;
    final NycVehicleStateDistribution state = getAverageVehicleState().clone();
    final RunState runState = state.getRunStateParam().getValue();
    final Observation obs = state.getObservation();
    final BlockStateObservation blockState = runState.getBlockStateObs();
    final NycRawLocationRecord nycRawRecord = obs.getRecord();
    record.setBearing(nycRawRecord.getBearing());

    if (blockState != null) {
      final ScheduledBlockLocation blockLocation = blockState.getBlockState().getBlockLocation();

      // set sched. dev. if we have a match in UTS and are therefore comfortable
      // saying that this schedule deviation is a true match to the schedule.
      if (blockState.isRunFormal()) {
        final int deviation = (int) ((record.getRecordTimestamp() - record.getServiceDate()) / 1000 - blockLocation.getScheduledTime());

        record.setScheduleDeviation(deviation);
      } else {
        record.setScheduleDeviation(null);
      }

      // distance along trip
      final BlockTripEntry activeTrip = blockLocation.getActiveTrip();
      final double distanceAlongTrip = blockLocation.getDistanceAlongBlock()
          - activeTrip.getDistanceAlongBlock();
      Preconditions.checkState(distanceAlongTrip >= 0d);
      record.setDistanceAlongTrip(distanceAlongTrip);
    }

    return record;
  }

  /**
   * To obtain our reported inference state, we choose the mode of the marginal
   * runs + phase, then the mode of the marginal states for that combo.
   * 
   * @return
   */
  private NycVehicleStateDistribution getAverageVehicleState() {

    if (currentAvgVehicleState != null)
      return currentAvgVehicleState;

    final Map<String, DataDistribution<NycVehicleStateDistribution>> idToStates = Maps.newHashMap();
    final DataDistribution<String> idDist = new DefaultDataDistribution<String>();
    for (final java.util.Map.Entry<VehicleStateDistribution<Observation>, ? extends Number> entry : _particles.asMap().entrySet()) {
      final MutableDoubleCount countEntry = (MutableDoubleCount) entry.getValue();
      final NycVehicleStateDistribution vehicleState = (NycVehicleStateDistribution) entry.getKey();
      final String idString;
      final RunState runState = vehicleState.getRunStateParam().getValue();
      if (runState.getVehicleState().getBlockState() != null) {
        idString = runState.getVehicleState()
            .getBlockState().getBlockLocation().getActiveTrip().toString()
            + "_" + runState.getJourneyState().getPhase();
      } else {
        idString = "none";
      }
      DataDistribution<NycVehicleStateDistribution> marginalDist = idToStates.get(idString);
      if (marginalDist == null) {
        marginalDist = new DefaultDataDistribution<NycVehicleStateDistribution>();
        idToStates.put(idString, marginalDist);
      }
      marginalDist.increment(vehicleState, countEntry.count);
      idDist.increment(idString, countEntry.count);
    }

    final String sampledId = idDist.getMaxValueKey();// .sample(this._particleFilter.getRandom());
    final DataDistribution<NycVehicleStateDistribution> marginalRunDist = idToStates.get(sampledId);
    currentAvgVehicleState = marginalRunDist.getMaxValueKey();

    // final NycVehicleStateDistribution naiveBestState =
    // (NycVehicleStateDistribution)_particles.getMaxValueKey();

    return currentAvgVehicleState;
  }

  public NycVehicleManagementStatusBean getCurrentManagementState() {
    final NycVehicleManagementStatusBean record = new NycVehicleManagementStatusBean();

    if (_particles == null)
      return null;
    final NycVehicleStateDistribution state = getAverageVehicleState().clone();
    final RunState runState = state.getRunStateParam().getValue();
    final Observation obs = state.getObservation();
    final BlockStateObservation blockState = runState.getBlockStateObs();
    final NycRawLocationRecord nycRawRecord = obs.getRecord();

    record.setUUID(nycRawRecord.getUuid());
    record.setInferenceIsEnabled(true);
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
      record.setInferenceIsFormal(blockState.isRunFormal());
    }

    return record;
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
      final String[] runInfo = StringUtils.splitByWholeSeparator(
          _nycTestInferredLocationRecord.getReportedRunId(), "-");

      if (runInfo != null && runInfo.length > 0) {
        lastRecord.setRunRouteId(runInfo[0]);
        if (runInfo.length > 1)
          lastRecord.setRunNumber(runInfo[1]);
      }
    }

    details.setLastObservation(lastRecord);
  }

  /**
   * This method rechecks the operator assignment and returns either the old
   * run-results or new ones.
   * 
   * @param observation
   * @param results
   * @return
   */
  private RunResults updateRunIdMatches(NycRawLocationRecord observation,
      RunResults results) {

    final Date obsDate = new Date(observation.getTime());

    String opAssignedRunId = null;
    final String operatorId = observation.getOperatorId();

    final boolean noOperatorIdGiven = StringUtils.isEmpty(operatorId)
        || StringUtils.containsOnly(operatorId, "0");

    final Set<AgencyAndId> routeIds = Sets.newHashSet();
    if (!noOperatorIdGiven) {
      try {
        final OperatorAssignmentItem oai = _operatorAssignmentService.getOperatorAssignmentItemForServiceDate(
            new ServiceDate(obsDate), new AgencyAndId(
                observation.getVehicleId().getAgencyId(), operatorId));

        if (oai != null) {
          if (_runService.isValidRunId(oai.getRunId())) {
            opAssignedRunId = oai.getRunId();

            /*
             * same results; we're done
             */
            if (opAssignedRunId.equals(results.getAssignedRunId()))
              return results;

            /*
             * new assigned run-id; recompute the routes
             */
            routeIds.addAll(_runService.getRoutesForRunId(opAssignedRunId));
            for (final String runId : results.getFuzzyMatches()) {
              routeIds.addAll(_runService.getRoutesForRunId(runId));
            }
            return new RunResults(opAssignedRunId, results.getFuzzyMatches(),
                results.getBestFuzzyDist(), routeIds);
          }
        }
      } catch (final Exception e) {
        _log.warn(e.getMessage());
      }
    }
    /*
     * if we're here, then the op-assignment call probably failed, so just
     * return the old results
     */
    return results;
  }

  private RunResults findRunIdMatches(NycRawLocationRecord observation) {
    final Date obsDate = new Date(observation.getTime());

    String opAssignedRunId = null;
    final String operatorId = observation.getOperatorId();

    final boolean noOperatorIdGiven = StringUtils.isEmpty(operatorId)
        || StringUtils.containsOnly(operatorId, "0");

    final Set<AgencyAndId> routeIds = Sets.newHashSet();
    if (!noOperatorIdGiven) {
      try {
        final OperatorAssignmentItem oai = _operatorAssignmentService.getOperatorAssignmentItemForServiceDate(
            new ServiceDate(obsDate), new AgencyAndId(
                observation.getVehicleId().getAgencyId(), operatorId));

        if (oai != null) {
          if (_runService.isValidRunId(oai.getRunId())) {
            opAssignedRunId = oai.getRunId();
            routeIds.addAll(_runService.getRoutesForRunId(opAssignedRunId));
          }
        }
      } catch (final Exception e) {
        _log.warn(e.getMessage());
      }
    }

    Set<String> fuzzyMatches = Collections.emptySet();
    final String reportedRunId = observation.getRunId();
    Integer bestFuzzyDistance = null;
    if (StringUtils.isEmpty(opAssignedRunId) && !noOperatorIdGiven) {
      _log.info("no assigned run found for operator=" + operatorId);
    }

    if (StringUtils.isNotEmpty(reportedRunId)
        && !StringUtils.containsOnly(reportedRunId, new char[] {'0', '-'})) {

      try {
        final TreeMultimap<Integer, String> fuzzyReportedMatches = _runService.getBestRunIdsForFuzzyId(reportedRunId);
        if (fuzzyReportedMatches.isEmpty()) {
          _log.info("couldn't find a fuzzy match for reported runId="
              + reportedRunId);
        } else {
          bestFuzzyDistance = fuzzyReportedMatches.keySet().first();
          if (bestFuzzyDistance <= 1) {
            fuzzyMatches = fuzzyReportedMatches.get(bestFuzzyDistance);
            for (final String runId : fuzzyMatches) {
              routeIds.addAll(_runService.getRoutesForRunId(runId));
            }
          }
        }
      } catch (final IllegalArgumentException ex) {
        _log.warn(ex.getMessage());
      }
    }

    return new RunResults(opAssignedRunId, fuzzyMatches, bestFuzzyDistance,
        routeIds);
  }

  /**
   * Returns the most recent inference result as an
   * NycTestInferredLocationRecord, i.e. simulator output.
   * 
   * @return
   * @throws TransformException
   * @throws NoninvertibleTransformException
   * @throws MismatchedDimensionException
   */
  private NycTestInferredLocationRecord getMostRecentParticleAsNycTestInferredLocationRecord() {

    if (_particles == null)
      return null;

    final NycVehicleStateDistribution state = getAverageVehicleState().clone();
    final RunState runState = state.getRunStateParam().getValue();
    final Observation obs = state.getObservation();
    final BlockStateObservation blockState = runState.getBlockStateObs();
    final MotionState motionState = runState.getVehicleState().getMotionState();
    final JourneyState journeyState = runState.getJourneyState();
    final CoordinatePoint location = obs.getLocation();
    final NycRawLocationRecord nycRecord = obs.getRecord();

    final NycTestInferredLocationRecord record = new NycTestInferredLocationRecord();
    if (state.getObservation().getAdjustedGps() != null) {
      record.setAdjLat(state.getObservation().getAdjustedGps().getLat());
      record.setAdjLon(state.getObservation().getAdjustedGps().getLon());
    }
    record.setLat(location.getLat());
    record.setLon(location.getLon());
    record.setTimestamp(obs.getTime());
    record.setDsc(nycRecord.getDestinationSignCode());
    record.setOperatorId(nycRecord.getOperatorId());
    record.setReportedRunId(RunTripEntry.createId(nycRecord.getRunRouteId(),
        nycRecord.getRunNumber()));
    record.setFixQuality(obs.getFixQuality());

    final EVehiclePhase phase = journeyState.getPhase();
    if (phase != null)
      record.setInferredPhase(phase.name());
    else
      record.setInferredPhase(EVehiclePhase.UNKNOWN.name());

    final Set<String> statusFields = new HashSet<String>();

    /*
     * This should make sure these are populated. (will show prev. values when
     * record lat/lon are zero)
     */
    // record.setInferredBlockLat(location.getLat());
    // record.setInferredBlockLon(location.getLon());
    Coordinate stateMeanGps;
    try {
      final PathStateDistribution pathStateDistribution = state.getPathStateParam().getParameterPrior();
      stateMeanGps = GeoUtils.convertToLatLon(state.getMeanLocation(),
          obs.getObsProjected().getTransform());
      record.setInferredBlockLat(stateMeanGps.x);
      record.setInferredBlockLon(stateMeanGps.y);
      record.setInferredStateMean(pathStateDistribution.getMotionDistribution().getMean().toString());
      record.setInferredStateCovariance(pathStateDistribution.getCovariance().toString());
      record.setInferredObsCovariance(state.getObservationCovarianceParam().getValue().toString());
      record.setInferredEdges("none");
      if (pathStateDistribution.getPathState().isOnRoad()) {
        record.setInferredEdges(pathStateDistribution.getPathState().getPath().getEdgeIds().toString());
        final MathTransform transform = state.getObservation().getObsProjected().getTransform();
        final Geometry infEdgeGeom = JTS.transform(
            pathStateDistribution.getPathState().getEdge().getInferenceGraphSegment().getGeometry(),
            transform.inverse());
        final Geometry pathEdgeGeom = JTS.transform(
            pathStateDistribution.getPathState().getEdge().getLine().toGeometry(
                JTSFactoryFinder.getGeometryFactory()), transform.inverse());
        record.setInferredEdgeGeom(infEdgeGeom.getCoordinates());
        record.setInferredPathEdgeGeom(pathEdgeGeom.getCoordinates());
      }
    } catch (final NoninvertibleTransformException e) {
      e.printStackTrace();
    } catch (final TransformException e) {
      e.printStackTrace();
    }

    if (blockState != null) {
      record.setInferredRunId(blockState.getBlockState().getRunId());
      record.setInferredIsRunFormal(blockState.isRunFormal());

      // formality match exposed to TDS
      if (blockState.isRunFormal()) {
        statusFields.add("blockInf");
      }

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

      // final CoordinatePoint locationAlongBlock = blockLocation.getLocation();
      // if (locationAlongBlock != null &&
      // (EVehiclePhase.IN_PROGRESS.equals(phase) ||
      // phase.toLabel().toUpperCase().startsWith("LAYOVER_"))) {
      // record.setInferredBlockLat(locationAlongBlock.getLat());
      // record.setInferredBlockLon(locationAlongBlock.getLon());
      // }

      if (EVehiclePhase.IN_PROGRESS.equals(phase)) {
        final int secondsSinceLastMotion = (int) ((obs.getTime() - motionState.getLastInMotionTime()) / 1000);
        if (secondsSinceLastMotion > _configurationService.getConfigurationValueAsInteger(
            "display.stalledTimeout", 900))
          statusFields.add("stalled");
      } else {
        // vehicles on detour should be in_progress with status=deviated
        if (journeyState.getIsDetour()) {
          // remap this journey state/phase to IN_PROGRESS to conform to
          // previous pilot project semantics.
          if (EVehiclePhase.DEADHEAD_DURING.equals(phase)) {
            record.setInferredPhase(EVehiclePhase.IN_PROGRESS.name());
            statusFields.add("deviated");
          }
        }
      }

      record.setInferredDsc(blockState.getBlockState().getDestinationSignCode());
    }

    // Set the status field
    if (!statusFields.isEmpty()) {
      record.setInferredStatus(StringUtils.join(statusFields, "+"));
    } else {
      record.setInferredStatus("default");
    }

    if (StringUtils.isBlank(record.getInferredDsc()))
      record.setInferredDsc("0000");

    return record;
  }

  public static Particle createParticleForVehicleState(
      NycVehicleStateDistribution nycVehicleState) {
    final Particle particle = new Particle(
        nycVehicleState.getObservation().getTime());
    final VehicleState vehicleState = nycVehicleState.getRunStateParam().getValue().getVehicleState();
    vehicleState.setDistribution(nycVehicleState);
    particle.setData(vehicleState);
    try {
      final SensorModelResult result = new SensorModelResult("likelihood");
      result.addLogResultAsAnd("edgeTrans",
          nycVehicleState.getEdgeTransitionLogLikelihood());
      result.addLogResultAsAnd("priorPredObs",
          nycVehicleState.getObsLogLikelihood());
      result.addLogResultAsAnd("pathState",
          nycVehicleState.getPathStateDistLogLikelihood());
      final RunStateEdgePredictiveResults runLikelihoodInfo = nycVehicleState.getRunStateParam().getValue().getLikelihoodInfo();
      result.addLogResultAsAnd("dsc", runLikelihoodInfo.getDscLogLikelihood());
      result.addLogResultAsAnd("movement",
          runLikelihoodInfo.getMovedLogLikelihood());
      result.addLogResultAsAnd("null",
          runLikelihoodInfo.getNullStateLogLikelihood());
      result.addLogResultAsAnd("run", runLikelihoodInfo.getRunLogLikelihood());
      result.addLogResultAsAnd("runTrans",
          runLikelihoodInfo.getRunTransitionLogLikelihood());
      result.addLogResultAsAnd("schedule",
          runLikelihoodInfo.getSchedLogLikelihood());
      particle.setResult(result);
    } catch (final BadProbabilityParticleFilterException e) {
      e.printStackTrace();
    }
    return particle;
  }

  public VehicleStateInitialParameters getInitialParameters() {
    return this._initialParams;
  }

}
