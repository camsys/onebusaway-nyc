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

import org.onebusaway.collections.Min;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.BlockInstanceComparator;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.TurboButton;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.blocks.IndexAdapters;
import org.onebusaway.transit_data_federation.impl.shapes.DistanceTraveledShapePointIndex;
import org.onebusaway.transit_data_federation.impl.shapes.PointAndOrientation;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointIndex;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.impl.time.GenericBinarySearch;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.InterpolationLibrary;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

@Component
public class BlockStateService {

  /**
   * Distance in meters to search around the current lat/long for blocks.
   */
  private static final double _tripSearchRadius = 60d;

  /**
   * Time in ms to search after the last stop time on the trip, compared against
   * the search time, to determine whether the trip is active.
   */
  private static final long _tripSearchTimeAfterLastStop = 5 * 60 * 60 * 1000;

  /**
   * Time in ms to search before the first stop time on the trip, compared
   * against the search time, to determine whether the trip is active.
   */
  private static final long _tripSearchTimeBeforeFirstStop = 5 * 60 * 60 * 1000;

  /**
   * If the distance moved backwards exceeds this number, the bus is not
   * considered to have moved backwards. (i.e. no check is made against
   * orientation to verify).
   */
  private static final double _oppositeDirMoveCutoff = 10d;

  /**
   * Set this field to true if you want to require snapped states match the DSC
   * implied Route
   */
  private static boolean _requireDSCImpliedRoutes = true;

  /**
   * Set this field to true if you want to only allow snapping to best fuzzy or
   * operator assigned runs, when no valid DSC is available.
   */
  private static boolean _requireRunMatchesForNullDSC = true;

  /**
   * This will sound weird, but DON'T REMOVE THIS
   */
  private final Logger _log = LoggerFactory.getLogger(BlockStateService.class);

  private ObservationCache _observationCache;

  private DestinationSignCodeService _destinationSignCodeService;

  private RunService _runService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ShapePointsLibrary _shapePointsLibrary;

  private SpatialIndex _index;

  @Autowired
  private BlockCalendarService _blockCalendarService;

  @Autowired
  private BlockIndexService _blockIndexService;

  private TransitGraphDao _transitGraphDao;

  private Map<String, Multimap<LocationIndexedLine, TripInfo>> _linesToTripInfoByAgencyId;

  private ShapePointService _shapePointService;

  @PostConstruct
  @Refreshable(dependsOn = {
      RefreshableResources.TRANSIT_GRAPH, RefreshableResources.NARRATIVE_DATA})
  public void setup() throws IOException, ClassNotFoundException {
    // buildShapeSpatialIndex();
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
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
  public void setShapePointService(ShapePointService shapePointService) {
    _shapePointService = shapePointService;
  }

  @Autowired
  public void setShapePointsLibrary(ShapePointsLibrary shapePointsLibrary) {
    _shapePointsLibrary = shapePointsLibrary;
  }

  public void setLocalMinimumThreshold(double localMinimumThreshold) {
    _shapePointsLibrary.setLocalMinimumThreshold(localMinimumThreshold);
  }

  public static double getOppositeDirMoveCutoff() {
    return _oppositeDirMoveCutoff;
  }

  public static boolean isRequireDSCImpliedRoutes() {
    return _requireDSCImpliedRoutes;
  }

  public static void setRequireDSCImpliedRoutes(boolean requireDSCImpliedRoutes) {
    _requireDSCImpliedRoutes = requireDSCImpliedRoutes;
  }

  public static boolean isRequireRunMatchesForNullDSC() {
    return _requireRunMatchesForNullDSC;
  }

  public static void setRequireRunMatchesForNullDSC(
      boolean requireRunMatchesForNullDSC) {
    _requireRunMatchesForNullDSC = requireRunMatchesForNullDSC;
  }

  public BestBlockStates getBestBlockLocations(Observation observation,
      BlockInstance blockInstance, double blockDistanceFrom,
      double blockDistanceTo) throws MissingShapePointsException {

    synchronized (observation) {
      final BlockLocationKey key = new BlockLocationKey(blockInstance, 0,
          Double.POSITIVE_INFINITY);

      Map<BlockLocationKey, BestBlockStates> mb = _observationCache.getValueForObservation(
          observation, EObservationCacheKey.BLOCK_LOCATION);

      if (mb == null) {
        mb = computeBlockStatesForObservation(observation);
        _observationCache.putValueForObservation(observation,
            EObservationCacheKey.BLOCK_LOCATION, mb);

        final Set<BlockState> m = Sets.newHashSet();
        for (final BestBlockStates bbs : mb.values()) {
          m.addAll(bbs.getAllStates());
        }
        _observationCache.putValueForObservation(observation,
            EObservationCacheKey.BEST_BLOCK_STATES, m);
      }

      BestBlockStates blockStates = mb.get(key);

      if (blockStates == null) {
        blockStates = new BestBlockStates(Collections.<BlockState> emptySet());
      }

      return blockStates;
    }
  }

  public BlockState getScheduledTimeAsState(BlockInstance blockInstance,
      int scheduledTime) {
    final BlockConfigurationEntry blockConfig = blockInstance.getBlock();

    final ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        blockConfig, scheduledTime);

    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " scheduleTime=" + scheduledTime);

