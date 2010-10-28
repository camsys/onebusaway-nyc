package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.List;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.services.BaseLocationService;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehicleStateLibrary {

  private BaseLocationService _baseLocationService;

  private double _layoverStopDistance = 400;

  @Autowired
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
  }

  /****
   * Vehicle State Methods
   ****/

  public boolean isAtBase(EdgeState edgeState) {
    String baseName = _baseLocationService.getBaseNameForLocation(edgeState.getLocationOnEdge());

    boolean isAtBase = (baseName != null);
    return isAtBase;
  }

  public boolean isAtPotentialLayoverSpot(ScheduledBlockLocation location) {
    return getPotentialLayoverSpot(location) != null;
  }

  public BlockStopTimeEntry getPotentialLayoverSpot(
      ScheduledBlockLocation location) {

    int time = location.getScheduledTime();

    /**
     * We could be at a layover at a stop itself
     */
    BlockStopTimeEntry closestStop = location.getClosestStop();
    StopTimeEntry closestStopTime = closestStop.getStopTime();

    if (closestStopTime.getAccumulatedSlackTime() > 60
        && closestStopTime.getArrivalTime() <= time
        && time <= closestStopTime.getDepartureTime())
      return closestStop;

    BlockStopTimeEntry nextStop = location.getNextStop();

    /**
     * If the next stop is null, it means we're at the end of the block. Do we
     * consider this a layover spot? My sources say no.
     */
    if (nextStop == null)
      return null;

    /**
     * Is the next stop the first stop on the block? Then we're potentially at a
     * layover before the route starts
     */
    if (nextStop.getBlockSequence() == 0)
      return nextStop;

    BlockTripEntry nextTrip = nextStop.getTrip();
    BlockConfigurationEntry blockConfig = nextTrip.getBlockConfiguration();
    List<BlockStopTimeEntry> stopTimes = blockConfig.getStopTimes();

    if (tripChangesBetweenPrevAndNextStop(stopTimes, nextStop, location))
      return nextStop;

    /**
     * Due to some issues in the underlying GTFS with stop locations, buses
     * sometimes make their layover after they have just passed the layover
     * point or right before they get there
     */
    if (nextStop.getBlockSequence() > 1) {
      BlockStopTimeEntry previousStop = blockConfig.getStopTimes().get(
          nextStop.getBlockSequence() - 1);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, previousStop, location))
        return previousStop;
    }

    if (nextStop.getBlockSequence() + 1 < stopTimes.size()) {
      BlockStopTimeEntry nextNextStop = stopTimes.get(nextStop.getBlockSequence() + 1);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, nextNextStop, location))
        return nextNextStop;
    }

    if (nextStop.getBlockSequence() + 2 < stopTimes.size()) {
      BlockStopTimeEntry nextNextStop = stopTimes.get(nextStop.getBlockSequence() + 2);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, nextNextStop, location))
        return nextNextStop;
    }

    return null;
  }

  public double getClosestDistanceToBlockLocation(EdgeState prevEdgeState,
      EdgeState edgeState, BlockState blockState) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    CoordinatePoint blockStart = blockLocation.getLocation();
    CoordinatePoint currentLocation = edgeState.getLocationOnEdge();

    if (prevEdgeState != null) {
      CoordinatePoint prevLocationOnEdge = prevEdgeState.getLocationOnEdge();
      currentLocation = SphericalGeometryLibrary.projectPointToSegmentAppropximate(
          blockStart, prevLocationOnEdge, currentLocation);
    }

    return SphericalGeometryLibrary.distance(currentLocation, blockStart);
  }

  public double getDistanceToBlockLocation(EdgeState edgeState,
      BlockState blockState) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    CoordinatePoint blockStart = blockLocation.getLocation();
    CoordinatePoint currentLocation = edgeState.getLocationOnEdge();

    return SphericalGeometryLibrary.distance(currentLocation, blockStart);
  }

  /****
   * Private Methods
   ****/

  private boolean tripChangesBetweenPrevAndNextStop(
      List<BlockStopTimeEntry> stopTimes, BlockStopTimeEntry nextStop,
      ScheduledBlockLocation location) {

    BlockStopTimeEntry previousStop = stopTimes.get(nextStop.getBlockSequence() - 1);
    BlockTripEntry nextTrip = nextStop.getTrip();
    BlockTripEntry previousTrip = previousStop.getTrip();

    return !previousTrip.equals(nextTrip)
        && isLayoverInRange(previousStop, nextStop, location);
  }

  private boolean isLayoverInRange(BlockStopTimeEntry prevStop,
      BlockStopTimeEntry nextStop, ScheduledBlockLocation location) {

    if (prevStop.getDistaceAlongBlock() <= location.getDistanceAlongBlock()
        && location.getDistanceAlongBlock() <= nextStop.getDistaceAlongBlock())
      return true;

    double d1 = Math.abs(location.getDistanceAlongBlock()
        - prevStop.getDistaceAlongBlock());
    double d2 = Math.abs(location.getDistanceAlongBlock()
        - nextStop.getDistaceAlongBlock());
    return Math.min(d1, d2) < _layoverStopDistance;
  }

}
