package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Arrays;
import java.util.List;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Min;
import org.onebusaway.collections.tuple.T2;
import org.onebusaway.geospatial.model.XYPoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.model.ProjectedShapePointService;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.impl.shapes.PointAndIndex;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlockStateService {

  private DestinationSignCodeService _destinationSignCodeService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ShapePointsLibrary _shapePointsLibrary = new ShapePointsLibrary(100);

  private ProjectedShapePointService _projectedShapePointService;

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
  public void setProjectedShapePointService(ProjectedShapePointService projectedShapePointService) {
    _projectedShapePointService = projectedShapePointService;
  }

  public void setLocalMinimumThreshold(double localMinimumThreshold) {
    _shapePointsLibrary.setLocalMinimumThreshold(localMinimumThreshold);
  }

  public BlockState getBestBlockLocation(long timestamp,
      ProjectedPoint targetPoint, BlockInstance blockInstance,
      double blockDistanceFrom, double blockDistanceTo) {

    BlockConfigurationEntry block = blockInstance.getBlock();

    List<AgencyAndId> shapePointIds = MappingLibrary.map(block.getTrips(),
        "trip.shapeId");

    T2<List<XYPoint>, double[]> tuple = _projectedShapePointService.getProjectedShapePoints(
        shapePointIds, targetPoint.getSrid());

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

    List<PointAndIndex> assignments = _shapePointsLibrary.computePotentialAssignments(
        projectedShapePoints, distances, xyPoint, fromIndex, toIndex);

    if (assignments.size() == 0) {
      return getAsState(blockInstance, blockDistanceFrom);
    } else if (assignments.size() == 1) {
      PointAndIndex pIndex = assignments.get(0);
      return getAsState(blockInstance, pIndex.distanceAlongShape);
    }

    Min<PointAndIndex> best = new Min<PointAndIndex>();

    for (PointAndIndex index : assignments) {

      double distanceAlongBlock = index.distanceAlongShape;

      if (distanceAlongBlock > block.getTotalBlockDistance())
        distanceAlongBlock = block.getTotalBlockDistance();

      ScheduledBlockLocation location = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
          block, distanceAlongBlock);

      if (location != null) {
        int scheduledTime = location.getScheduledTime();
        long scheduleTimestamp = blockInstance.getServiceDate() + scheduledTime
            * 1000;

        double delta = Math.abs(scheduleTimestamp - timestamp);
        best.add(delta, index);
      }
    }

    PointAndIndex index = best.getMinElement();
    return getAsState(blockInstance, index.distanceAlongShape);
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
    return new BlockState(blockInstance, blockLocation, dsc);
  }

  public BlockState getAsState(BlockInstance blockInstance,
      double distanceAlongBlock) {

    BlockConfigurationEntry block = blockInstance.getBlock();

    if (distanceAlongBlock > block.getTotalBlockDistance())
      distanceAlongBlock = block.getTotalBlockDistance();

    ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
        block, distanceAlongBlock);

    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " d=" + distanceAlongBlock);

    BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    return new BlockState(blockInstance, blockLocation, dsc);
  }
}
