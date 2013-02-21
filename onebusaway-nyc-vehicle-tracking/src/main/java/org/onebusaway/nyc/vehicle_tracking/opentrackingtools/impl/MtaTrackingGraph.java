package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.collections.tuple.Tuples;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.gtfs.services.GtfsDao;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlocksFromObservationService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;
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
import org.onebusaway.transit_data_federation.impl.narrative.NarrativeProviderImpl;
import org.onebusaway.transit_data_federation.impl.otp.OBAGraphServiceImpl;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockTripEntryImpl;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.ExtendedCalendarService;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.onebusaway.transit_data_federation.services.blocks.AbstractBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.blocks.ServiceIntervalBlock;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.onebusaway.utility.ObjectSerializationLibrary;

import gov.sandia.cognition.statistics.bayesian.BayesianCredibleInterval;
import gov.sandia.cognition.statistics.distribution.UnivariateGaussian;
import gov.sandia.cognition.util.DefaultPair;
import gov.sandia.cognition.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.SIRtree;
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
import org.opentrackingtools.graph.paths.states.PathState;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.statistics.distributions.impl.OnOffEdgeTransDistribution;
import org.opentrackingtools.statistics.filters.vehicles.road.impl.AbstractRoadTrackingFilter;
import org.opentrackingtools.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

@Component
public class MtaTrackingGraph extends GenericJTSGraph {

  public class BlockTripEntryAndDate {

    final private BlockTripEntry blockTripEntry;
    final private Date serviceDate;

    public BlockTripEntryAndDate(BlockTripEntry entry, Date serviceDate) {
      this.blockTripEntry = entry;
      this.serviceDate = serviceDate;
    }

    public BlockTripEntry getBlockTripEntry() {
      return blockTripEntry;
    }

    public Date getServiceDate() {
      return serviceDate;
    }

  }

  private static final long _tripSearchTimeAfterLastStop = 5 * 60 * 60 * 1000;

  private static final long _tripSearchTimeBeforeFirstStop = 5 * 60 * 60 * 1000;

  private final Logger _log = LoggerFactory.getLogger(MtaTrackingGraph.class);
  
  
  public ScheduleLikelihood schedLikelihood = new ScheduleLikelihood();
  
  public RunTransitionLikelihood runTransitionLikelihood = new RunTransitionLikelihood();
  
  public RunLikelihood runLikelihood = new RunLikelihood();
  
//  @Autowired
//  public OBAGraphServiceImpl obaGraph;

  @Autowired
  public FederatedTransitDataBundle _bundle;
  
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
  private ExtendedCalendarService _extCalendarService;
  
  @Autowired
  private BlockCalendarService _blockCalendarService;

  @Autowired
  private BlockIndexService _blockIndexService;

  @Autowired
  private TransitGraphDao _transitGraphDao;
  
  @Autowired
  private CalendarService _calendarService;

  @Autowired
  private ShapePointService _shapePointService;

  public static class TripInfo {
    final private Collection<BlockTripEntry> _entries;
    final private AgencyAndId _shapeId;
    private SIRtree timeIndex;

    public TripInfo(AgencyAndId shapeId, Collection<BlockTripEntry> entries) {
      _shapeId = shapeId;
      _entries = entries;
    }

    public Collection<BlockTripEntry> getEntries() {
      return _entries;
    }

    public AgencyAndId getShapeId() {
      return _shapeId;
    }

    public void setTimeIndex(SIRtree timeIndex) {
      this.timeIndex = timeIndex;
    }

