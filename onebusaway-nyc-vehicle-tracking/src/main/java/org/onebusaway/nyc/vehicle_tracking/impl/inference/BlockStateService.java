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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Min;
import org.onebusaway.collections.tuple.T2;
import org.onebusaway.geospatial.model.XYPoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.transit_data_federation.impl.blocks.IndexAdapters;
import org.onebusaway.transit_data_federation.impl.shapes.PointAndIndex;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.impl.time.GenericBinarySearch;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.shapes.ProjectedShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlockStateService {

  /**
   * This will sound weird, but DON'T REMOVE THIS
   */
  @SuppressWarnings("unused")
  private Logger _log = LoggerFactory.getLogger(BlockStateService.class);

  private ObservationCache _observationCache;

  private DestinationSignCodeService _destinationSignCodeService;

  private RunService _runService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ShapePointsLibrary _shapePointsLibrary;

  private ProjectedShapePointService _projectedShapePointService;

  private double _threshold = 50;

  private int _cacheAccessCount = 0;

  private int _cacheMissCount = 0;

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

  public BlockState getBestBlockLocation(Observation observation,
      BlockInstance blockInstance, double blockDistanceFrom,
      double blockDistanceTo) {
    return getBestBlockLocation(observation, blockInstance, null,
        blockDistanceFrom, blockDistanceTo);
  }

  public BlockState getBestBlockLocation(Observation observation,
      BlockInstance blockInstance, RunTripEntry rte, double blockDistanceFrom,
      double blockDistanceTo) {

    blockDistanceFrom = Math.floor(blockDistanceFrom / _threshold) * _threshold;
    blockDistanceTo = Math.ceil(blockDistanceTo / _threshold) * _threshold;

    BlockLocationKey key = new BlockLocationKey(blockInstance,
        blockDistanceFrom, blockDistanceTo);

    Map<BlockLocationKey, BlockState> m = _observationCache
        .getValueForObservation(observation,
            EObservationCacheKey.BLOCK_LOCATION);

    if (m == null) {
      m = new HashMap<BlockStateService.BlockLocationKey, BlockState>();
      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.BLOCK_LOCATION, m);
    }

    BlockState blockState = m.get(key);

    _cacheAccessCount++;

    if (blockState == null) {
      _cacheMissCount++;
      blockState = getUncachedBestBlockLocation(observation, blockInstance,
          rte, blockDistanceFrom, blockDistanceTo);
      m.put(key, blockState);
    }

    // _log.info("cache: " + _cacheMissCount + " / " + _cacheAccessCount);

    return blockState;
  }

  public BlockState getScheduledTimeAsState(BlockInstance blockInstance,
      RunTripEntry rte, int scheduledTime) {
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();

    ScheduledBlockLocation blockLocation = _scheduledBlockLocationService
        .getScheduledBlockLocationFromScheduledTime(blockConfig, scheduledTime);

    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " scheduleTime=" + scheduledTime);

    BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    String dsc = _destinationSignCodeService
        .getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    return new BlockState(blockInstance, blockLocation, rte, dsc);
  }

  public BlockState getScheduledTimeAsState(BlockInstance blockInstance,
      int scheduledTime) {
    RunTripEntry rte = _runService.getRunTripEntryForBlockInstance(
        blockInstance, scheduledTime);
    return getScheduledTimeAsState(blockInstance, rte, scheduledTime);
  }

  public BlockState getAsState(BlockInstance blockInstance,
      double distanceAlongBlock) {

    BlockConfigurationEntry block = blockInstance.getBlock();
    List<BlockStopTimeEntry> stopTimes = block.getStopTimes();
    int n = stopTimes.size();

    int stopTimeIndex = GenericBinarySearch.search(block, n,
        distanceAlongBlock, IndexAdapters.BLOCK_CONFIG_DISTANCE_INSTANCE);

    // TODO FIXME check that this is doing what I think it is. we need to figure
    // in
    // relief runs and their times...
    int estScheduleTime = 0;
    if (stopTimeIndex > n - 1) {
      BlockStopTimeEntry bste = stopTimes.get(n - 1);
      estScheduleTime = bste.getStopTime().getDepartureTime();
    } else {
      BlockStopTimeEntry bste = stopTimes.get(stopTimeIndex);
      estScheduleTime = bste.getStopTime().getDepartureTime();
    }

    RunTripEntry rte = _runService.getRunTripEntryForBlockInstance(
        blockInstance, estScheduleTime);
    return getAsState(blockInstance, rte, distanceAlongBlock);
  }

  /****
   * Private Methods
   ****/

  private BlockState getUncachedBestBlockLocation(Observation observation,
      BlockInstance blockInstance, RunTripEntry rte, double blockDistanceFrom,
      double blockDistanceTo) {

    long timestamp = observation.getTime();
    ProjectedPoint targetPoint = observation.getPoint();

    BlockConfigurationEntry block = blockInstance.getBlock();

    List<AgencyAndId> shapePointIds = MappingLibrary.map(block.getTrips(),
        "trip.shapeId");

    T2<List<XYPoint>, double[]> tuple = _projectedShapePointService
        .getProjectedShapePoints(shapePointIds, targetPoint.getSrid());

    if (tuple == null) {
      throw new IllegalStateException("block had no shape points: "
          + block.getBlock().getId());
    }

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

    List<PointAndIndex> assignments = _shapePointsLibrary
        .computePotentialAssignments(projectedShapePoints, distances, xyPoint,
            fromIndex, toIndex);

    if (assignments.size() == 0) {
      return getAsState(blockInstance, rte, blockDistanceFrom);
    } else if (assignments.size() == 1) {
      PointAndIndex pIndex = assignments.get(0);
      return getAsState(blockInstance, rte, pIndex.distanceAlongShape);
    }

    Min<PointAndIndex> best = new Min<PointAndIndex>();

    for (PointAndIndex index : assignments) {

      double distanceAlongBlock = index.distanceAlongShape;

      if (distanceAlongBlock > block.getTotalBlockDistance())
        distanceAlongBlock = block.getTotalBlockDistance();

      ScheduledBlockLocation location = _scheduledBlockLocationService
          .getScheduledBlockLocationFromDistanceAlongBlock(block,
              distanceAlongBlock);

      if (location != null) {
        int scheduledTime = location.getScheduledTime();
        long scheduleTimestamp = blockInstance.getServiceDate() + scheduledTime
            * 1000;

        double delta = Math.abs(scheduleTimestamp - timestamp);
        best.add(delta, index);
      }
    }

    PointAndIndex index = best.getMinElement();
    return getAsState(blockInstance, rte, index.distanceAlongShape);
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
      if (Double.doubleToLongBits(distanceFrom) != Double
          .doubleToLongBits(other.distanceFrom))
        return false;
      if (Double.doubleToLongBits(distanceTo) != Double
          .doubleToLongBits(other.distanceTo))
        return false;
      return true;
    }

  }

  public BlockState getAsState(BlockInstance blockInstance, RunTripEntry rte,
      double distanceAlongBlock) {

    // FIXME this is a poor hack
    if (rte == null) {
      return getAsState(blockInstance, distanceAlongBlock);
    }

    BlockConfigurationEntry block = blockInstance.getBlock();

    if (distanceAlongBlock > block.getTotalBlockDistance())
      distanceAlongBlock = block.getTotalBlockDistance();

    ScheduledBlockLocation blockLocation = _scheduledBlockLocationService
        .getScheduledBlockLocationFromDistanceAlongBlock(block,
            distanceAlongBlock);

    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " d=" + distanceAlongBlock);

    if (distanceAlongBlock < 0.0
        || distanceAlongBlock > block.getTotalBlockDistance())
      return null;

    BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    String dsc = _destinationSignCodeService
        .getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    return new BlockState(blockInstance, blockLocation, rte, dsc);
  }
}
