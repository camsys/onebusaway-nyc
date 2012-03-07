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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.EdgeLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.HalfNormal;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;

import umontreal.iro.lecuyer.randvar.FoldedNormalGen;
import umontreal.iro.lecuyer.randvar.HalfNormalGen;
import umontreal.iro.lecuyer.randvar.NormalGen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Motion model implementation for vehicle location inference. Determine if the
 * vehicle is in motion, and propagate the particles' positions or states.
 * 
 * @author bdferris, bwillard
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private JourneyStateTransitionModel _journeyMotionModel;

  private BlocksFromObservationService _blocksFromObservationService;
  
  /**
   * Distance, in meters, that a bus has to travel to be considered "in motion"
   */
  static private double _motionThreshold = 20;

  @Autowired
  public void setJourneyMotionModel(
      JourneyStateTransitionModel journeyMotionModel) {
    _journeyMotionModel = journeyMotionModel;
  }
  
  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  static public void setMotionThreshold(double motionThreshold) {
    _motionThreshold = motionThreshold;
  }

  static public double getMotionThreshold() {
    return _motionThreshold;
  }

  EdgeLikelihood edgeLikelihood = new EdgeLikelihood();
  ScheduleLikelihood schedLikelihood = new ScheduleLikelihood();

  private JourneyStateTransitionModel _journeyStateTransitionModel;
  
  @Autowired
  public void setJourneyStateTransitionModel(
      JourneyStateTransitionModel journeyStateTransitionModel) {
    _journeyStateTransitionModel = journeyStateTransitionModel;
  }
  
  @Override
  public void move(Entry<Particle> parent, double timestamp, double timeElapsed,
      Observation obs, Multiset<Particle> results,
      Multimap<VehicleState, VehicleState> cache) {

    final VehicleState parentState = parent.getElement().getData();
    final MotionState motionState = updateMotionState(parentState, obs);

    BlockState parentBlockState = parentState.getBlockState();
    BlockStateObservation distState;
    Set<BlockStateObservation> transitions = Sets.newHashSet();
    for (int i = 0 ; i < parent.getCount(); ++i) {
      transitions.clear();
      final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
      boolean vehicleNotMoved = inMotionSample < SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(motionState, obs);
      if (parentBlockState == null) {
        
        /*
         * These are all the snapped and DSC/run blocks
         */
        transitions.addAll(_blocksFromObservationService.determinePotentialBlockStatesForObservation(
                obs));
        
      } else {
        
        transitions.addAll(_blocksFromObservationService.getSnappedBlockStates(obs));
        
        /*
         * For our two-part proposal transition distribution,
         * start with our observed movement. 
         */
        transitions.add(samplePropagatedDistanceState(vehicleNotMoved, obs, parentBlockState));
        
        /*
         * Now, sample a change in our schedule deviation
         */
        transitions.add(samplePropagatedScheduleState(parentState, parentBlockState, obs));
        
      }
      
      CategoricalDist<VehicleState> transitionProb = new CategoricalDist<VehicleState>();
      for (BlockStateObservation newEdge : transitions) {
        SensorModelResult transProb = new SensorModelResult("transition");
        JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(newEdge, obs, vehicleNotMoved);
        VehicleState newState = new VehicleState(motionState, newEdge, journeyState, null, obs);
        Context context = new Context(parentState, newState, obs);
        
        /*
         * Block/Trip transition 
         */
  //      transProb.addResultAsAnd(BlockTransition.likelihood(null, context));
        if (parentBlockState != null
            && !parentBlockState.getBlockInstance().equals(newEdge.getBlockState().getBlockInstance())) {
          transProb.addResultAsAnd("no block transitions", 0.0);
          continue;
        }
        
        /*
         * Edge movement 
         */
        transProb.addResultAsAnd(edgeLikelihood.likelihood(null, context));
        
        /*
         * Schedule Dev
         */
        transProb.addResultAsAnd(schedLikelihood.likelihood(null, context));
        
        
        transitionProb.put(transProb.getProbability(), newState);
      }
      
      if (transitionProb.canSample()) {
        results.add(new Particle(timestamp, parent.getElement(), 1.0, transitionProb.sample()));
      } else {
        results.add(new Particle(timestamp, parent.getElement(), 1.0, null));
      }
    }
  }
  
