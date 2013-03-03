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
import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geomgraph.Edge;
import com.vividsolutions.jts.geomgraph.Label;
import com.vividsolutions.jts.geomgraph.index.EdgeSetIntersector;
import com.vividsolutions.jts.geomgraph.index.SegmentIntersector;
import com.vividsolutions.jts.geomgraph.index.SimpleMCSweepLineIntersector;
import com.vividsolutions.jts.index.strtree.SIRtree;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.noding.SegmentStringDissolver.SegmentStringMerger;
import com.vividsolutions.jts.operation.overlay.EdgeSetNoder;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

import org.geotools.geometry.jts.JTS;
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
import org.opentrackingtools.graph.paths.edges.impl.MtaPathEdge;
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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
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
    final private Collection<SIRtree> _entries;
    final private AgencyAndId _shapeId;

    public TripInfo(AgencyAndId shapeId, Collection<SIRtree> entries) {
      _shapeId = shapeId;
      _entries = entries;
    }

    public Collection<SIRtree> getEntries() {
      return _entries;
    }

    public AgencyAndId getShapeId() {
      return _shapeId;
    }

    public Set<BlockTripEntryAndDate> getActiveTrips(double timeFrom, double timeTo) {
      Set<BlockTripEntryAndDate> activeTrips = Sets.newHashSet();
      for (SIRtree tree : _entries) {
        activeTrips.addAll((Collection<BlockTripEntryAndDate>)tree.query(timeFrom, timeTo));
      } 
      return activeTrips;
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

//  final private Multimap<LineString, AgencyAndId> _geoToShapeId = HashMultimap.create(); 
//  
//  final private Multimap<AgencyAndId, LineString> _shapeIdToGeo = HashMultimap.create();
//
//  final private Map<LineString, TripInfo> _geometryToTripInfo = Maps.newHashMap();
  final private Multimap<Geometry, AgencyAndId> _geoToShapeId = HashMultimap.create(); 
  
  final private Multimap<AgencyAndId, Geometry> _shapeIdToGeo = HashMultimap.create();

  final private Map<Geometry, TripInfo> _geometryToTripInfo = Maps.newHashMap();
  
  private Random rng;
  
  private void buildGraph() {
    try {
      _shapeIdToGeo.clear();
      _geoToShapeId.clear();
      _geometryToTripInfo.clear();

      final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
      
      final Map<AgencyAndId, NodedSegmentString> shapeIdToSegments = Maps.newHashMap();
      final Map<LineString, NodedSegmentString> lineToSegments = Maps.newHashMap();
      for (TripEntry trip: _transitGraphDao.getAllTrips()) {
        AgencyAndId shapeId = trip.getShapeId();
        final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);
        if (shapePoints == null || shapePoints.isEmpty()) {
          _log.debug("shape with no shapepoints: " + shapeId);
          continue;
        }
      
        final CoordinateList coords = new CoordinateList();
        for (int i = 0; i < shapePoints.getSize(); ++i) {
          final Coordinate nextCoord = new Coordinate(shapePoints.getLats()[i], 
              shapePoints.getLons()[i]);
          coords.add(nextCoord, false);
        }
        
        if (coords.isEmpty()) {
          _log.debug("shape with no length found: " + shapeId);
          continue;
        }

        final Geometry lineGeo = gf.createLineString(coords.toCoordinateArray());
        
        Geometry euclidGeo = JTS.transform(lineGeo, 
            GeoUtils.getTransform(lineGeo.getCoordinate()));
        euclidGeo = DouglasPeuckerSimplifier.simplify(euclidGeo, 5);
        
        final NodedSegmentString segments = new NodedSegmentString(euclidGeo.getCoordinates(), 
            Lists.newArrayList(DefaultPair.create(shapeId, true)));
        
        lineToSegments.put((LineString)euclidGeo, segments);
        shapeIdToSegments.put(shapeId, segments);
        
      }
      _log.info("\tshapePoints=" + lineToSegments.size());
      
      MCIndexNoder noder = new MCIndexNoder();
      noder.setSegmentIntersector(new IntersectionAdder(new RobustLineIntersector()));
      
      _log.info("\tcomputing nodes");
      noder.computeNodes(lineToSegments.values());
      
      SegmentStringMerger merger = new SegmentStringMerger() {
        @Override
        public void merge(SegmentString mergeTarget, SegmentString ssToMerge,
            boolean isSameOrientation) {
          
          List<Pair<AgencyAndId, Boolean>> newPairs = Lists.newArrayList();
          for (Pair<AgencyAndId, Boolean> pair : (List<Pair<AgencyAndId, Boolean>>) ssToMerge.getData()) {
            newPairs.add(DefaultPair.create(pair.getFirst(), isSameOrientation));
          }
            
          List<Pair<AgencyAndId, Boolean>> currentPairs = 
              (List<Pair<AgencyAndId, Boolean>>) mergeTarget.getData();
          currentPairs.addAll(newPairs);
        }
      };
      
      _log.info("\tdissolving nodes");
      SegmentStringDissolver dissolver = new SegmentStringDissolver(merger);
      dissolver.dissolve(noder.getNodedSubstrings());
    
      
      _log.info("\tdissolved lines=" + dissolver.getDissolved().size());
      for (Object obj : dissolver.getDissolved()) {
        final NodedSegmentString segments = (NodedSegmentString) obj;
        final LineString line = gf.createLineString(segments.getCoordinates());
        line.setUserData(Boolean.FALSE);
        for (Pair<AgencyAndId, Boolean> pair : (List<Pair<AgencyAndId, Boolean>>) segments.getData()) {
          /*
           * Mark line as having a reverse
           */
          if (!pair.getSecond())
            line.setUserData(Boolean.TRUE);
          _geoToShapeId.put(line, pair.getFirst());
          _shapeIdToGeo.put(pair.getFirst(), line);
        }
      }
      _log.info("\tresult shapes=" + _geoToShapeId.keySet().size());
      _log.info("\tresult shapeIds=" + _geoToShapeId.size());
      
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
                _log.debug("trip with null block id: " + blockId);
                continue;
              }
              
              Collection<Geometry> shapesForId = _shapeIdToGeo.get(shapeId);
              if (shapesForId.isEmpty()) {
                missingShapeGeoms.add(shapeId);
                continue;
              }
              
              SIRtree tree = buildTimeIndex(Collections.singletonList(blockTrip));
              
              for (Geometry lineGeo : shapesForId) {
                TripInfo tripInfo = _geometryToTripInfo.get(lineGeo);
                if (tripInfo == null) {
                  List<SIRtree> entries = Lists.newArrayList(tree);
                  _geometryToTripInfo.put(lineGeo, new TripInfo(shapeId, entries));
                } else {
                  tripInfo.getEntries().add(tree);
                }
              }
            }
          }
        }
      }

      if (missingShapeGeoms.size() > 0) {
        _log.debug(missingShapeGeoms.size() + " shape(s) with no geom mapping: " + missingShapeGeoms);
      }
      
      _log.info("\ttripInfo=" + _geometryToTripInfo.size());
      
      final List<LineString> geoms = Lists.newArrayList();

      Set<Entry<Geometry, TripInfo>> currentEntries = Sets.newHashSet(_geometryToTripInfo.entrySet());
      for (final Entry<Geometry, TripInfo> entry : currentEntries) {
        geoms.add((LineString)entry.getKey());
        /*
         * Add reverses, if they exist.
         */
        if ((Boolean)entry.getKey().getUserData()) {
          final LineString revLine = (LineString)(entry.getKey().reverse());
          geoms.add(revLine);
          _geometryToTripInfo.put(revLine, entry.getValue());
        }
      }

      this.createGraphFromLineStrings(geoms, false);

    } catch (final Exception ex) {
      ex.printStackTrace();
    }

    _log.info("done.");
  } 
  
  /*
  private void buildGraphTest() {
    try {
      _shapeIdToGeo.clear();
      _geoToShapeId.clear();
      _geometryToTripInfo.clear();

      final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
      
      Map<Geometry, Pair<Edge, AgencyAndId>> geoEdgePairMap = Maps.newHashMap();
      Map<Geometry, Edge> geoEdgeMap = Maps.newHashMap();
      for (TripEntry trip: _transitGraphDao.getAllTrips()) {
        AgencyAndId shapeId = trip.getShapeId();
        final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);
        if (shapePoints == null || shapePoints.isEmpty()) {
//          _log.warn("shape with no shapepoints: " + shapeId);
          continue;
        }
      
        final CoordinateList coords = new CoordinateList();
        for (int i = 0; i < shapePoints.getSize(); ++i) {
          final Coordinate nextCoord = new Coordinate(shapePoints.getLats()[i], 
              shapePoints.getLons()[i]);
          coords.add(nextCoord, false);
        }
        
        if (coords.isEmpty()) {
//          _log.warn("shape with no length found: " + shapeId);
          continue;
        }
        
        final LineString lineGeo = gf.createLineString(coords.toCoordinateArray());
        final Edge edge = new Edge(coords.toCoordinateArray(), new Label(0));
        geoEdgeMap.put(lineGeo, edge);
        geoEdgePairMap.put(lineGeo, DefaultPair.create(edge, shapeId));
      }
      _log.info("\tshapes=" + geoEdgeMap.size());
      
      LineIntersector li = new RobustLineIntersector();
      EdgeSetIntersector esi = new SimpleMCSweepLineIntersector();
      SegmentIntersector si = new SegmentIntersector(li, true, false);
      esi.computeIntersections(Lists.newArrayList(geoEdgeMap.values()), si, false);

      for (Pair<Edge, AgencyAndId> edgePair: geoEdgePairMap.values()) {
        List<Edge> splitEdges = Lists.newArrayList();
        edgePair.getFirst().getEdgeIntersectionList().addSplitEdges(splitEdges);
        AgencyAndId shapeId = edgePair.getSecond();
        
        Preconditions.checkState(!splitEdges.isEmpty());
        
        for (Edge sedge : splitEdges) {
          final LineString lineGeo = gf.createLineString(sedge.getCoordinates());
          _geoToShapeId.put(lineGeo, shapeId);
        }
      }
      
      // Now create the unique exploded geometries to shape map.
      for (Entry<LineString, AgencyAndId> entry : _geoToShapeId.entries()) {
        _shapeIdToGeo.put(entry.getValue(), entry.getKey());
      }
      
      geoEdgePairMap = null;
      geoEdgeMap = null;
      _log.info("\tnodedShapes=" + _geoToShapeId.keySet().size());
      
      Set<AgencyAndId> missingShapeGeoms = Sets.newHashSet();
      _log.info("generating shapeId & blockConfig to block trips map...");
      Set<TripInfo> allTripInfo = Sets.newHashSet();
      Map<AgencyAndId, TripInfo> shapeToTripInfo = Maps.newHashMap();
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
//                _log.warn("trip with null block id: " + blockId);
                continue;
              }
              
              TripInfo tripInfo = shapeToTripInfo.get(shapeId);
              if (tripInfo == null) {
                List<BlockTripEntry> entries = Lists.newArrayList(blockTrip);
                tripInfo = new TripInfo(shapeId, entries);
                shapeToTripInfo.put(shapeId, tripInfo);
                allTripInfo.add(tripInfo);
              } else {
                tripInfo.getEntries().add(blockTrip);
              }
              
              for (LineString line : _shapeIdToGeo.get(shapeId)) {
                _geometryToTripInfo.put(line, tripInfo);
              }
            }
          }
        }
      }
      shapeToTripInfo = null;

      if (missingShapeGeoms.size() > 0) {
        _log.warn(missingShapeGeoms.size() + " shape(s) with no geom mapping: " + missingShapeGeoms);
      }
      missingShapeGeoms = null;
      
      _log.info("\ttripInfo=" + allTripInfo.size());
      
      _log.info("\tbuilding trip time indices...");
      
      int i = 0;
      for (TripInfo info : allTripInfo) {
        if (i % 100 == 0)
          _log.info("\t" + i + "/" + allTripInfo.size());
        info.setTimeIndex(buildTimeIndex(info.getEntries()));
        i++;
      }
      allTripInfo = null;
      
      final List<LineString> geoms = Lists.newArrayList(_geometryToTripInfo.keySet());

      _log.info("\tcreating graph...");
      this.createGraphFromLineStrings(geoms);

    } catch (final Exception ex) {
      ex.printStackTrace();
    }

    _log.info("done.");
  }
  */

  private MtaTrackingGraph() {
  }

  @Override
  public PathEdge getPathEdge(InferredEdge edge, Geometry line, double d, Boolean b) {
    return new MtaPathEdge(edge, line, d, b);
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
    // TODO what's a good value for max nodes?
    SIRtree blockTripTimeIndex = new SIRtree();
    for (BlockTripEntry entry : collection) {
      for (Date serviceDate : _calendarService.getDatesForLocalizedServiceId(entry.getTrip().getServiceId())) {
        final int maxDeparture = Iterables.getFirst(entry.getStopTimes(), null).getStopTime().getDepartureTime();
        final double fromTime = maxDeparture * 1000d + serviceDate.getTime();
        final int minArrival = Iterables.getLast(entry.getStopTimes()).getStopTime().getArrivalTime();
        final double toTime = minArrival * 1000d + serviceDate.getTime();
        blockTripTimeIndex.insert(fromTime, toTime, new BlockTripEntryAndDate(entry, serviceDate));
      }
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

  public ExtendedCalendarService getExtCalendarService() {
    return _extCalendarService;
  }

//  public Map<LineString, TripInfo> getGeometryToTripInfo() {
//    return _geometryToTripInfo;
//  }
//
//  public Multimap<LineString, AgencyAndId> getGeoToShapeId() {
//    return _geoToShapeId;
//  }
//
//  public Multimap<AgencyAndId, LineString> getShapeIdToGeo() {
//    return _shapeIdToGeo;
//  }

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
