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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Max;
import org.onebusaway.collections.tuple.T2;
import org.onebusaway.geospatial.model.XYPoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.transit_data_federation.impl.shapes.PointAndIndex;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.shapes.ProjectedShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;


@Component
public class BlockStateService {

  /**
   * This will sound weird, but DON'T REMOVE THIS
   */
  private Logger _log = LoggerFactory.getLogger(BlockStateService.class);

  private ObservationCache _observationCache;

  private DestinationSignCodeService _destinationSignCodeService;

  private RunService _runService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ShapePointsLibrary _shapePointsLibrary;

  private ProjectedShapePointService _projectedShapePointService;

  private double _threshold = 50;

  private ScheduleDeviationLibrary _scheduleDeviationLibrary;

  @Autowired
  public void setScheduleDeviationLibrary(
      ScheduleDeviationLibrary scheduleDeviationLibrary) {
    _scheduleDeviationLibrary = scheduleDeviationLibrary;
  }

  @Autowired
  public void setRunService(RunService runService) {
    _runService = runService;
  }

  @Autowired
  public void setObservationCache(ObservationCache observationCache) {
    _observationCache = observationCache;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setScheduledBlockService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  @Autowired
  public void setProjectedShapePointService(
      ProjectedShapePointService projectedShapePointService) {
    _projectedShapePointService = projectedShapePointService;
  }

  @Autowired
  public void setShapePointsLibrary(ShapePointsLibrary shapePointsLibrary) {
    _shapePointsLibrary = shapePointsLibrary;
  }

  public void setLocalMinimumThreshold(double localMinimumThreshold) {
    _shapePointsLibrary.setLocalMinimumThreshold(localMinimumThreshold);
  }

  public BestBlockStates getBestBlockLocations(Observation observation,
      BlockInstance blockInstance, double blockDistanceFrom,
      double blockDistanceTo) throws MissingShapePointsException {

    blockDistanceFrom = Math.floor(blockDistanceFrom / _threshold) * _threshold;
    blockDistanceTo = Math.ceil(blockDistanceTo / _threshold) * _threshold;

    BlockLocationKey key = new BlockLocationKey(blockInstance,
        blockDistanceFrom, blockDistanceTo);

    Map<BlockLocationKey, BestBlockStates> m = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.BLOCK_LOCATION);

    if (m == null) {
      m = new ConcurrentHashMap<BlockStateService.BlockLocationKey, BestBlockStates>();
      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.BLOCK_LOCATION, m);
    }

    BestBlockStates blockStates = m.get(key);

    if (blockStates == null) {
      blockStates = getUncachedBestBlockLocations(observation, blockInstance,
          blockDistanceFrom, blockDistanceTo);
      m.put(key, blockStates);
      
    }