    public SIRtree getTimeIndex() {
      return timeIndex;
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

  final private Multimap<Geometry, AgencyAndId> _geoToShapeId = HashMultimap.create(); 
  
  final private Map<AgencyAndId, Geometry> _shapeIdToGeo = Maps.newHashMap();

  final private Map<Geometry, TripInfo> _geometryToTripInfo = Maps.newHashMap();

  private Random rng;
  
  private void buildGraph() {
    try {
      _shapeIdToGeo.clear();
      _geoToShapeId.clear();
      _geometryToTripInfo.clear();

      final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
      
      for (TripEntry trip: _transitGraphDao.getAllTrips()) {
        AgencyAndId shapeId = trip.getShapeId();
        final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);
        if (shapePoints == null || shapePoints.isEmpty()) {
          _log.warn("shape with no shapepoints: " + shapeId);
          continue;
        }
      
        final CoordinateList coords = new CoordinateList();
        for (int i = 0; i < shapePoints.getSize(); ++i) {
          final Coordinate nextCoord = new Coordinate(shapePoints.getLats()[i], 
              shapePoints.getLons()[i]);
          coords.add(nextCoord, false);
        }
        
        if (coords.isEmpty()) {
          _log.warn("shape with no length found: " + shapeId);
          continue;
        }

        final Geometry lineGeo = gf.createLineString(coords.toCoordinateArray());
        
        _geoToShapeId.put(lineGeo, shapeId);
        _shapeIdToGeo.put(shapeId, lineGeo);
      }
      _log.info("\tshapePoints=" + _geoToShapeId.size());
      
      Set<AgencyAndId> missingShapeGeoms = Sets.newHashSet();
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
              if (!blockId.hasValues()) {
                _log.warn("trip with null block id: " + blockId);
                continue;
              }
              
              Geometry lineGeo = _shapeIdToGeo.get(shapeId);
              if (lineGeo == null) {
                missingShapeGeoms.add(shapeId);
                continue;
              }
              if (!_geometryToTripInfo.containsKey(lineGeo)) {
                List<BlockTripEntry> entries = Lists.newArrayList(blockTrip);
                _geometryToTripInfo.put(lineGeo, new TripInfo(shapeId, entries));
              } else {
                final TripInfo tripInfo = _geometryToTripInfo.get(lineGeo);
                tripInfo.getEntries().add(blockTrip);
              }
            }
          }
        }
      }

      if (missingShapeGeoms.size() > 0) {
        _log.warn(missingShapeGeoms.size() + " shape(s) with no geom mapping: " + missingShapeGeoms);
      }
      
      _log.info("\ttripInfo=" + _geometryToTripInfo.size());
      
      _log.info("\tbuilding trip time indices=" + _geometryToTripInfo.size());
      
      for (TripInfo info : _geometryToTripInfo.values()) {
        info.setTimeIndex(buildTimeIndex(info.getEntries()));
      }
      

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
      PathStateBelief pathStateBelief, OnOffEdgeTransDistribution edgeTransDist,
      VehicleState parent) {
    
    Preconditions.checkState((obs instanceof Observation));
    Preconditions.checkState((pathStateBelief instanceof MtaPathStateBelief));
    Preconditions.checkState(parent == null || (parent instanceof MtaVehicleState));
    
    MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief) pathStateBelief;
    
    return new MtaVehicleState(this, obs, trackingFilter,
        pathStateBelief, edgeTransDist, parent, mtaPathStateBelief.getRunStateBelief());
  }
  
  public BlockStateObservation getBlockStateObs(@Nonnull Observation obs, @Nonnull PathState pathState, 
     @Nullable BlockTripEntry blockTripEntry, long serviceDate) {
    
    final BlockState blockState;
    
    if (pathState.isOnRoad()) {
      
      Preconditions.checkNotNull(blockTripEntry);
      
      final double distanceAlongBlock = blockTripEntry.getDistanceAlongBlock() 
          + pathState.getGlobalState().getElement(0);
    
      InstanceState instState = new InstanceState(serviceDate);
      BlockInstance instance = new BlockInstance(blockTripEntry.getBlockConfiguration(), 
          instState);
//        _blockCalendarService.getBlockInstance(
//              blockTripEntry.getBlockConfiguration().getBlock().getId(),
//              serviceDate);
        
      blockState = _blockStateService.getAsState(instance, distanceAlongBlock);
    } else {
      blockState = null;
    }
    
    final boolean isAtPotentialLayoverSpot = VehicleStateLibrary.isAtPotentialLayoverSpot(blockState, 
        obs);
    final BlockStateObservation blockStateObs;
    if (blockState != null) {
      blockStateObs = new BlockStateObservation(
        blockState, obs, isAtPotentialLayoverSpot,
        pathState.isOnRoad());
    } else {
      blockStateObs = null;
    }
    return blockStateObs;
  }
  
  public TripInfo getTripInfo(@Nonnull InferredEdge inferredEdge) {
    if (inferredEdge.isNullEdge())
      return null;
    BasicDirectedEdge edge = (BasicDirectedEdge) inferredEdge.getBackingEdge();
    LineString edgeGeom = (LineString)edge.getObject();
    // TODO this null check is temporary.  remove when non-route streets are included.
    final TripInfo tripInfo = Preconditions.checkNotNull(this._geometryToTripInfo.get(edgeGeom.getUserData()));
    return tripInfo;
  }

  public BlockCalendarService getBlockCalendarService() {
    return _blockCalendarService;
  }

  private SIRtree buildTimeIndex(Collection<BlockTripEntry> collection) {
    List<Object[]> intervalTrips = Lists.newArrayList();
    for (BlockTripEntry entry : collection) {
      for (Date serviceDate : _calendarService.getDatesForLocalizedServiceId(entry.getTrip().getServiceId())) {
        final int maxDeparture = Iterables.getFirst(entry.getStopTimes(), null).getStopTime().getDepartureTime();
        final double fromTime = maxDeparture * 1000d + serviceDate.getTime();
        final int minArrival = Iterables.getLast(entry.getStopTimes()).getStopTime().getArrivalTime();
        final double toTime = minArrival * 1000d + serviceDate.getTime();
        intervalTrips.add(new Object[] {new Double(fromTime), new Double(toTime), 
            new BlockTripEntryAndDate(entry, serviceDate)});
      }
    }
    
    // TODO what's a good value?
    final int initialSize = 10; //intervalTrips.size()/3;
    SIRtree blockTripTimeIndex = new SIRtree(initialSize);
    
    for (Object[] objects : intervalTrips) {
      blockTripTimeIndex.insert((Double)objects[0], (Double)objects[1], (BlockTripEntryAndDate)objects[2]);
    }
    
    blockTripTimeIndex.build();
    
    return blockTripTimeIndex;
  }
  
  public BlockState getBlockState(@Nonnull BlockInstance instance,
      double distanceAlongBlock) {
    return _blockStateService.getAsState(instance, distanceAlongBlock);
  }
  
  public Collection<BlockStateObservation> getBlockStatesFromObservation(@Nonnull Observation obs) {
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

  public CalendarService getCalendarService() {
    return _calendarService;
  }

  public BlockIndexService getBlockIndexService() {
    return _blockIndexService;
  }

  public Map<Geometry, TripInfo> getGeometryToTripInfo() {
    return _geometryToTripInfo;
  }

  public ExtendedCalendarService getExtCalendarService() {
    return _extCalendarService;
  }

  public Multimap<Geometry, AgencyAndId> getGeoToShapeId() {
    return _geoToShapeId;
  }

  public Map<AgencyAndId, Geometry> getShapeIdToGeo() {
    return _shapeIdToGeo;
  }

  public org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState createOldTypeVehicleState(
      @Nonnull Observation mtaObs, @Nullable BlockStateObservation blockStateObs, boolean vehicleHasNotMoved,
      @Nullable org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState oldTypeParent) {

    final CoordinatePoint point;
    if (blockStateObs != null) {
      point = blockStateObs.getBlockState().getBlockLocation().getLocation();
    } else {
      point = mtaObs.getLocation();
    }
    final MotionState motionState = new MotionState(mtaObs.getTime(), 
        point, vehicleHasNotMoved);
    
    final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
        blockStateObs, oldTypeParent, mtaObs, vehicleHasNotMoved); 
    
    final org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState newOldTypeState = 
        new org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState(
          motionState, blockStateObs, journeyState, null, mtaObs);
    
    return newOldTypeState;
  }

  public JourneyStateTransitionModel getJourneyStateTransitionModel() {
    return _journeyStateTransitionModel;
  }

}
