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

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ObservationCache.EObservationCacheKey;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.shapes.ProjectedShapePointService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
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

  private static final double _tripSearchRadius = 60;

  private static final long _tripSearchTimeAfterLastStop = 2 * 60 * 60 * 1000;

  private static final long _tripSearchTimeBeforeFirstStop = 1 * 60 * 60 * 1000;

  /**
   * This will sound weird, but DON'T REMOVE THIS
   */
  private final Logger _log = LoggerFactory.getLogger(BlockStateService.class);

  private ObservationCache _observationCache;

  private DestinationSignCodeService _destinationSignCodeService;

  private RunService _runService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ShapePointsLibrary _shapePointsLibrary;

  private ProjectedShapePointService _projectedShapePointService;

  private final double _threshold = 50;

  private ScheduleDeviationLibrary _scheduleDeviationLibrary;

  private SpatialIndex _index;

  private BlockIndexService _blockIndexService;

  private BlockCalendarService _blockCalendarService;

  private TransitGraphDao _transitGraphDao;

  private Multimap<LocationIndexedLine, TripInfo> _linesToTripInfo;

  private ShapePointService _shapePointService;

  private CalendarService _calendarService;

  @PostConstruct
  @Refreshable(dependsOn = {
      RefreshableResources.TRANSIT_GRAPH,
      RefreshableResources.BLOCK_INDEX_DATA,
      RefreshableResources.CALENDAR_DATA,
      RefreshableResources.SHAPE_GEOSPATIAL_INDEX})
  public void setup() throws IOException, ClassNotFoundException {
    buildShapeSpatialIndex();
  }

  @Autowired
  public void setCalendarService(CalendarService calendarService) {
    _calendarService = calendarService;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService calendarService) {
    _blockCalendarService = calendarService;
  }

  @Autowired
  public void setProjected(ProjectedShapePointService projectedShapePointService) {
    _projectedShapePointService = projectedShapePointService;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setBlockIndexService(BlockIndexService blockIndexService) {
    _blockIndexService = blockIndexService;
  }

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

  public BestBlockStates getBestBlockLocations(Observation observation,
      BlockInstance blockInstance, double blockDistanceFrom,
      double blockDistanceTo) throws MissingShapePointsException {

    final BlockLocationKey key = new BlockLocationKey(blockInstance, 0,
        Double.POSITIVE_INFINITY);

    Map<BlockLocationKey, BestBlockStates> m = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.BLOCK_LOCATION);

    if (m == null) {
      m = Maps.newConcurrentMap();
      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.BLOCK_LOCATION, m);
    }

    BestBlockStates blockStates = m.get(key);

    if (blockStates == null) {
      computeBlockStatesForObservation(observation);
      /*
       * Should be populated now, so try again...
       */
      blockStates = m.get(key);
      if (blockStates == null)
        blockStates = new BestBlockStates(Collections.<BlockState> emptySet());
    }

    return blockStates;
  }

  // public BestBlockStates getBestBlockStateForRoute(Observation observation) {
  // BestBlockStates bestRouteStates =
  // _observationCache.getValueForObservation(observation,
  // EObservationCacheKey.ROUTE_LOCATION);
  // return bestRouteStates;
  // }

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

    // final private BlockState bestTime;
    // final private BlockState bestLocation;
    // final private Double locDev;
    // final private Long timeDev;

    final private Set<BlockState> _bestStates;

    // public BestBlockStates(BestBlockObservationStates bestObsState) {
    // this.bestTime = bestObsState.getBestTime().getBlockState();
    // this.bestLocation = bestObsState.getBestLocation().getBlockState();
    // }

    public BestBlockStates(Set<BlockState> hashSet) {
      _bestStates = hashSet;
    }

    // public BestBlockStates(BlockState bestTime, BlockState bestLocation,
    // Long timeDev, Double locDev) {
    // if (bestTime == null || bestLocation == null)
    // throw new IllegalArgumentException("best block states cannot be null");
    // this.bestLocation = bestLocation;
    // this.bestTime = bestTime;
    // this.timeDev = timeDev;
    // this.locDev = locDev;
    // }

    // public BlockState getBestTime() {
    // return this.bestTime;
    // }
    //
    // public BlockState getBestLocation() {
    // return this.bestLocation;
    // }

    public Set<BlockState> getAllStates() {
      // return bestTime.equals(bestLocation) ? Arrays.asList(bestTime)
      // : Arrays.asList(bestTime, bestLocation);
      return _bestStates;
    }

    // public double getLocDev() {
    // return this.locDev;
    // }
    //
    // public double getTimeDev() {
    // return this.timeDev;
    // }
  }

  // private BestBlockStates getUncachedBestBlockLocations(
  // Observation observation, BlockInstance blockInstance,
  // double blockDistanceFrom, double blockDistanceTo)
  // throws MissingShapePointsException {
  //
  // ProjectedPoint targetPoint = observation.getPoint();
  //
  // BlockConfigurationEntry block = blockInstance.getBlock();
  //
  // List<AgencyAndId> shapePointIds = MappingLibrary.map(block.getTrips(),
  // "trip.shapeId");
  //
  // if (shapePointIds.removeAll(Collections.singleton(null))) {
  // if (shapePointIds.isEmpty())
  // throw new MissingShapePointsException("block has no shape points: "
  // + block.getBlock().getId());
  // else
  // _log.warn("block missing some shape points: "
  // + block.getBlock().getId());
  // }
  //
  // T2<List<XYPoint>, double[]> tuple =
  // _projectedShapePointService.getProjectedShapePoints(
  // shapePointIds, targetPoint.getSrid());
  //
  // List<XYPoint> projectedShapePoints = tuple.getFirst();
  // double[] distances = tuple.getSecond();
  //
  // int fromIndex = 0;
  // int toIndex = distances.length;
  //
  // if (blockDistanceFrom > 0) {
  // fromIndex = Arrays.binarySearch(distances, blockDistanceFrom);
  // if (fromIndex < 0) {
  // fromIndex = -(fromIndex + 1);
  // // Include the previous point if we didn't get an exact match
  // if (fromIndex > 0)
  // fromIndex--;
  // }
  // }
  //
  // if (blockDistanceTo < distances[distances.length - 1]) {
  // toIndex = Arrays.binarySearch(distances, blockDistanceTo);
  // if (toIndex < 0) {
  // toIndex = -(toIndex + 1);
  // // Include the previous point if we didn't get an exact match
  // if (toIndex < distances.length)
  // toIndex++;
  // }
  // }
  //
  // XYPoint xyPoint = new XYPoint(targetPoint.getX(), targetPoint.getY());
  //
  // List<PointAndIndex> assignments =
  // _shapePointsLibrary.computePotentialAssignments(
  // projectedShapePoints, distances, xyPoint, fromIndex, toIndex);
  //
  // Set<BlockState> results = Sets.newHashSet();
  // if (assignments.size() == 0) {
  // BlockState bs = getAsState(blockInstance, blockDistanceFrom);
  // return new BestBlockStates(Sets.newHashSet(bs));
  // }
  //
  //
  // Max<BlockState> best = new Max<BlockState>();
  // for (PointAndIndex index : assignments) {
  //
  // double distanceAlongBlock = index.distanceAlongShape;
  //
  // if (distanceAlongBlock > block.getTotalBlockDistance())
  // distanceAlongBlock = block.getTotalBlockDistance();
  //
  // BlockState blockState = getAsState(blockInstance, distanceAlongBlock);
  // int obsSchedDev =
  // _scheduleDeviationLibrary.computeScheduleDeviation(blockState.getBlockInstance(),
  // blockState.getBlockLocation(), observation);
  //
  // double pInP1 = 1.0 - FoldedNormalDist.cdf(10.0, 50.0,
  // index.distanceFromTarget);
  // double pInP2 = 1.0 - FoldedNormalDist.cdf(5.0, 40.0, obsSchedDev/60.0);
  // best.add(pInP1*pInP2, blockState);
  // }
  //
  // results.add(best.getMaxElement());
  //
  // return new BestBlockStates(results);
  // }

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
    Set<BlockState> m = _observationCache.getValueForObservation(observation,
        EObservationCacheKey.BEST_BLOCK_STATES);

    if (m == null) {
      m = Sets.newHashSet();
      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.BEST_BLOCK_STATES, m);
      computeBlockStatesForObservation(observation);
    }
    return m;
  }

  /**
   * New method that does an STR-tree-based lookup of block-locations.
   * XXX: Still very experimental!  
   * Run integration tests Trace_4138_20111207_150000_220000_IntegrationTest
   * and see what I mean.  There is a bug somewhere that doesn't catch block
   * MTA NYCT_MTA NYCT_20110904EE_YU_51600_MISC-158_2161 as associated
   * with any lines.
   * 
   * @param observation
   */
  public void computeBlockStatesForObservation(Observation observation) {

    Set<BlockState> m = _observationCache.getValueForObservation(observation,
        EObservationCacheKey.BEST_BLOCK_STATES);

    if (m != null && !m.isEmpty()) {
      return;
    } else if (m == null) {
      m = Sets.newHashSet();
    }

    /*
     * As a temp hack for transitioning this code, we populate the caches that
     * the other methods usually used.
     */
    Map<BlockLocationKey, BestBlockStates> mb = _observationCache.getValueForObservation(
        observation, EObservationCacheKey.BLOCK_LOCATION);

    /*
     * FIXME this is redundant
     */
    if (mb == null) {
      mb = Maps.newConcurrentMap();
      _observationCache.putValueForObservation(observation,
          EObservationCacheKey.BLOCK_LOCATION, mb);
    }

    final NycRawLocationRecord record = observation.getRecord();
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
    final List<LocationIndexedLine> lineMatches = _index.query(searchEnv);
    final Set<BlockState> blockStates = Sets.newHashSet();
    final Multimap<AgencyAndId, BlockInstance> blockToActiveInstances = HashMultimap.create();
    for (final LocationIndexedLine line : lineMatches) {

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
          final Collection<BlockInstance> instances = blockToActiveInstances.get(block.getId());

          if (instances.isEmpty()) {
            instances.addAll(_blockCalendarService.getActiveBlocks(block.getId(),
                timeFrom.getTime(), timeTo.getTime()));
          }

          for (final BlockInstance instance : instances) {

            final BlockTripEntry blockTrip = _shapesAndBlockConfigsToBlockTrips.get(
                tripInfo.getShapeAndIdx().getShapeId(), instance.getBlock());

            /*
             * XXX:
             * This is still questionable, however, ScheduledBlockLocationServiceImpl.java
             * appears to do something similar, where it assumes the block's distance-along
             * can be related to the shape's (for the particular BlockTripEntry).
             * (see ScheduledBlockLocationServiceImpl.getLocationAlongShape)
             * 
             * Anyway, what we're doing is using the blockTrip's getDistanceAlongBlock to 
             * find out what the distance-along the block is for the start of the shape,
             * then we're using our computed distance along shape for the snapped point
             * to find the total distanceAlongBlock.
             */
            final double distanceAlongShape = tripInfo.getDistanceFrom()
                + distTraveledOnLine;
            final double distanceAlongBlock = blockTrip.getDistanceAlongBlock()
                + distanceAlongShape;
            final ScheduledBlockLocation location = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
                instance.getBlock(), distanceAlongBlock);

            if (location == null
                || !observation.getDscImpliedRouteCollections().contains(
                    location.getActiveTrip().getTrip().getRouteCollection().getId()))
              continue;

            final int searchTimeFrom = (int) (timeFrom.getTime() - instance.getServiceDate()) / 1000;
            final int searchTimeTo = (int) (timeTo.getTime() - instance.getServiceDate()) / 1000;

            final int schedTime = location.getScheduledTime();
            if (searchTimeFrom < schedTime && schedTime < searchTimeTo) {
              final BlockTripEntry activeTrip = location.getActiveTrip();
              final String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
              final RunTripEntry rte = _runService.getRunTripEntryForTripAndTime(
                  activeTrip.getTrip(), schedTime);
              final BlockState state = new BlockState(instance, location, rte,
                  dsc);
              blockStates.add(state);

              final BlockLocationKey key = new BlockLocationKey(instance, 0,
                  Double.POSITIVE_INFINITY);
              BestBlockStates currentStates = mb.get(key);
              if (currentStates == null) {
                currentStates = new BestBlockStates(Sets.newHashSet(state));
              } else {
                currentStates.getAllStates().add(state);
              }
              mb.put(key, currentStates);
            }
          }
        }
      }
    }

    m.addAll(blockStates);
    _observationCache.putValueForObservation(observation,
        EObservationCacheKey.BEST_BLOCK_STATES, m);
  }

  private static class TripInfo {
    final double _distanceFrom;
    final double _distanceTo;
    final ShapeIdxAndId _shapeAndIdx;
    final private Collection<BlockEntry> _blocks;

    public TripInfo(double distanceFrom, double distanceTo,
        Collection<BlockEntry> blocks, ShapeIdxAndId shapeIdxAndId) {
      _distanceFrom = distanceFrom;
      _distanceTo = distanceTo;
      _shapeAndIdx = shapeIdxAndId;
      _blocks = blocks;
    }

    public Collection<BlockEntry> getBlocks() {
      return _blocks;
    }

    public double getDistanceFrom() {
      return _distanceFrom;
    }

    public double getDistanceTo() {
      return _distanceTo;
    }

    public ShapeIdxAndId getShapeAndIdx() {
      return _shapeAndIdx;
    }

  };

  private static class ShapeIdxAndId {
    final private AgencyAndId _shapeId;
    final private int _shapePointIndex;

    public ShapeIdxAndId(AgencyAndId shapeId, int shapePointIndex) {
      _shapeId = shapeId;
      _shapePointIndex = shapePointIndex;
    }

    public AgencyAndId getShapeId() {
      return _shapeId;
    }

    public int getShapePointIndex() {
      return _shapePointIndex;
    }

  }

  private Table<AgencyAndId, BlockConfigurationEntry, BlockTripEntry> _shapesAndBlockConfigsToBlockTrips;

  /**
   * 
   * Build the maps and STR tree for look-up.
   * 
   */
  private void buildShapeSpatialIndex() throws IOException,
      ClassNotFoundException {
    try {
      _shapesAndBlockConfigsToBlockTrips = HashBasedTable.create();
      final Multimap<AgencyAndId, BlockEntry> allUniqueShapePoints = HashMultimap.create();
  
      _log.info("generating shapeId & blockConfig to block trips map...");
      for (final BlockEntry blockEntry : _transitGraphDao.getAllBlocks()) {
        for (final BlockConfigurationEntry blockConfig : blockEntry.getConfigurations()) {
          for (final BlockTripEntry blockTrip : blockConfig.getTrips()) {
            final TripEntry trip = blockTrip.getTrip();
            final AgencyAndId shapeId = trip.getShapeId();
            if (shapeId != null) {
              _shapesAndBlockConfigsToBlockTrips.put(shapeId, blockConfig,
                  blockTrip);
              allUniqueShapePoints.put(shapeId, blockEntry);
            }
          }
        }
      }
      
      _linesToTripInfo = HashMultimap.create();
      final GeometryFactory gf = new GeometryFactory();


      _log.info("creating location indexed trip-lines index...");
      _log.info("trips=" + _transitGraphDao.getAllTrips().size());
      for (final TripEntry trip : _transitGraphDao.getAllTrips()) {
        final AgencyAndId shapeId = trip.getShapeId();
        if (shapeId == null) {
          continue;
        }

      }

      final Multimap<ShapePoints, Entry<Envelope, LocationIndexedLine>> shapePointsToLines = HashMultimap.create();
      _log.info("\tshapePoints=" + allUniqueShapePoints.keySet().size());
      for (final Entry<AgencyAndId, Collection<BlockEntry>> shapePointsEntry : allUniqueShapePoints.asMap().entrySet()) {
        
        final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapePointsEntry.getKey());
        if (shapePoints == null || shapePoints.isEmpty()) {
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

          shapePointsToLines.put(shapePoints, Maps.immutableEntry(env, line));
        }
      }

      if (shapePointsToLines.size() > 0) {
        _log.info("\tshapePoint lines=" + shapePointsToLines.size());
        _index = new STRtree(shapePointsToLines.size());
        for (final Entry<ShapePoints, Collection<Entry<Envelope, LocationIndexedLine>>> shapePointsEntry : shapePointsToLines.asMap().entrySet()) {
          for (final Entry<Envelope, LocationIndexedLine> envLine : shapePointsEntry.getValue()) {
            _index.insert(envLine.getKey(), envLine.getValue());
          }
        }
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
  }

}
