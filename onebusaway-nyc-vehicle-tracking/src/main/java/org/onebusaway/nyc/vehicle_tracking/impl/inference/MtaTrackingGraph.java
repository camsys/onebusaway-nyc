package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.DscLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.GpsLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.NullStateLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.RunLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.RunTransitionLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MtaPathStateBelief;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.blocks.ServiceIntervalBlock;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import gov.sandia.cognition.statistics.bayesian.BayesianCredibleInterval;
import gov.sandia.cognition.statistics.distribution.UnivariateGaussian;
import gov.sandia.cognition.util.DefaultPair;
import gov.sandia.cognition.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.graph.build.line.DirectedLineStringGraphGenerator;
import org.geotools.graph.structure.basic.BasicDirectedEdge;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opentrackingtools.GpsObservation;
import org.opentrackingtools.graph.edges.InferredEdge;
import org.opentrackingtools.graph.impl.GenericJTSGraph;
import org.opentrackingtools.graph.paths.InferredPath;
import org.opentrackingtools.graph.paths.edges.PathEdge;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.statistics.distributions.impl.OnOffEdgeTransDirMulti;
import org.opentrackingtools.statistics.filters.vehicles.road.impl.AbstractRoadTrackingFilter;
import org.opentrackingtools.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;

@Component
public class MtaTrackingGraph extends GenericJTSGraph {

  private static final long _tripSearchTimeAfterLastStop = 5 * 60 * 60 * 1000;

  private static final long _tripSearchTimeBeforeFirstStop = 5 * 60 * 60 * 1000;

  private final Logger _log = LoggerFactory.getLogger(MtaTrackingGraph.class);
  
  public ScheduleLikelihood schedLikelihood = new ScheduleLikelihood();
  
  public RunTransitionLikelihood runTransitionLikelihood = new RunTransitionLikelihood();
  
  public RunLikelihood runLikelihood = new RunLikelihood();
  
  @Autowired
  public DscLikelihood dscLikelihood;
  
  @Autowired
  public NullStateLikelihood nullStateLikelihood;

  @Autowired
  private JourneyStateTransitionModel _journeyStateTransitionModel;
  
  @Autowired
  private BlocksFromObservationService _blocksFromObservationService;
  
  @Autowired
  private BlockStateService _blockStateService;
  
  @Autowired
  private ExtendedCalendarService _calendarService;
  
  @Autowired
  private BlockCalendarService _blockCalendarService;

  @Autowired
  private BlockIndexService _blockIndexService;

  @Autowired
  private TransitGraphDao _transitGraphDao;

  @Autowired
  private ShapePointService _shapePointService;

  public static class TripInfo {
    final private Collection<BlockTripIndex> _indices;
    final private Collection<BlockLayoverIndex> _layoverIndices;
    final private Collection<FrequencyBlockTripIndex> _frequencyIndices;
    final private AgencyAndId _shapeId;

    public TripInfo(AgencyAndId shapeId, Collection<BlockTripIndex> indices,
        Collection<BlockLayoverIndex> layoverIndices,
        Collection<FrequencyBlockTripIndex> frequencyIndices) {
      _shapeId = shapeId;
      _indices = indices;
      _layoverIndices = layoverIndices;
      _frequencyIndices = frequencyIndices;
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

    public AgencyAndId getShapeId() {
      return _shapeId;
    }
    
  };

  @PostConstruct
  @Refreshable(dependsOn = {
      RefreshableResources.TRANSIT_GRAPH, RefreshableResources.NARRATIVE_DATA})
  public void setup() throws IOException, ClassNotFoundException {
    this.edgeIndex = new STRtree();
    this.graphGenerator = new DirectedLineStringGraphGenerator();
    buildGraph();
  }

  final private BiMap<Geometry, String> _geometryIdBiMap = HashBiMap.create();

  final private Map<Geometry, TripInfo> _geometryToTripInfo = Maps.newHashMap();

  private Random rng;
  