private BlockStateObservation samplePropagatedScheduleState(VehicleState parentState, BlockState parentBlockState, Observation obs) {
    int prevSchedTime = (int)(obs.getPreviousObservation().getTime() - parentBlockState.getBlockInstance().getServiceDate())/1000;
    int startSchedTime = Iterables.getFirst(parentBlockState.getBlockInstance().getBlock().getStopTimes(), null).getStopTime()
        .getArrivalTime();
    int endSchedTime = Iterables.getLast(parentBlockState.getBlockInstance().getBlock().getStopTimes(), null).getStopTime()
        .getDepartureTime();
    int newSchedTime = prevSchedTime + (int)NormalGen.nextDouble(ParticleFactoryImpl.getThreadLocalRng().get(), 
        0.0, SensorModelSupportLibrary.schedDevStdDev);
    
    BlockStateObservation schedState;
    if (newSchedTime >= startSchedTime
        && endSchedTime >= newSchedTime) {
      schedState = _blocksFromObservationService.getBlockStateObservationFromTime(obs, 
          parentBlockState.getBlockInstance(), newSchedTime);
    } else {
      schedState = parentState.getBlockStateObservation();
    }
    
    return schedState;
  }

private BlockStateObservation samplePropagatedDistanceState(boolean vehicleNotMoved, Observation obs, BlockState parentBlockState) {
    double distAlongSample;
    if (!vehicleNotMoved) {
      double distAlongPrior = SphericalGeometryLibrary.distance(obs.getLocation(),
          obs.getPreviousObservation().getLocation());
      distAlongSample = FoldedNormalGen.nextDouble(ParticleFactoryImpl.getThreadLocalRng().get(), 
          distAlongPrior, SensorModelSupportLibrary.distanceAlongStdDev);
    } else {
      distAlongSample = parentBlockState.getBlockLocation().getDistanceAlongBlock();
    }
    
    if (distAlongSample > parentBlockState.getBlockInstance().getBlock().getTotalBlockDistance())
      distAlongSample = parentBlockState.getBlockInstance().getBlock().getTotalBlockDistance();
    
    BlockStateObservation distState = _blocksFromObservationService.getBlockStateObservation(obs, 
        parentBlockState.getBlockInstance(), parentBlockState.getBlockLocation().getDistanceAlongBlock()
        + distAlongSample);
    return distState;
  }

//  @Override
//  public void move(Entry<Particle> parent, double timestamp, double timeElapsed,
//      Observation obs, Multiset<Particle> results,
//      Multimap<VehicleState, VehicleState> cache) {
//
//    final VehicleState parentState = parent.getElement().getData();
//    final MotionState motionState = updateMotionState(parentState, obs);
//
//    final Collection<VehicleState> vehicleStates = cache.get(parentState);
//
//    if (vehicleStates.isEmpty()) {
//      _journeyMotionModel.move(parentState, motionState, obs, vehicleStates);
//      for (final VehicleState vs : vehicleStates)
//        results.add(new Particle(timestamp, parent.getElement(), 1.0, vs), parent.getCount());
//    }
//
//  }

  @Override
  public void move(Particle parent, double timestamp, double timeElapsed,
      Observation obs, Collection<Particle> results) {

    final VehicleState parentState = parent.getData();

    final MotionState motionState = updateMotionState(parentState, obs);

    final List<VehicleState> vehicleStates = new ArrayList<VehicleState>();
    _journeyMotionModel.move(parentState, motionState, obs, vehicleStates);

    for (final VehicleState vs : vehicleStates)
      results.add(new Particle(timestamp, parent, 1.0, vs));
  }

  public MotionState updateMotionState(Observation obs) {
    return updateMotionState(null, obs);
  }

  public MotionState updateMotionState(VehicleState parentState, Observation obs) {

    long lastInMotionTime = obs.getTime();
    CoordinatePoint lastInMotionLocation = obs.getLocation();

    if (parentState != null) {

      final MotionState motionState = parentState.getMotionState();

      final double d = SphericalGeometryLibrary.distance(
          motionState.getLastInMotionLocation(), obs.getLocation());

      if (d <= _motionThreshold) {
        lastInMotionTime = motionState.getLastInMotionTime();
        lastInMotionLocation = motionState.getLastInMotionLocation();
      }
    }

    return new MotionState(lastInMotionTime, lastInMotionLocation);
  }
}
