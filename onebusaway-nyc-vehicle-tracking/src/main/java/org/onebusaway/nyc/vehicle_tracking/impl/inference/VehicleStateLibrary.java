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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BaseLocationService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexFactoryService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockSequenceIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopSequenceIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockStopTimeIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehicleStateLibrary {

  private BaseLocationService _baseLocationService;

  static private double _layoverStopDistance = 400;

  /**
   * If we're more than X meters off our block, then we really don't think we're
   * serving the block any more
   */
  private double _offBlockDistance = 1000;

  private double _terminalSearchRadius = 150;

  private TransitGraphDao _transitGraphDao;

  private BlockIndexService _blockIndexService;

  private BlockGeospatialService _blockGeospatialService;

  private BlockStateService _blockStateService;

  private BlockCalendarService _blockCalendarService;

  private BlockIndexFactoryService _blockIndexFactoryService;

  @Autowired
  public void setBlockIndexFactoryService(
      BlockIndexFactoryService blockIndexFactoryService) {
    _blockIndexFactoryService = blockIndexFactoryService;
  }

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @Autowired
  public void setBlockGeospatialService(
      BlockGeospatialService blockGeospatialService) {
    _blockGeospatialService = blockGeospatialService;
  }

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
    String baseName = _baseLocationService.getBaseNameForLocation(location);

    boolean isAtBase = (baseName != null);
    return isAtBase;
  }

  public boolean isAtPotentialLayoverSpot(VehicleState state, Observation obs) {
    if (_baseLocationService.getTerminalNameForLocation(obs.getLocation()) != null)
      return true;

    /**
     * For now, we assume that if we're at the base, we're NOT in a layover
     */
    if (_baseLocationService.getBaseNameForLocation(obs.getLocation()) != null)
      return false;


    return isAtPotentialLayoverSpot(state.getBlockState(), obs);
  }

  public boolean isAtPotentialLayoverSpot(BlockState blockState, Observation obs) {

    /**
     * If there is no block assigned to this vehicle state,
     * then we allow a layover spot the terminals of all blocks. 
     */
    if (blockState == null) {
      return obs.isAtTerminal();
    }

    /**
     * Otherwise, if there is a block assigned, then we need
     * to be more specific and check only terminals for trips
     * on this block.
     */
    if (isAtPotentialTerminal(obs.getRecord(), blockState.getBlockInstance()))
      return true;
   
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    if (blockLocation == null)
      return false;

    return getPotentialLayoverSpot(blockLocation) != null;
  }

  public static boolean isAtPotentialLayoverSpot(ScheduledBlockLocation location) {
    return getPotentialLayoverSpot(location) != null;
  }

  /**
   * This method determines if an observation record is within
   * a certain radius, _terminalSearchRadius argument, of a stop at the
   * start or end of a trip on a given block, blockInstance argument. 
   * @param record
   * @param blockInstance
   * @return whether the observation is within the search radius
   */
  public boolean isAtPotentialTerminal(NycRawLocationRecord record,
      BlockInstance blockInstance) {

    CoordinatePoint loc = new CoordinatePoint(record.getLatitude(),
        record.getLongitude());

    for (BlockTripEntry bte : blockInstance.getBlock().getTrips()) {
      /*
       * is this the first stop on this trip?
       */
      List<StopTimeEntry> stopsOnTrip = bte.getTrip().getStopTimes();

      double firstStopDist = SphericalGeometryLibrary.distance(loc, stopsOnTrip
          .get(0).getStop().getStopLocation());
      int lastStopIdx = stopsOnTrip.size() - 1;
      double lastStopDist = SphericalGeometryLibrary.distance(loc, stopsOnTrip
          .get(lastStopIdx).getStop().getStopLocation());

      if (firstStopDist <= _terminalSearchRadius
          || lastStopDist <= _terminalSearchRadius) {
        return true;
      }
    }

    return false;
  }

  /**
   * This method determines if an observation record is within
   * a certain radius, _terminalSearchRadius argument, of a stop at the
   * start or end of a trip.
   * <br>
   * Note: all trips' stops within the radius are checked.
   *  
   * @param record
   * @return whether the observation is within the search radius
   */
  public boolean isAtPotentialTerminal(NycRawLocationRecord record) {

    CoordinatePoint loc = new CoordinatePoint(record.getLatitude(),
        record.getLongitude());
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(loc,
        _terminalSearchRadius);

    List<StopEntry> stops = _transitGraphDao.getStopsByLocation(bounds);
    for (StopEntry stop : stops) {
      List<BlockStopTimeIndex> stopTimeIndices = _blockIndexService
          .getStopTimeIndicesForStop(stop);
      for (BlockStopTimeIndex stopTimeIndex : stopTimeIndices) {
        for (BlockTripEntry bte : stopTimeIndex.getTrips()) {
          /*
           * is this the first stop on this trip?
           */
          List<StopTimeEntry> stopsOnTrip = bte.getTrip().getStopTimes();
          if (stop.equals(stopsOnTrip.get(0).getStop())) {
            return true;
          }

          int numOfStops = stopsOnTrip.size() - 1;
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
      BlockStopTimeEntry nextNextStop = stopTimes.get(nextStop
          .getBlockSequence() + 1);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, nextNextStop, location))
        return nextNextStop;
    }

    if (nextStop.getBlockSequence() + 2 < stopTimes.size()) {
      BlockStopTimeEntry nextNextStop = stopTimes.get(nextStop
          .getBlockSequence() + 2);
      if (tripChangesBetweenPrevAndNextStop(stopTimes, nextNextStop, location))
        return nextNextStop;
    }

    return null;
  }

  public double getDistanceToBlockLocation(Observation obs,
      BlockState blockState) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();

    CoordinatePoint blockStart = blockLocation.getLocation();
    CoordinatePoint currentLocation = obs.getLocation();

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

    BlockStopTimeEntry previousStop = stopTimes
        .get(nextStop.getBlockSequence() - 1);
    BlockTripEntry nextTrip = nextStop.getTrip();
    BlockTripEntry previousTrip = previousStop.getTrip();

    return !previousTrip.equals(nextTrip)
        && isLayoverInRange(previousStop, nextStop, location);
  }

  static private boolean isLayoverInRange(BlockStopTimeEntry prevStop,
      BlockStopTimeEntry nextStop, ScheduledBlockLocation location) {

    if (prevStop.getDistanceAlongBlock() <= location.getDistanceAlongBlock()
        && location.getDistanceAlongBlock() <= nextStop.getDistanceAlongBlock())
      return true;

    double d1 = Math.abs(location.getDistanceAlongBlock()
        - prevStop.getDistanceAlongBlock());
    double d2 = Math.abs(location.getDistanceAlongBlock()
        - nextStop.getDistanceAlongBlock());
    return Math.min(d1, d2) < _layoverStopDistance;
  }

}
