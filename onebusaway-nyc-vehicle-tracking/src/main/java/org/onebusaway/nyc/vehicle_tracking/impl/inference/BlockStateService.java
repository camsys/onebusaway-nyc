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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

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
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.InterpolationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

@Component
public class BlockStateService {

  /**
   * Distance in meters to search around the current lat/long for blocks.
   */
  private static final double _tripSearchRadius = 60d;

  /**
   * Time in ms to search after the last stop time on the trip, compared against the search time, to determine
   * whether the trip is active.
   */
  private static final long _tripSearchTimeAfterLastStop = 5 * 60 * 60 * 1000;

  /**
   * Time in ms to search before the first stop time on the trip, compared against the search time, to determine
   * whether the trip is active.
   */
  private static final long _tripSearchTimeBeforeFirstStop = 5 * 60 * 60 * 1000;

  /**
   * If the distance moved backwards exceeds this number, the bus is not considered to have moved backwards.
   *  (i.e. no check is made against orientation to verify).
   */
  private static final double _oppositeDirMoveCutoff = 10d;

  /**
   * Set this field to true if you want to require snapped states
   * match the DSC implied Route
   */
  private static boolean _requireDSCImpliedRoutes = true;

  /**
   * Set this field to true if you want to only allow snapping to
   * best fuzzy or operator assigned runs, when no valid DSC
   * is available.
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

  private BlockCalendarService _blockCalendarService;

  private TransitGraphDao _transitGraphDao;

  private Multimap<LocationIndexedLine, TripInfo> _linesToTripInfo;

  private ShapePointService _shapePointService;

  private Multimap<Entry<AgencyAndId, BlockConfigurationEntry>, BlockTripEntry> _shapesAndBlockConfigsToBlockTrips;

  @PostConstruct
  @Refreshable(dependsOn = {
      RefreshableResources.TRANSIT_GRAPH, 
      RefreshableResources.NARRATIVE_DATA})
  public void setup() throws IOException, ClassNotFoundException {
    buildShapeSpatialIndex();
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService calendarService) {
    _blockCalendarService = calendarService;
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
  public void setDestinationSignCodeService(DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setScheduledBlockService(ScheduledBlockLocationService scheduledBlockLocationService) {
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
    synchronized(observation) {
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

    GregorianCalendar gc = new GregorianCalendar();
    gc.setTimeInMillis(observation.getTime());
    gc.set(GregorianCalendar.MINUTE, (int)Math.floor(gc.get(GregorianCalendar.MINUTE) / 15) * 60);
    gc.set(GregorianCalendar.SECOND, 0);
    final long time = gc.getTimeInMillis();        
    final Date timeFrom = new Date(time - _tripSearchTimeAfterLastStop);
    final Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);

    final NycRawLocationRecord record = observation.getRecord();
    final CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
        record.getLatitude(), record.getLongitude(), _tripSearchRadius);
    final Coordinate obsPoint = new Coordinate(record.getLongitude(),
        record.getLatitude());
    final Envelope searchEnv = new Envelope(bounds.getMinLon(),
        bounds.getMaxLon(), bounds.getMinLat(), bounds.getMaxLat());
    
    Set<String> validRunIds = Sets.newHashSet(Iterables.concat(observation.getBestFuzzyRunIds(),
                      Collections.singleton(observation.getOpAssignedRunId())));

    @SuppressWarnings("unchecked")
    final List<Collection<LocationIndexedLine>> lineMatches = _index.query(searchEnv);
    final Multimap<AgencyAndId, BlockInstance> blockToActiveInstances = HashMultimap.create();
    final Multimap<BlockInstance, Double> instancesToDists = TreeMultimap.create(
        BlockInstanceComparator.INSTANCE, Ordering.natural());
    for (final LocationIndexedLine line : Iterables.concat(lineMatches)) {
      final LinearLocation here = line.project(obsPoint);
      final Coordinate pointOnLine = line.extractPoint(here);
      final double dist = SphericalGeometryLibrary.distance(pointOnLine.y,
          pointOnLine.x, obsPoint.y, obsPoint.x);

      if (dist > _tripSearchRadius)
        continue;

      final Coordinate startOfLine = line.extractPoint(line.getStartIndex());
      final double distTraveledOnLine = SphericalGeometryLibrary.distance(
          pointOnLine.y, pointOnLine.x, startOfLine.y, startOfLine.x);

      for (final TripInfo tripInfo : _linesToTripInfo.get(line)) {
        for (final BlockEntry block : tripInfo.getBlocks()) {

          /*
           * Avoid getting active blockInstances when there are none for this
           * block (since the multimap will return an empty set when it has no
           * entry and when there are no active blockInstances).
           */
          final Collection<BlockInstance> instances;
          if (!blockToActiveInstances.containsKey(block.getId())) {
            instances = blockToActiveInstances.get(block.getId());
            instances.addAll(_blockCalendarService.getActiveBlocks(
                block.getId(), timeFrom.getTime(), timeTo.getTime()));
            if(!instances.isEmpty())
            	blockToActiveInstances.putAll(block.getId(), instances);
          } else {
            instances = blockToActiveInstances.get(block.getId());
          }