    return blockStates;
  }

  
  public BestBlockStates getBestBlockStateForRoute(Observation observation) {
      BestBlockStates bestRouteStates = _observationCache.getValueForObservation(observation, 
          EObservationCacheKey.ROUTE_LOCATION);
      return bestRouteStates;
  } 

  public BlockState getScheduledTimeAsState(BlockInstance blockInstance,
      int scheduledTime) {
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();

    ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        blockConfig, scheduledTime);

    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " scheduleTime=" + scheduledTime);

    BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(activeTrip.getTrip(), scheduledTime);
    
    return new BlockState(blockInstance, blockLocation, rte, dsc);
  }

  /****
   * Private Methods
   ****/

  public static class BestBlockStates {

//    final private BlockState bestTime;
//    final private BlockState bestLocation;
//    final private Double locDev;
//    final private Long timeDev;

    final private ImmutableSet<BlockState> _bestStates;
//    public BestBlockStates(BestBlockObservationStates bestObsState) {
//      this.bestTime = bestObsState.getBestTime().getBlockState();
//      this.bestLocation = bestObsState.getBestLocation().getBlockState();
//    }

    public BestBlockStates(ImmutableSet<BlockState> bestStates) {
      _bestStates = bestStates;
    }
    
//    public BestBlockStates(BlockState bestTime, BlockState bestLocation, 
//        Long timeDev, Double locDev) {
//      if (bestTime == null || bestLocation == null)
//        throw new IllegalArgumentException("best block states cannot be null");
//      this.bestLocation = bestLocation;
//      this.bestTime = bestTime;
//      this.timeDev = timeDev;
//      this.locDev = locDev;
//    }

//    public BlockState getBestTime() {
//      return this.bestTime;
//    }
//
//    public BlockState getBestLocation() {
//      return this.bestLocation;
//    }

    public ImmutableSet<BlockState> getAllStates() {
//      return bestTime.equals(bestLocation) ? Arrays.asList(bestTime)
//          : Arrays.asList(bestTime, bestLocation);
      return _bestStates;
    }
    
//    public double getLocDev() {
//      return this.locDev;
//    }
//    
//    public double getTimeDev() {
//      return this.timeDev;
//    }
  }

  private BestBlockStates getUncachedBestBlockLocations(
      Observation observation, BlockInstance blockInstance,
      double blockDistanceFrom, double blockDistanceTo)
      throws MissingShapePointsException {

    ProjectedPoint targetPoint = observation.getPoint();

    BlockConfigurationEntry block = blockInstance.getBlock();

    List<AgencyAndId> shapePointIds = MappingLibrary.map(block.getTrips(),
        "trip.shapeId");

    if (shapePointIds.removeAll(Collections.singleton(null))) {
      if (shapePointIds.isEmpty())
        throw new MissingShapePointsException("block has no shape points: "
            + block.getBlock().getId());
      else
        _log.warn("block missing some shape points: "
            + block.getBlock().getId());
    }

    T2<List<XYPoint>, double[]> tuple = _projectedShapePointService.getProjectedShapePoints(
        shapePointIds, targetPoint.getSrid());

    List<XYPoint> projectedShapePoints = tuple.getFirst();
    double[] distances = tuple.getSecond();

    int fromIndex = 0;
    int toIndex = distances.length;

    if (blockDistanceFrom > 0) {
      fromIndex = Arrays.binarySearch(distances, blockDistanceFrom);
      if (fromIndex < 0) {
        fromIndex = -(fromIndex + 1);
        // Include the previous point if we didn't get an exact match
        if (fromIndex > 0)
          fromIndex--;
      }
    }

    if (blockDistanceTo < distances[distances.length - 1]) {
      toIndex = Arrays.binarySearch(distances, blockDistanceTo);
      if (toIndex < 0) {
        toIndex = -(toIndex + 1);
        // Include the previous point if we didn't get an exact match
        if (toIndex < distances.length)
          toIndex++;
      }
    }

    XYPoint xyPoint = new XYPoint(targetPoint.getX(), targetPoint.getY());

    List<PointAndIndex> assignments = _shapePointsLibrary.computePotentialAssignments(
        projectedShapePoints, distances, xyPoint, fromIndex, toIndex);

    Set<BlockState> results = new HashSet<BlockState>();
    if (assignments.size() == 0) {
      BlockState bs = getAsState(blockInstance, blockDistanceFrom);
      return new BestBlockStates(ImmutableSet.of(bs));
    } 


    Max<BlockState> best = new Max<BlockState>(); 
    for (PointAndIndex index : assignments) {

      double distanceAlongBlock = index.distanceAlongShape;

      if (distanceAlongBlock > block.getTotalBlockDistance())
        distanceAlongBlock = block.getTotalBlockDistance();

      BlockState blockState = getAsState(blockInstance, distanceAlongBlock);
      int obsSchedDev = _scheduleDeviationLibrary.computeScheduleDeviation(blockState.getBlockInstance(),
          blockState.getBlockLocation(), observation);
      
      double pInP1 = 1.0 - FoldedNormalDist.cdf(10.0, 50.0, index.distanceFromTarget);
      double pInP2 = 1.0 - FoldedNormalDist.cdf(5.0, 40.0, obsSchedDev/60.0);
      best.add(pInP1*pInP2, blockState);
    }
    
    results.add(best.getMaxElement());

    return new BestBlockStates(new ImmutableSet.Builder<BlockState>().addAll(results).build());
  }

  private static class BlockLocationKey {
    private final BlockInstance blockInstance;
    private final double distanceFrom;
    private final double distanceTo;

    public BlockLocationKey(BlockInstance blockInstance, double distanceFrom,
        double distanceTo) {
      this.blockInstance = blockInstance;
      this.distanceFrom = distanceFrom;
      this.distanceTo = distanceTo;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((blockInstance == null) ? 0 : blockInstance.hashCode());
      long temp;
      temp = Double.doubleToLongBits(distanceFrom);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(distanceTo);
      result = prime * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      BlockLocationKey other = (BlockLocationKey) obj;
      if (blockInstance == null) {
        if (other.blockInstance != null)
          return false;
      } else if (!blockInstance.equals(other.blockInstance))
        return false;
      if (Double.doubleToLongBits(distanceFrom) != Double.doubleToLongBits(other.distanceFrom))
        return false;
      if (Double.doubleToLongBits(distanceTo) != Double.doubleToLongBits(other.distanceTo))
        return false;
      return true;
    }

  }

  public BlockState getAsState(BlockInstance blockInstance,
      double distanceAlongBlock) {

    BlockConfigurationEntry block = blockInstance.getBlock();

    if (distanceAlongBlock < 0.0)
      return null;

    if (distanceAlongBlock > block.getTotalBlockDistance())
      distanceAlongBlock = block.getTotalBlockDistance();

    ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
        block, distanceAlongBlock);

    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " d=" + distanceAlongBlock);

    BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(
        activeTrip.getTrip(), blockLocation.getScheduledTime());
    return new BlockState(blockInstance, blockLocation, rte, dsc);
  }
}