    final BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    final String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    final RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(
        activeTrip.getTrip(), scheduledTime);

    return new BlockState(blockInstance, blockLocation, rte, dsc);
  }

  /****
   * Private Methods
   ****/

  public static class BestBlockStates {

    final private Set<BlockState> _bestStates;

    public BestBlockStates(Set<BlockState> hashSet) {
      _bestStates = hashSet;
    }

    public Set<BlockState> getAllStates() {
      return _bestStates;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper("BestBlockStates").add("bestStates",
          _bestStates).toString();
    }

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
      final BlockLocationKey other = (BlockLocationKey) obj;
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

  public BlockState getAsState(BlockInstance blockInstance, ScheduledBlockLocation blockLocation,
      double distanceAlongBlock) {
    
    final ScheduledBlockLocation newBlockLocation = _scheduledBlockLocationService.
        getScheduledBlockLocationFromDistanceAlongBlock(blockLocation, distanceAlongBlock);

    final BlockTripEntry activeTrip = newBlockLocation.getActiveTrip();
    final String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    final RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(
        activeTrip.getTrip(), newBlockLocation.getScheduledTime());
    return new BlockState(blockInstance, newBlockLocation, rte, dsc);
    
  }
  
  public BlockState getAsState(BlockInstance blockInstance,
      double distanceAlongBlock) {

    final BlockConfigurationEntry block = blockInstance.getBlock();

    if (distanceAlongBlock < 0.0)
      return null;

    if (distanceAlongBlock > block.getTotalBlockDistance())
      distanceAlongBlock = block.getTotalBlockDistance();

    final ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
        block, distanceAlongBlock);

    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " d=" + distanceAlongBlock);

    final BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    final String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    final RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(
        activeTrip.getTrip(), blockLocation.getScheduledTime());
    return new BlockState(blockInstance, blockLocation, rte, dsc);
  }

  public Set<BlockState> getBlockStatesForObservation(Observation observation) {
    synchronized (observation) {
      Set<BlockState> m = _observationCache.getValueForObservation(observation,
          EObservationCacheKey.BEST_BLOCK_STATES);

      if (m == null) {
        final Map<BlockLocationKey, BestBlockStates> res = computeBlockStatesForObservation(observation);
        _observationCache.putValueForObservation(observation,
            EObservationCacheKey.BLOCK_LOCATION, res);

        m = Sets.newHashSet();
        for (final BestBlockStates bbs : res.values()) {
          m.addAll(bbs.getAllStates());
        }
        _observationCache.putValueForObservation(observation,
            EObservationCacheKey.BEST_BLOCK_STATES, m);
      }
      return m;
    }
  }

  private Map<BlockLocationKey, BestBlockStates> computeBlockStatesForObservation(
      Observation observation) {

    final Map<BlockLocationKey, BestBlockStates> results = Maps.newHashMap();

    Set<String> validRunIds = null;
    if (_requireRunMatchesForNullDSC) {
      validRunIds = Sets.newHashSet(Iterables.concat(
          observation.getBestFuzzyRunIds(),
          Collections.singleton(observation.getOpAssignedRunId())));
    }

    final NycRawLocationRecord record = observation.getRecord();
    final String vehicleAgencyId = record.getVehicleId().getAgencyId();

    final long time = observation.getTime();
    final Date timeFrom = new Date(time - _tripSearchTimeAfterLastStop);
    final Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);

    final CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
        record.getLatitude(), record.getLongitude(), _tripSearchRadius);
    final Coordinate obsPoint = new Coordinate(record.getLongitude(),
        record.getLatitude());
    final Envelope searchEnv = new Envelope(bounds.getMinLon(),
        bounds.getMaxLon(), bounds.getMinLat(), bounds.getMaxLat());

    @SuppressWarnings("unchecked")
    final List<Collection<LocationIndexedLine>> lineMatches = _index.query(searchEnv);
    final Multimap<BlockInstance, Double> instancesToDists = TreeMultimap.create(
        BlockInstanceComparator.INSTANCE, Ordering.natural());

    // lines under the current observed location
    for (final LocationIndexedLine line : Iterables.concat(lineMatches)) {
      final LinearLocation here = line.project(obsPoint);
      final Coordinate pointOnLine = line.extractPoint(here);
      final double dist = pointOnLine.distance(obsPoint);

      // filter out if too far away
      if (dist > _tripSearchRadius)
        continue;

      final Multimap<LocationIndexedLine, TripInfo> linesToTripInfoForVehicleAgencyId = _linesToTripInfoByAgencyId.get(vehicleAgencyId);
      if (linesToTripInfoForVehicleAgencyId == null
          || linesToTripInfoForVehicleAgencyId.isEmpty()) {
        continue;
      }

      // trips that follow the path under the current observed location
      for (final TripInfo tripInfo : linesToTripInfoForVehicleAgencyId.get(line)) {
        final Collection<BlockTripIndex> indices = tripInfo.getIndices();
        final Collection<BlockLayoverIndex> layoverIndices = tripInfo.getLayoverIndices();
        final Collection<FrequencyBlockTripIndex> frequencyIndices = tripInfo.getFrequencyIndices();

        final Coordinate startOfLine = line.extractPoint(line.getStartIndex());
        final double distTraveledOnLine = TurboButton.distance(pointOnLine.y,
            pointOnLine.x, startOfLine.y, startOfLine.x);

        final double distanceAlongShape = tripInfo.getDistanceFrom()
            + distTraveledOnLine;

        final List<BlockInstance> instances = _blockCalendarService.getActiveBlocksInTimeRange(
            indices, layoverIndices, frequencyIndices, timeFrom.getTime(),
            timeTo.getTime());

        for (final BlockInstance instance : instances) {
          for (final BlockTripEntry blockTrip : instance.getBlock().getTrips()) {
            /*
             * XXX: This is still questionable, however,
             * ScheduledBlockLocationServiceImpl.java appears to do something
             * similar, where it assumes the block's distance-along can be
             * related to the shape's (for the particular BlockTripEntry). (see
             * ScheduledBlockLocationServiceImpl.getLocationAlongShape)
             * 
             * Anyway, what we're doing is using the blockTrip's
             * getDistanceAlongBlock to find out what the distance-along the
             * block is for the start of the shape, then we're using our
             * computed distance along shape for the snapped point to find the
             * total distanceAlongBlock.
             */
            final double distanceAlongBlock = blockTrip.getDistanceAlongBlock()
                + distanceAlongShape;

            /*
             * Here we make sure that the DSC and/or run-info matches
             */
            if (_requireDSCImpliedRoutes) {
              if (!observation.getImpliedRouteCollections().contains(
                  blockTrip.getTrip().getRouteCollection().getId()))
                continue;
            }

            if (_requireRunMatchesForNullDSC) {
              /*
               * When there is no valid DSC only allow snapping to assigned or
               * best fuzzy run.
               */
              if (!observation.hasValidDsc()) {
                if (Sets.intersection(validRunIds,
                    _runService.getRunIdsForTrip(blockTrip.getTrip())).isEmpty()) {
                  continue;
                }
              }
            }

            instancesToDists.put(instance, distanceAlongBlock);
          }
        }
      }
    }

    for (final Entry<BlockInstance, Collection<Double>> biEntry : instancesToDists.asMap().entrySet()) {
      final BlockInstance instance = biEntry.getKey();
      final int searchTimeFrom = (int) (timeFrom.getTime() - instance.getServiceDate()) / 1000;
      final int searchTimeTo = (int) (timeTo.getTime() - instance.getServiceDate()) / 1000;

      final Map<Map.Entry<BlockTripEntry, String>, Min<ScheduledBlockLocation>> tripToDists = Maps.newHashMap();

      /*
       * TODO could do some really easy improved traversing of these
       * distances...
       */
      for (final Double distanceAlongBlock : biEntry.getValue()) {
        final ScheduledBlockLocation location = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
            instance.getBlock(), distanceAlongBlock);

        if (location == null)
          continue;

        /*
         * Don't consider opposite direction trips.
         */
        if (movedInOppositeDirection(observation, location))
          continue;

        /*
         * Should be increasing time for increasing distanceAlongBlock...
         */
        final int schedTime = location.getScheduledTime();

        if (schedTime > searchTimeTo)
          break;

        if (schedTime < searchTimeFrom)
          continue;

        final BlockTripEntry activeTrip = location.getActiveTrip();
        final Map.Entry<BlockTripEntry, String> tripDistEntry = Maps.immutableEntry(
            activeTrip, activeTrip.getTrip().getDirectionId());

        Min<ScheduledBlockLocation> thisMin = tripToDists.get(tripDistEntry);
        if (thisMin == null) {
          thisMin = new Min<ScheduledBlockLocation>();
          tripToDists.put(tripDistEntry, thisMin);
        }

        final double dist = TurboButton.distance(observation.getLocation(),
            location.getLocation());
        thisMin.add(dist, location);
      }

      for (final Min<ScheduledBlockLocation> thisMin : tripToDists.values()) {
        final ScheduledBlockLocation location = thisMin.getMinElement();
        final BlockTripEntry activeTrip = location.getActiveTrip();
        final String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
        final RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(
            activeTrip.getTrip(), location.getScheduledTime());

        final BlockState state = new BlockState(instance, location, rte, dsc);
        final BlockLocationKey key = new BlockLocationKey(instance, 0,
            Double.POSITIVE_INFINITY);

        BestBlockStates currentStates = results.get(key);
        if (currentStates == null) {
          currentStates = new BestBlockStates(Sets.newHashSet(state));
          results.put(key, currentStates);
        } else {
          currentStates.getAllStates().add(state);
        }
      }
    }

    return results;
  }

  private boolean movedInOppositeDirection(Observation observation,
      ScheduledBlockLocation location) {

    if (observation.getPreviousRecord() != null
        && observation.getDistanceMoved() >= _oppositeDirMoveCutoff) {
      /*
       * We get NaN when the locations are the same; in which case, we don't
       * have a comparison to make, so use an orientation difference of 0.
       */
      final double locOrientation = Angle.normalize(Angle.toRadians(location.getOrientation()));
      final double obsOrientation;
      if (Double.isNaN(observation.getOrientation())) {
        obsOrientation = locOrientation;
      } else {
        obsOrientation = Angle.normalize(Angle.toRadians(observation.getOrientation()));
      }

      final double orientDiff = Angle.diff(obsOrientation, locOrientation);
      if (orientDiff >= Math.PI * 3d / 4d) {
        return true;
      }
    }

    return false;
  }

  /**
   * Computes distance along a blockConfigEntry for a given starting stop-time
   * index.<br>
   * Here's an example of how to get the index:<br>
   * 
   * <pre>
   * {@code
   * List<BlockStopTimeEntry> stopTimes = blockConfig.getStopTimes();
   * int n = stopTimes.size();
   * int index = GenericBinarySearch.search(blockConfig, n, scheduleTime,
   *     IndexAdapters.BLOCK_STOP_TIME_DEPARTURE_INSTANCE);
   * }
   * </pre>
   * 
   * <br>
   * Code taken from ScheduledBlockLocationServiceImpl.
   * 
   * @param blockEntry
   * @param fromStopTimeIndex
   * @param scheduleTime
   * @return
   */
  public double getDistanceAlongBlock(BlockConfigurationEntry blockEntry,
      int fromStopTimeIndex, int scheduleTime) {

    final List<BlockStopTimeEntry> stopTimes = blockEntry.getStopTimes();

    Preconditions.checkElementIndex(fromStopTimeIndex, stopTimes.size() + 1);

    double distanceAlongBlock = Double.NaN;
    if (fromStopTimeIndex == 0) {
      final BlockStopTimeEntry blockStopTime = stopTimes.get(0);
      final StopTimeEntry stopTime = blockStopTime.getStopTime();

      /**
       * If we have more than one stop time in the block (we'd hope!), then we
       * attempt to interpolate the distance along the block
       */
      if (stopTimes.size() > 1) {

        final BlockStopTimeEntry secondBlockStopTime = stopTimes.get(1);
        final StopTimeEntry secondStopTime = secondBlockStopTime.getStopTime();

        distanceAlongBlock = InterpolationLibrary.interpolatePair(
            stopTime.getDepartureTime(), blockStopTime.getDistanceAlongBlock(),
            secondStopTime.getArrivalTime(),
            secondBlockStopTime.getDistanceAlongBlock(), scheduleTime);

        if (distanceAlongBlock < 0)
          distanceAlongBlock = 0.0;
      }
    } else {
      while (fromStopTimeIndex < stopTimes.size()) {
        final int t = blockEntry.getDepartureTimeForIndex(fromStopTimeIndex);
        if (scheduleTime <= t)
          break;
        fromStopTimeIndex++;
      }

      if (fromStopTimeIndex < stopTimes.size()) {
        final BlockStopTimeEntry blockBefore = stopTimes.get(fromStopTimeIndex - 1);
        final BlockStopTimeEntry blockAfter = stopTimes.get(fromStopTimeIndex);

        final StopTimeEntry before = blockBefore.getStopTime();
        final StopTimeEntry after = blockAfter.getStopTime();

        final int fromTime = before.getDepartureTime();
        final int toTime = after.getArrivalTime();

        final double ratio = (scheduleTime - fromTime)
            / ((double) (toTime - fromTime));

        final double fromDistance = blockBefore.getDistanceAlongBlock();
        final double toDistance = blockAfter.getDistanceAlongBlock();

        distanceAlongBlock = ratio * (toDistance - fromDistance) + fromDistance;
      } else {
        distanceAlongBlock = blockEntry.getTotalBlockDistance();
      }
    }

    return distanceAlongBlock;
  }

  private static class TripInfo {
    final double _distanceFrom;
    final private Collection<BlockTripIndex> _indices;
    final private Collection<BlockLayoverIndex> _layoverIndices;
    final private Collection<FrequencyBlockTripIndex> _frequencyIndices;

    public TripInfo(double distanceFrom, double distanceTo,
        Collection<BlockTripIndex> indices,
        Collection<BlockLayoverIndex> layoverIndices,
        Collection<FrequencyBlockTripIndex> frequencyIndices) {
      _distanceFrom = distanceFrom;
      _indices = indices;
      _layoverIndices = layoverIndices;
      _frequencyIndices = frequencyIndices;
    }

    public double getDistanceFrom() {
      return _distanceFrom;
    }

    public Collection<BlockTripIndex> getIndices() {
      return _indices;
    }

    public Collection<BlockLayoverIndex> getLayoverIndices() {
      return _layoverIndices;
    }

    public Collection<FrequencyBlockTripIndex> getFrequencyIndices() {
      return _frequencyIndices;
    }
  }
  

  public BlockState getAsState(BlockTripEntry blockTripEntry,
      long serviceDate, double distanceAlongBlock) {
    final InstanceState instState = new InstanceState(serviceDate);
    final BlockInstance blockInstance = new BlockInstance(
        blockTripEntry.getBlockConfiguration(), instState);
    final BlockConfigurationEntry blockConfig = blockTripEntry.getBlockConfiguration();
    
    List<BlockStopTimeEntry> stopTimes = blockConfig.getStopTimes();

    int indexFrom = blockTripEntry.getStopTimes().get(0).getBlockSequence();
    int indexTo = indexFrom + 1;

    while (indexTo < stopTimes.size()) {
      double d = blockConfig.getDistanceAlongBlockForIndex(indexTo);
      if (distanceAlongBlock <= d)
        break;
      indexTo++;
    }

    int stopTimeIndex = GenericBinarySearch.searchRange(blockConfig, indexFrom,
        indexTo, distanceAlongBlock,
        IndexAdapters.BLOCK_CONFIG_DISTANCE_INSTANCE);

    ScheduledBlockLocation blockLocation = getScheduledBlockLocationFromDistanceAlongBlockAndStopTimeIndex(
        stopTimes, distanceAlongBlock, stopTimeIndex);
    
    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " d=" + distanceAlongBlock);

    final BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    Preconditions.checkState(activeTrip.equals(blockTripEntry));
    
    final String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    final RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(
        activeTrip.getTrip(), blockLocation.getScheduledTime());
    return new BlockState(blockInstance, blockLocation, rte, dsc);
  }

  private ScheduledBlockLocation getScheduledBlockLocationFromDistanceAlongBlockAndStopTimeIndex(
      List<BlockStopTimeEntry> stopTimes, double distanceAlongBlock,
      int stopTimeIndex) {

    int n = stopTimes.size();

    // Are we out beyond our last stop-time?
    if (stopTimeIndex == n) {

      // If we only have one stop time, we can't interpolate the schedule time
      if (n == 1)
        return null;

      BlockStopTimeEntry blockFrom = stopTimes.get(n - 2);
      BlockStopTimeEntry blockTo = stopTimes.get(n - 1);
      return interpolateLocation(blockFrom, blockTo, distanceAlongBlock,
          stopTimeIndex);
    }

    // Are we before out first stop-time?
    if (stopTimeIndex == 0) {

      // If we only have one stop time, we can't interpolate the schedule time
      if (n == 1)
        return null;

      BlockStopTimeEntry blockFrom = stopTimes.get(0);
      BlockStopTimeEntry blockTo = stopTimes.get(1);

      return interpolateLocation(blockFrom, blockTo, distanceAlongBlock,
          stopTimeIndex);
    }

    BlockStopTimeEntry blockBefore = stopTimes.get(stopTimeIndex - 1);
    BlockStopTimeEntry blockAfter = stopTimes.get(stopTimeIndex);

    StopTimeEntry before = blockBefore.getStopTime();
    StopTimeEntry after = blockAfter.getStopTime();

    double ratio = (distanceAlongBlock - blockBefore.getDistanceAlongBlock())
        / (blockAfter.getDistanceAlongBlock() - blockBefore.getDistanceAlongBlock());

    int scheduleTime = (int) (before.getDepartureTime() + 
        Math.ceil((after.getArrivalTime() - before.getDepartureTime()) * ratio));

    return getScheduledBlockLocationFromScheduleTimeAndStopTimeIndex(stopTimes,
        scheduleTime, stopTimeIndex);
  }

  private ScheduledBlockLocation getScheduledBlockLocationFromScheduleTimeAndStopTimeIndex(
      List<BlockStopTimeEntry> stopTimes, int scheduleTime, int stopTimeIndex) {

    // Did we have a direct hit?
    if (0 <= stopTimeIndex && stopTimeIndex < stopTimes.size()) {

      BlockStopTimeEntry blockStopTime = stopTimes.get(stopTimeIndex);
      StopTimeEntry stopTime = blockStopTime.getStopTime();

      /**
       * Is the vehicle currently at a layover at the stop?
       */
      if (stopTime.getArrivalTime() <= scheduleTime
          && scheduleTime <= stopTime.getDepartureTime()) {

        return getScheduledBlockLocationWhenAtStopTime(blockStopTime, stopTime,
            scheduleTime, stopTimeIndex);
      }
    }

    /**
     * If the stopTimeIndex is zero, and we weren't at a layover at the first
     * block stop time (see above), then we are looking for the scheduled
     * location before the scheduled start of the block
     */
    if (stopTimeIndex == 0) {
      return getScheduledBlockLocationBeforeStartOfBlock(stopTimes,
          scheduleTime);
    }

    /**
     * If the stopTimeIndex is beyond the last stop time, we don't attempt to
     * determine a scheduled location, since we're beyond the end of the block
     */
    if (stopTimeIndex == stopTimes.size()) {
      // Out of bounds for these stop times
      return null;
    }

    return getScheduledBlockLocationBetweenStopTimes(stopTimes, scheduleTime,
        stopTimeIndex);
  }

  private ScheduledBlockLocation getScheduledBlockLocationWhenAtStopTime(
      BlockStopTimeEntry blockStopTime, StopTimeEntry stopTime,
      int scheduleTime, int stopTimeIndex) {
    StopEntry stop = stopTime.getStop();

    ScheduledBlockLocation result = new ScheduledBlockLocation();

    int shapePointIndex = stopTime.getShapePointIndex();

    PointAndOrientation po = getLocationAlongShape(blockStopTime.getTrip(),
        blockStopTime.getDistanceAlongBlock(), shapePointIndex,
        shapePointIndex + 1);
    if (po != null) {
      result.setLocation(po.getPoint());
      result.setOrientation(po.getOrientation());
    } else {
      CoordinatePoint location = new CoordinatePoint(stop.getStopLat(),
          stop.getStopLon());
      result.setLocation(location);
      result.setOrientation(0);
    }

    result.setClosestStop(blockStopTime);
    result.setClosestStopTimeOffset(0);
    result.setNextStop(blockStopTime);
    result.setNextStopTimeOffset(0);
    result.setScheduledTime(scheduleTime);
    result.setDistanceAlongBlock(blockStopTime.getDistanceAlongBlock());
    result.setActiveTrip(blockStopTime.getTrip());
    result.setInService(true);
    result.setStopTimeIndex(stopTimeIndex);
    return result;
  }

  private ScheduledBlockLocation getScheduledBlockLocationBetweenStopTimes(
      List<BlockStopTimeEntry> stopTimes, int scheduleTime, int stopTimeIndex) {

    BlockStopTimeEntry blockBefore = stopTimes.get(stopTimeIndex - 1);
    BlockStopTimeEntry blockAfter = stopTimes.get(stopTimeIndex);

    StopTimeEntry before = blockBefore.getStopTime();
    StopTimeEntry after = blockAfter.getStopTime();

    ScheduledBlockLocation result = new ScheduledBlockLocation();
    result.setScheduledTime(scheduleTime);
    result.setInService(true);
    result.setStopTimeIndex(stopTimeIndex);

    int fromTime = before.getDepartureTime();
    int toTime = after.getArrivalTime();

    int fromTimeOffset = fromTime - scheduleTime;
    int toTimeOffset = toTime - scheduleTime;

    if (Math.abs(fromTimeOffset) < Math.abs(toTimeOffset)) {
      result.setClosestStop(blockBefore);
      result.setClosestStopTimeOffset(fromTimeOffset);
    } else {
      result.setClosestStop(blockAfter);
      result.setClosestStopTimeOffset(toTimeOffset);
    }

    result.setNextStop(blockAfter);
    result.setNextStopTimeOffset(toTimeOffset);

    double ratio = (scheduleTime - fromTime) / ((double) (toTime - fromTime));

    double fromDistance = blockBefore.getDistanceAlongBlock();
    double toDistance = blockAfter.getDistanceAlongBlock();

    double distanceAlongBlock = ratio * (toDistance - fromDistance)
        + fromDistance;
    result.setDistanceAlongBlock(distanceAlongBlock);

    int shapePointIndexFrom = -1;
    int shapePointIndexTo = -1;

    /**
     * Are we between trips? Where is the transition point?
     */
    if (!before.getTrip().equals(after.getTrip())) {

      if (distanceAlongBlock >= blockAfter.getTrip().getDistanceAlongBlock()) {
        result.setActiveTrip(blockAfter.getTrip());
        shapePointIndexFrom = 0;
        shapePointIndexTo = nextShapePointIndex(after);
      } else {
        result.setActiveTrip(blockBefore.getTrip());
        shapePointIndexFrom = before.getShapePointIndex();
        shapePointIndexTo = Integer.MAX_VALUE;
      }
    } else {
      result.setActiveTrip(blockBefore.getTrip());
      shapePointIndexFrom = before.getShapePointIndex();
      shapePointIndexTo = nextShapePointIndex(after);
    }

    BlockTripEntry activeTrip = result.getActiveTrip();

    PointAndOrientation po = getLocationAlongShape(activeTrip,
        distanceAlongBlock, shapePointIndexFrom, shapePointIndexTo);

    if (po != null) {
      result.setLocation(po.getPoint());
      result.setOrientation(po.getOrientation());
      return result;
    }

    StopEntry beforeStop = before.getStop();
    StopEntry afterStop = after.getStop();
    double latFrom = beforeStop.getStopLat();
    double lonFrom = beforeStop.getStopLon();
    double latTo = afterStop.getStopLat();
    double lonTo = afterStop.getStopLon();
    double lat = (latTo - latFrom) * ratio + latFrom;
    double lon = (lonTo - lonFrom) * ratio + lonFrom;

    CoordinatePoint location = new CoordinatePoint(lat, lon);
    result.setLocation(location);

    double orientation = SphericalGeometryLibrary.getOrientation(latFrom,
        lonFrom, latTo, lonTo);
    result.setOrientation(orientation);

    return result;
  }

  private ScheduledBlockLocation getScheduledBlockLocationBeforeStartOfBlock(
      List<BlockStopTimeEntry> stopTimes, int scheduleTime) {

    /**
     * The first block stop time
     */
    BlockStopTimeEntry blockStopTime = stopTimes.get(0);
    StopTimeEntry stopTime = blockStopTime.getStopTime();

    double distanceAlongBlock = Double.NaN;
    boolean inService = false;

    /**
     * If we have more than one stop time in the block (we'd hope!), then we
     * attempt to interpolate the distance along the block
     */
    if (stopTimes.size() > 1) {

      BlockStopTimeEntry secondBlockStopTime = stopTimes.get(1);
      StopTimeEntry secondStopTime = secondBlockStopTime.getStopTime();

      distanceAlongBlock = InterpolationLibrary.interpolatePair(
          stopTime.getDepartureTime(), blockStopTime.getDistanceAlongBlock(),
          secondStopTime.getArrivalTime(),
          secondBlockStopTime.getDistanceAlongBlock(), scheduleTime);

      if (distanceAlongBlock >= 0)
        inService = true;
      else
        distanceAlongBlock = 0.0;
    }

    PointAndOrientation po = null;

    if (!Double.isNaN(distanceAlongBlock))
      po = getLocationAlongShape(blockStopTime.getTrip(), distanceAlongBlock,
          0, nextShapePointIndex(stopTime));

    ScheduledBlockLocation result = new ScheduledBlockLocation();

    if (po != null) {
      result.setLocation(po.getPoint());
      result.setOrientation(po.getOrientation());
    }

    result.setClosestStop(blockStopTime);
    result.setClosestStopTimeOffset(stopTime.getArrivalTime() - scheduleTime);
    result.setNextStop(blockStopTime);
    result.setNextStopTimeOffset(stopTime.getArrivalTime() - scheduleTime);
    result.setScheduledTime(scheduleTime);
    result.setDistanceAlongBlock(distanceAlongBlock);
    result.setActiveTrip(blockStopTime.getTrip());
    result.setInService(inService);
    result.setStopTimeIndex(0);
    return result;
  }

  private ScheduledBlockLocation interpolateLocation(
      BlockStopTimeEntry blockFrom, BlockStopTimeEntry blockTo,
      double distanceAlongBlock, int stopTimeIndex) {

    if (distanceAlongBlock < 0.0)
      return null;

    StopTimeEntry from = blockFrom.getStopTime();
    StopTimeEntry to = blockTo.getStopTime();

    double r = (distanceAlongBlock - blockFrom.getDistanceAlongBlock())
        / (blockTo.getDistanceAlongBlock() - blockFrom.getDistanceAlongBlock());
    int scheduledTime = (int) (r
        * (to.getArrivalTime() - from.getDepartureTime()) + from.getDepartureTime());

    if (r > 1)
      scheduledTime += to.getSlackTime();

    BlockTripEntry activeTrip = distanceAlongBlock < blockTo.getDistanceAlongBlock()
        ? blockFrom.getTrip() : blockTo.getTrip();

    BlockStopTimeEntry closestStop = r < 0.5 ? blockFrom : blockTo;

    BlockStopTimeEntry nextStop = null;
    int shapePointIndexFrom = -1;
    int shapePointIndexTo = -1;

    if (r <= 0) {

      /**
       * Location along the block is before the two stop times
       */
      nextStop = blockFrom;
      shapePointIndexFrom = 0;
      shapePointIndexTo = nextShapePointIndex(from);

    } else if (r <= 1.0) {

      /**
       * Location along the block is between the two stop times
       */
      nextStop = blockTo;
      shapePointIndexFrom = from.getShapePointIndex();
      shapePointIndexTo = nextShapePointIndex(to);

    } else {

      /**
       * Location along the block is after the two stop times
       */
      shapePointIndexFrom = to.getShapePointIndex();
      shapePointIndexTo = Integer.MAX_VALUE;
    }

    ScheduledBlockLocation location = new ScheduledBlockLocation();
    location.setActiveTrip(activeTrip);
    location.setClosestStop(closestStop);
    location.setClosestStopTimeOffset(closestStop.getStopTime().getArrivalTime()
        - scheduledTime);
    location.setNextStop(nextStop);
    if (nextStop != null)
      location.setNextStopTimeOffset(nextStop.getStopTime().getArrivalTime()
          - scheduledTime);
    location.setInService(nextStop != null);
    location.setStopTimeIndex(stopTimeIndex);
    location.setDistanceAlongBlock(distanceAlongBlock);
    location.setScheduledTime(scheduledTime);

    /**
     * In this case, distance along block and distance along trip are the same
     * because we are still in the first trip of the block
     */

    PointAndOrientation po = getLocationAlongShape(activeTrip,
        distanceAlongBlock, shapePointIndexFrom, shapePointIndexTo);

    if (po != null) {
      location.setLocation(po.getPoint());
      location.setOrientation(po.getOrientation());
    }

    return location;
  }

  private int nextShapePointIndex(StopTimeEntry stopTime) {
    int index = stopTime.getShapePointIndex();
    if (index != -1)
      index++;
    return index;
  }

  private PointAndOrientation getLocationAlongShape(
      BlockTripEntry activeBlockTrip, double distanceAlongBlock,
      int shapePointIndexFrom, int shapePointIndexTo) {

    TripEntry activeTrip = activeBlockTrip.getTrip();
    AgencyAndId shapeId = activeTrip.getShapeId();

    if (shapeId == null)
      return null;

    ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);

    if (shapePoints == null || shapePoints.isEmpty())
      return null;

    /**
     * We allow callers of this method to specify an arbitrarily high
     * shapePointIndexTo, knowing we'll bound it by the max number of points
     */
    shapePointIndexFrom = Math.min(shapePointIndexFrom, shapePoints.getSize());
    shapePointIndexTo = Math.min(shapePointIndexTo, shapePoints.getSize());

    double distanceAlongTrip = distanceAlongBlock
        - activeBlockTrip.getDistanceAlongBlock();

    ShapePointIndex shapePointIndexMethod = new DistanceTraveledShapePointIndex(
        distanceAlongTrip, shapePointIndexFrom, shapePointIndexTo);
    return shapePointIndexMethod.getPointAndOrientation(shapePoints);
  }
  
}