          for (final BlockInstance instance : instances) {

            final Collection<BlockTripEntry> blockTrips = _shapesAndBlockConfigsToBlockTrips.get(Maps.immutableEntry(
                tripInfo.getShapeAndIdx().getShapeId(), instance.getBlock()));

            final double distanceAlongShape = tripInfo.getDistanceFrom()
                + distTraveledOnLine;

            for (final BlockTripEntry blockTrip : blockTrips) {
              /*
               * XXX: This is still questionable, however,
               * ScheduledBlockLocationServiceImpl.java appears to do something
               * similar, where it assumes the block's distance-along can be
               * related to the shape's (for the particular BlockTripEntry).
               * (see ScheduledBlockLocationServiceImpl.getLocationAlongShape)
               * 
               * Anyway, what we're doing is using the blockTrip's
               * getDistanceAlongBlock to find out what the distance-along the
               * block is for the start of the shape, then we're using our
               * computed distance along shape for the snapped point to find the
               * total distanceAlongBlock.
               */
              double distanceAlongBlock = blockTrip.getDistanceAlongBlock()
                  + distanceAlongShape;

              /*
               * Here we make sure that the DSC and/or run-info matches
               */
              if (_requireDSCImpliedRoutes) {
                if (!observation.getImpliedRouteCollections()
                      .contains(blockTrip.getTrip().getRouteCollection().getId()))
                  continue;
              }
                
              if (_requireRunMatchesForNullDSC) {
                /*
                 * When there is no valid DSC only allow snapping
                 * to assigned or best fuzzy run.
                 */
                if (!observation.hasValidDsc()) {
                  if (Sets.intersection(validRunIds, 
                      _runService.getRunIdsForTrip(blockTrip.getTrip())).isEmpty()) {
                    continue;
                  }
                } 
              }
                
              if (distanceAlongBlock > instance.getBlock().getTotalBlockDistance()) {
                distanceAlongBlock = instance.getBlock().getTotalBlockDistance();
              }

              instancesToDists.put(instance, distanceAlongBlock);
            }
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
        
        /*
         * Skip other agencies.
         * TODO query TripInfo by agency? 
         */
        if (!Objects.equal(location.getActiveTrip().getTrip().getId().getAgencyId(),
            record.getVehicleId().getAgencyId()))
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
        final double dist = SphericalGeometryLibrary.distance(
            observation.getLocation(), location.getLocation());
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
       * We get NaN when the locations are the same;
       * in which case, we don't have a comparison to make,
       * so use an orientation difference of 0.
       */
      final double locOrientation = Angle.normalize(Angle.toRadians(location.getOrientation()));
      final double obsOrientation;
      if (Double.isNaN(observation.getOrientation())) {
        obsOrientation = locOrientation;
      } else {
        obsOrientation = Angle.normalize(Angle.toRadians(observation.getOrientation()));
      }
      
      double orientDiff = Angle.diff(obsOrientation, locOrientation);
      if (orientDiff >= Math.PI * 3d/4d) {
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
    final ShapeIdxAndId _shapeAndIdx;
    final private Collection<BlockEntry> _blocks;

    public TripInfo(double distanceFrom, double distanceTo,
        Collection<BlockEntry> blocks, ShapeIdxAndId shapeIdxAndId) {
      _distanceFrom = distanceFrom;
      _shapeAndIdx = shapeIdxAndId;
      _blocks = blocks;
    }

    public Collection<BlockEntry> getBlocks() {
      return _blocks;
    }

    public double getDistanceFrom() {
      return _distanceFrom;
    }

    public ShapeIdxAndId getShapeAndIdx() {
      return _shapeAndIdx;
    }

  };

  private static class ShapeIdxAndId {
    final private AgencyAndId _shapeId;

    public ShapeIdxAndId(AgencyAndId shapeId, int shapePointIndex) {
      _shapeId = shapeId;
    }

    public AgencyAndId getShapeId() {
      return _shapeId;
    }
  }

  /**
   * 
   * Build the maps and STR tree for look-up.
   * 
   */
  private void buildShapeSpatialIndex() throws IOException, ClassNotFoundException {
    try {
      _shapesAndBlockConfigsToBlockTrips = HashMultimap.create();
      _linesToTripInfo = HashMultimap.create();

      final Multimap<AgencyAndId, BlockEntry> allUniqueShapePoints = HashMultimap.create();
      final GeometryFactory gf = new GeometryFactory();
      final Multimap<Envelope, LocationIndexedLine> envToLines = HashMultimap.create();

      _log.debug("generating shapeId & blockConfig to block trips map...");
      for (final BlockEntry blockEntry : _transitGraphDao.getAllBlocks()) {
        _log.debug(blockEntry.getId().toString());
        for (final BlockConfigurationEntry blockConfig : blockEntry.getConfigurations()) {
          for (final BlockTripEntry blockTrip : blockConfig.getTrips()) {
            final TripEntry trip = blockTrip.getTrip();
            final AgencyAndId shapeId = trip.getShapeId();
            if (shapeId != null) {
              _shapesAndBlockConfigsToBlockTrips.put(Maps.immutableEntry(shapeId, blockConfig), blockTrip);
              allUniqueShapePoints.put(shapeId, blockEntry);
            }
          }
        }
      }

      _log.debug("\tshapePoints=" + allUniqueShapePoints.keySet().size());
      for (final Entry<AgencyAndId, Collection<BlockEntry>> shapePointsEntry : allUniqueShapePoints.asMap().entrySet()) {
        final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapePointsEntry.getKey());
        if (shapePoints == null || shapePoints.isEmpty()) {
          _log.debug("blocks with no shapes: " + shapePointsEntry.getValue());
          continue;
        }

        final Collection<BlockEntry> blocks = shapePointsEntry.getValue();
        for (int i = 0; i < shapePoints.getSize() - 1; ++i) {
          final CoordinatePoint from = shapePoints.getPointForIndex(i);
          final CoordinatePoint to = shapePoints.getPointForIndex(i + 1);

          final Coordinate fromJts = new Coordinate(from.getLon(),
              from.getLat());
          final Coordinate toJts = new Coordinate(to.getLon(), to.getLat());

          final Geometry lineGeo = gf.createLineString(new Coordinate[] {
              fromJts, toJts});
          final LocationIndexedLine line = new LocationIndexedLine(lineGeo);
          final Envelope env = lineGeo.getEnvelopeInternal();

          final double distanceFrom = shapePoints.getDistTraveledForIndex(i);
          final double distanceTo = shapePoints.getDistTraveledForIndex(i + 1);
          _linesToTripInfo.put(line, new TripInfo(distanceFrom, distanceTo,
              blocks, new ShapeIdxAndId(shapePoints.getShapeId(), i)));

          envToLines.put(env, line);
        }
      }

      if (envToLines.size() > 0) {
        _log.debug("\ttree size=" + envToLines.keySet().size());
        _index = new STRtree(envToLines.keySet().size());
        for (final Entry<Envelope, Collection<LocationIndexedLine>> envLines : envToLines.asMap().entrySet()) {
          _index.insert(envLines.getKey(), envLines.getValue());
        }
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
    
    _log.debug("done.");
  }

}
