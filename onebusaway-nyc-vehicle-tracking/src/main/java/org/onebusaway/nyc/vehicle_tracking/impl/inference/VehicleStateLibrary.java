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

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BaseLocationService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VehicleStateLibrary {

  private BaseLocationService _baseLocationService;

  private final static double _layoverStopDistance = 400;

  /**
   * If we're more than X meters off our block, then we really don't think we're
   * serving the block any more
   */
  private final double _offBlockDistance = 1000;

  private final static double _terminalSearchRadius = 150;

  private TransitGraphDao _transitGraphDao;

  private BlockIndexService _blockIndexService;

  @Autowired
  public void setBlockIndexService(BlockIndexService blockIndexService) {
    _blockIndexService = blockIndexService;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
  }

  /****
   * Vehicle State Methods
   ****/

  public boolean isAtBase(CoordinatePoint location) {
    final String baseName = _baseLocationService.getBaseNameForLocation(location);

    final boolean isAtBase = (baseName != null);
    return isAtBase;
  }

  public boolean isAtPotentialLayoverSpot(VehicleState state, Observation obs) {
//    if (_baseLocationService.getTerminalNameForLocation(obs.getLocation()) != null)
//      return true;
//
//    /**
//     * For now, we assume that if we're at the base, we're NOT in a layover
//     */
//    if (_baseLocationService.getBaseNameForLocation(obs.getLocation()) != null)
//      return false;

    return isAtPotentialLayoverSpot(state.getBlockState(), obs);
  }

  public static boolean isAtPotentialLayoverSpot(BlockState blockState,
      Observation obs) {

    /**
     * If there is no block assigned to this vehicle state, then we allow a
     * layover spot the terminals of all blocks.
     */
    if (blockState == null) {
      return obs.isAtTerminal();
    }

    /**
     * Otherwise, if there is a block assigned, then we need to be more specific
     * and check only terminals for trips on this block. if
     * (isAtPotentialTerminal(obs.getRecord(), blockState.getBlockInstance()))
     * return true;
     */

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    if (blockLocation == null)
      return false;

    final BlockStopTimeEntry layoverSpot = getPotentialLayoverSpot(blockLocation);
    if (layoverSpot == null)
      return false;

    final double dist = SphericalGeometryLibrary.distance(obs.getLocation(),
        layoverSpot.getStopTime().getStop().getStopLocation());
    return dist <= _layoverStopDistance;
  }

  /**
   * This method determines if an observation record is within a certain radius,
   * _terminalSearchRadius argument, of a stop at the start or end of a given
   * block.
   * 
   * @param record
   * @param blockInstance
   * @return whether the observation is within the search radius
   */
  public static boolean isAtPotentialTerminal(NycRawLocationRecord record,
      BlockInstance blockInstance) {

    final CoordinatePoint loc = new CoordinatePoint(record.getLatitude(),
        record.getLongitude());

    final StopEntry firstStop = blockInstance.getBlock().getStopTimes().get(0).getStopTime().getStop();

    final double firstStopDist = SphericalGeometryLibrary.distance(loc,
        firstStop.getStopLocation());

    final int lastStopIdx = blockInstance.getBlock().getStopTimes().size() - 1;

    final StopEntry lastStop = blockInstance.getBlock().getStopTimes().get(
        lastStopIdx).getStopTime().getStop();
    final double lastStopDist = SphericalGeometryLibrary.distance(loc,
        lastStop.getStopLocation());

    if (firstStopDist <= _terminalSearchRadius
        || lastStopDist <= _terminalSearchRadius) {
      return true;
    }

    return false;
  }

  /**
   * This method determines if an observation record is within a certain radius,
   * _terminalSearchRadius argument, of a stop at the start or end of a block. <br>
   * Note: all trips' stops within the radius are checked.
   * 
   * @param record
   * @return whether the observation is within the search radius
   */
  public boolean isAtPotentialBlockTerminal(NycRawLocationRecord record) {

    final CoordinatePoint loc = new CoordinatePoint(record.getLatitude(),
        record.getLongitude());
    final CoordinateBounds bounds = SphericalGeometryLibrary.bounds(loc,
        _terminalSearchRadius);

    final List<BlockConfigurationEntry> blocks = new ArrayList<BlockConfigurationEntry>();
    final List<StopEntry> stops = _transitGraphDao.getStopsByLocation(bounds);
    for (final StopEntry stop : stops) {
      final List<BlockStopTimeIndex> stopTimeIndices = _blockIndexService.getStopTimeIndicesForStop(stop);
      for (final BlockStopTimeIndex stopTimeIndex : stopTimeIndices) {
        blocks.addAll(stopTimeIndex.getBlockConfigs());
      }
    }

    for (final BlockConfigurationEntry block : blocks) {

      final StopEntry firstStop = block.getStopTimes().get(0).getStopTime().getStop();

      final double firstStopDist = SphericalGeometryLibrary.distance(loc,
          firstStop.getStopLocation());

      final int lastStopIdx = block.getStopTimes().size() - 1;

      final StopEntry lastStop = block.getStopTimes().get(lastStopIdx).getStopTime().getStop();
      final double lastStopDist = SphericalGeometryLibrary.distance(loc,
          lastStop.getStopLocation());

      if (firstStopDist <= _terminalSearchRadius
          || lastStopDist <= _terminalSearchRadius) {
        return true;
      }

    }

    return false;
  }

  /**
   * This method determines if an observation record is within a certain radius,
   * _terminalSearchRadius argument, of a stop at the start or end of a trip. <br>
   * Note: all trips' stops within the radius are checked.
   * 
   * @param record
   * @return whether the observation is within the search radius
   */
  public boolean isAtPotentialTripTerminal(NycRawLocationRecord record) {

    final CoordinatePoint loc = new CoordinatePoint(record.getLatitude(),
        record.getLongitude());
    final CoordinateBounds bounds = SphericalGeometryLibrary.bounds(loc,
        _terminalSearchRadius);

    final List<StopEntry> stops = _transitGraphDao.getStopsByLocation(bounds);
    for (final StopEntry stop : stops) {
      final List<BlockStopTimeIndex> stopTimeIndices = _blockIndexService.getStopTimeIndicesForStop(stop);
      for (final BlockStopTimeIndex stopTimeIndex : stopTimeIndices) {
        for (final BlockTripEntry bte : stopTimeIndex.getTrips()) {
          /*
           * is this the first stop on this trip?
           */
          final List<StopTimeEntry> stopsOnTrip = bte.getTrip().getStopTimes();
          if (stop.equals(stopsOnTrip.get(0).getStop())) {
            return true;
          }

          final int numOfStops = stopsOnTrip.size() - 1;
          if (stop.equals(stopsOnTrip.get(numOfStops).getStop())) {
            return true;
          }

        }
      }
    }

    return false;
  }

  static public BlockStopTimeEntry getPotentialLayoverSpot(
      ScheduledBlockLocation location) {

    final int time = location.getScheduledTime();

    /**
     * We could be at a layover at a stop itself
     */
    final BlockStopTimeEntry closestStop = location.getClosestStop();
    final StopTimeEntry closestStopTime = closestStop.getStopTime();

    if (closestStopTime.getAccumulatedSlackTime() > 60
        && closestStopTime.getArrivalTime() <= time
        && time <= closestStopTime.getDepartureTime())
      return closestStop;

    final BlockStopTimeEntry nextStop = location.getNextStop();

    /**
     * If the next stop is null, it means we're at the end of the block. Do we
     * consider this a layover spot? My sources say no. But now they say yes?
     */
    if (nextStop == null)
      return closestStop;

    /**
     * Is the next stop the first stop on the block? Then we're potentially at a
     * layover before the route starts
     */
    if (nextStop.getBlockSequence() == 0)
      return nextStop;

    final BlockTripEntry nextTrip = nextStop.getTrip();
    final BlockConfigurationEntry blockConfig = nextTrip.getBlockConfiguration();
    final List<BlockStopTimeEntry> stopTimes = blockConfig.getStopTimes();

    if (tripChangesBetweenPrevAndNextStop(stopTimes, nextStop, location))
      return nextStop;

    /**
     * Due to some issues in the underlying GTFS with stop locations, buses
     * sometimes make their layover after they have just passed the layover
     * point or right before they get there
     */
    if (nextStop.getBlockSequence() > 1) {
      final BlockStopTimeEntry previousStop = blockConfig.getStopTimes().get(
          nextStop.getBlockSequence() - 1);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, previousStop, location))
        return previousStop;
    }

    if (nextStop.getBlockSequence() + 1 < stopTimes.size()) {
      final BlockStopTimeEntry nextNextStop = stopTimes.get(nextStop.getBlockSequence() + 1);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, nextNextStop, location))
        return nextNextStop;
    }

    if (nextStop.getBlockSequence() + 2 < stopTimes.size()) {
      final BlockStopTimeEntry nextNextStop = stopTimes.get(nextStop.getBlockSequence() + 2);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, nextNextStop, location))
        return nextNextStop;
    }

    return null;
  }

  public double getDistanceToBlockLocation(Observation obs,
      BlockState blockState) {

    final ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    final CoordinatePoint blockStart = blockLocation.getLocation();
    final CoordinatePoint currentLocation = obs.getLocation();

    return SphericalGeometryLibrary.distance(currentLocation, blockStart);
  }

  public boolean isOffBlock(Observation obs, BlockState blockState) {
    return getDistanceToBlockLocation(obs, blockState) > _offBlockDistance;
  }

  /****
   * Private Methods
   ****/

  static private boolean tripChangesBetweenPrevAndNextStop(
      List<BlockStopTimeEntry> stopTimes, BlockStopTimeEntry nextStop,
      ScheduledBlockLocation location) {

    final BlockStopTimeEntry previousStop = stopTimes.get(nextStop.getBlockSequence() - 1);
    final BlockTripEntry nextTrip = nextStop.getTrip();
    final BlockTripEntry previousTrip = previousStop.getTrip();

    return !previousTrip.equals(nextTrip)
        && isLayoverInRange(previousStop, nextStop, location);
  }

  static private boolean isLayoverInRange(BlockStopTimeEntry prevStop,
      BlockStopTimeEntry nextStop, ScheduledBlockLocation location) {

    if (prevStop.getDistanceAlongBlock() <= location.getDistanceAlongBlock()
        && location.getDistanceAlongBlock() <= nextStop.getDistanceAlongBlock())
      return true;

    final double d1 = Math.abs(location.getDistanceAlongBlock()
        - prevStop.getDistanceAlongBlock());
    final double d2 = Math.abs(location.getDistanceAlongBlock()
        - nextStop.getDistanceAlongBlock());
    return Math.min(d1, d2) < _layoverStopDistance;
  }

}