  private void buildGraph() {
    try {
      _geometryIdBiMap.clear();
      _geometryToTripInfo.clear();

      final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();

      _log.info("generating shapeId & blockConfig to block trips map...");
      for (final BlockEntry blockEntry : _transitGraphDao.getAllBlocks()) {
        for (final BlockConfigurationEntry blockConfig : blockEntry.getConfigurations()) {
          for (final BlockTripEntry blockTrip : blockConfig.getTrips()) {
            final TripEntry trip = blockTrip.getTrip();
            final AgencyAndId shapeId = trip.getShapeId();
            final AgencyAndId blockId = blockEntry.getId();

            if (shapeId != null) {
//              if (shapeId.toString().equals("MTA_BXM20075")) {
//                System.out.println(trip.toString() + ", " + blockId.toString());
//              }
              final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);
              if (shapePoints == null || shapePoints.isEmpty()) {
                _log.warn("block with no shapes: " + blockId);
                continue;
              }
      
              if (!blockId.hasValues()) {
                _log.warn("shape with null block id: " + blockId);
                continue;
              }
      
              final List<Coordinate> coords = Lists.newArrayList();
              for (int i = 0; i < shapePoints.getSize(); ++i) {
                final CoordinatePoint next = shapePoints.getPointForIndex(i);
                final Coordinate nextJts = new Coordinate(next.getLat(), next.getLon());
      
                if (coords.size() == 0 || !nextJts.equals2D(coords.get(coords.size() - 1))) {
                  coords.add(nextJts);
                }
              }
              
              if (coords.isEmpty()) {
                _log.warn("shape with no length found: " + shapeId);
                continue;
              }
      
              final Geometry lineGeo = gf.createLineString(coords.toArray(new Coordinate[coords.size()]));
              
              _geometryIdBiMap.put(lineGeo, trip.getId().toString());
              
              if (!_geometryToTripInfo.containsKey(lineGeo)) {
                _geometryToTripInfo.put(lineGeo,
                    new TripInfo(shapeId, 
                        Lists.newArrayList(_blockIndexService.getBlockTripIndicesForBlock(blockId)),
                        Lists.newArrayList(_blockIndexService.getBlockLayoverIndicesForBlock(blockId)),
                        Lists.newArrayList(_blockIndexService.getFrequencyBlockTripIndicesForBlock(blockId))
                        ));
              } else {
                final TripInfo tripInfo = _geometryToTripInfo.get(lineGeo);
                tripInfo.getFrequencyIndices().addAll(_blockIndexService.getFrequencyBlockTripIndicesForBlock(blockId));
                tripInfo.getIndices().addAll(_blockIndexService.getBlockTripIndicesForBlock(blockId));
                tripInfo.getLayoverIndices().addAll(_blockIndexService.getBlockLayoverIndicesForBlock(blockId));
              }
            }
          }
        }
      }

      _log.info("\tshapePoints=" + _geometryToTripInfo.keySet().size());

      final List<LineString> geoms = Lists.newArrayList();

      for (final Entry<Geometry, TripInfo> entry : _geometryToTripInfo.entrySet()) {
        geoms.add((LineString)entry.getKey());
      }

      this.createGraphFromLineStrings(geoms);

    } catch (final Exception ex) {
      ex.printStackTrace();
    }

    _log.info("done.");
  }

  private MtaTrackingGraph() {
  }

  @Override
  public PathEdge getPathEdge(InferredEdge edge, double d, Boolean b) {
    return MtaPathEdge.getEdge(edge, d, b);
  }

  @Override
  public InferredPath getInferredPath(PathEdge pathEdge) {
    return MtaInferredPath.getInferredPath(pathEdge);
  }

  @Override
  public InferredPath getInferredPath(List<PathEdge> currentPath, Boolean b) {
    return MtaInferredPath.getInferredPath(currentPath, b);
  }

  @Override
  public Set<InferredPath> getPaths(VehicleState fromState, GpsObservation obs) {
    return super.getPaths(fromState, obs);
  }

  @Override
  public InferredEdge getNullInferredEdge() {
    return super.getNullInferredEdge();
  }

  @Override
  public InferredPath getNullPath() {
    return MtaInferredPath.getNullPath();
  }

  @Override
  public PathEdge getNullPathEdge() {
    return MtaPathEdge.getNullPathEdge();
  }

  @Override
  public VehicleState createVehicleState(GpsObservation obs,
      AbstractRoadTrackingFilter trackingFilter,
      PathStateBelief pathStateBelief, OnOffEdgeTransDirMulti edgeTransDist,
      VehicleState parent) {
    
    Preconditions.checkState((obs instanceof Observation));
    Preconditions.checkState((pathStateBelief instanceof MtaPathStateBelief));
    Preconditions.checkState(parent == null || (parent instanceof MtaVehicleState));
    
    MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief) pathStateBelief;
    final org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState
      oldTypeVehicleState; 
    if (mtaPathStateBelief.getBlockState() != null) {
      oldTypeVehicleState = createOldTypeVehicleState((Observation)obs, 
            mtaPathStateBelief, (MtaVehicleState) parent);
    } else {
      oldTypeVehicleState = null;
    }
    
    return new MtaVehicleState(this, obs, trackingFilter,
        pathStateBelief, edgeTransDist, parent, oldTypeVehicleState);
  }
    
  public org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState
    createOldTypeVehicleState(Observation mtaObs, MtaPathStateBelief pathStateBelief, 
        MtaVehicleState mtaParentState) {
    /*
     * Here are some hacks that enable us to use the old logic
     */
    try {

      final boolean vehicleHasNotMoved;
      
      final double velocityAvg = 
          AbstractRoadTrackingFilter.getVg().times(
          pathStateBelief.getGroundState()).norm2();
      final double velocityVar =
          AbstractRoadTrackingFilter.getVg().times(
              pathStateBelief.getGroundBelief().getCovariance()).times(
                  AbstractRoadTrackingFilter.getVg().transpose()).normFrobenius();
      
//      new UnivariateGaussian(velocityAvg, velocityVar).getCDF().evaluate())
      /*
       * If we could be stopped, then don't update this
       */
      if (rng.nextDouble() 
          < Math.min(1d, Math.max(0d, 
              1d - FoldedNormalDist.cdf(0d, Math.sqrt(velocityVar), velocityAvg))))
        vehicleHasNotMoved = true;
      else
        vehicleHasNotMoved = false;
      
      final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief)pathStateBelief;
      final BlockState blockState = mtaPathStateBelief.getBlockState();//getBlockState(mtaPathStateBelief);
      final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(blockState, 
          mtaObs);
      final BlockStateObservation blockStateObs;
      if (blockState != null) {
        blockStateObs = new BlockStateObservation(
          blockState, mtaObs, isAtPotentialLayoverSpot,
          pathStateBelief.isOnRoad());
      } else {
        blockStateObs = null;
      }
      Coordinate latLonCoord = GeoUtils.convertToLatLon(
              AbstractRoadTrackingFilter.getOg().times(pathStateBelief.getGroundState()), 
              mtaObs.getObsProjected().getTransform());
      final MotionState motionState = new MotionState(mtaObs.getTime(), 
          new CoordinatePoint(latLonCoord.x, latLonCoord.y), vehicleHasNotMoved);
      final org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState oldTypeParent;
      
      if (mtaParentState != null) {
        oldTypeParent = mtaParentState.getOldTypeVehicleState();
      } else {
        oldTypeParent = null;
      }
      
      final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
          blockStateObs, oldTypeParent, mtaObs, vehicleHasNotMoved); 
      
      return new org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState(
          motionState, blockStateObs, journeyState, null, mtaObs);
      
    } catch (NoninvertibleTransformException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (TransformException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return null;
  }

//  private BlockState getBlockState(MtaPathStateBelief mtaPathStateBelief) {
//    final MtaPathEdge mtaPathEdge = (MtaPathEdge) mtaPathStateBelief.getEdge();
//    final BlockTripEntry blockTripEntry = mtaPathEdge.getBlockTripEntry();
//	  final double distanceAlongBlock = blockTripEntry.getDistanceAlongBlock() 
//			  + mtaPathStateBelief.getGlobalState().getElement(0);
//    
//    return _blockStateService.getAsState(_blockCalendarService.getBlockInstance(
//        blockTripEntry.getBlockConfiguration().getBlock().getId(),
//        mtaPathEdge.getServiceDate()), distanceAlongBlock);
//  }

  public Collection<BlockInstance> getBlockInstances(InferredEdge inferredEdge, GpsObservation obs) {
    final TripInfo tripInfo = getTripInfo(inferredEdge);
    final long time = obs.getTimestamp().getTime();       
    final Date timeFrom = new Date(time - _tripSearchTimeAfterLastStop);
    final Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);
	  List<BlockInstance> instances = 
			  _blockCalendarService.getActiveBlocksInTimeRange(tripInfo.getIndices(), tripInfo.getLayoverIndices(), 
			      tripInfo.getFrequencyIndices(), 
					  timeFrom.getTime(), timeTo.getTime());
	  return instances;
  }

  public TripInfo getTripInfo(InferredEdge inferredEdge) {
    if (inferredEdge.isNullEdge())
      return null;
    BasicDirectedEdge edge = (BasicDirectedEdge) inferredEdge.getBackingEdge();
    LineString edgeGeom = (LineString)edge.getObject();
    final TripInfo tripInfo = this._geometryToTripInfo.get(edgeGeom.getUserData());
    return tripInfo;
  }

  public BlockCalendarService getBlockCalendarService() {
    return _blockCalendarService;
  }

  /**
   * Copied from BlockCalendarService
   * @param index
   * @param timeFrom
   * @param timeTo
   * @param instances
   * @return
   */
  public List<Pair<BlockTripEntry, Date>> getActiveBlockTripEntries(BlockTripIndex index,
      Date timeFrom, Date timeTo) {

    List<BlockTripEntry> trips = index.getTrips();

    ServiceIntervalBlock serviceIntervalBlock = index.getServiceIntervalBlock();
    ServiceInterval serviceInterval = serviceIntervalBlock.getRange();

    Collection<Date> serviceDates = _calendarService.getServiceDatesWithinRange(
        index.getServiceIds(), serviceInterval, timeFrom, timeTo);

    List<Pair<BlockTripEntry, Date>> blockTrips = Lists.newArrayList();
    for (Date serviceDate : serviceDates) {

      findBlockTripsInRange(serviceIntervalBlock, serviceDate, timeFrom,
          timeTo, trips, blockTrips);
    }

    return blockTrips;
  }

  /**
   * 
   * Copied from BlockCalendarService
   * 
   * @param intervals
   * @param serviceDate
   * @param timeFrom
   * @param timeTo
   * @param trips
   * @param instances
   */
  private void findBlockTripsInRange(ServiceIntervalBlock intervals,
      Date serviceDate, Date timeFrom, Date timeTo, List<BlockTripEntry> trips,
      Collection<Pair<BlockTripEntry, Date>> blockTrips) {

    int scheduledTimeFrom = (int) ((timeFrom.getTime() - serviceDate.getTime()) / 1000);
    int scheduledTimeTo = (int) ((timeTo.getTime() - serviceDate.getTime()) / 1000);

    int indexFrom = index(Arrays.binarySearch(intervals.getMaxDepartures(),
        scheduledTimeFrom));
    int indexTo = index(Arrays.binarySearch(intervals.getMinArrivals(),
        scheduledTimeTo));
    

    for (int in = indexFrom; in < indexTo; in++) {
      BlockTripEntry trip = trips.get(in);
      blockTrips.add(DefaultPair.create(trip, serviceDate));
    }
  }

  /**
   * 
   * Copied from BlockCalendarService
   * 
   * @param index
   * @return
   */
  private int index(int index) {
    if (index < 0)
      return -(index + 1);
    return index;
  }

  public BlockState getBlockState(BlockInstance instance,
      double distanceAlongBlock) {
    return _blockStateService.getAsState(instance, distanceAlongBlock);
  }
  
  public Collection<BlockStateObservation> getBlockStateObs(Observation obs) {
    return _blocksFromObservationService.determinePotentialBlockStatesForObservation(obs);
  }

  public ScheduleLikelihood getSchedLikelihood() {
    return schedLikelihood;
  }

  public RunLikelihood getRunLikelihood() {
    return runLikelihood;
  }

  public RunTransitionLikelihood getRunTransitionLikelihood() {
    return runTransitionLikelihood;
  }

  public DscLikelihood getDscLikelihood() {
    return dscLikelihood;
  }
  
  public NullStateLikelihood getNullStateLikelihood() {
    return nullStateLikelihood;
  }

  public Random getRng() {
    return rng;
  }

  public void setRng(Random rng) {
    this.rng = rng;
  }

  public BlocksFromObservationService getBlocksFromObservationService() {
    return _blocksFromObservationService;
  }

  public BlockStateService getBlockStateService() {
    return _blockStateService;
  }

  public ExtendedCalendarService getCalendarService() {
    return _calendarService;
  }

  public BlockIndexService getBlockIndexService() {
    return _blockIndexService;
  }

  public BiMap<Geometry, String> getGeometryIdBiMap() {
    return _geometryIdBiMap;
  }

  public Map<Geometry, TripInfo> getGeometryToTripInfo() {
    return _geometryToTripInfo;
  }
}
