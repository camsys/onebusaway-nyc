package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;
import org.onebusaway.transit_data_federation.services.blocks.BlockLayoverIndex;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.FrequencyBlockTripIndex;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opentrackingtools.graph.impl.GenericJTSGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

@Component
public class ObaTrackingGraph extends GenericJTSGraph {

  private final Logger _log = LoggerFactory.getLogger(ObaTrackingGraph.class);
  
  @Autowired
  private BlockCalendarService _blockCalendarService;

  @Autowired
  private BlockIndexService _blockIndexService;

  @Autowired
  private TransitGraphDao _transitGraphDao;
  
  @Autowired
  private ShapePointService _shapePointService;
  
  private static class TripInfo {
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

    public AgencyAndId getShapeId() {
      return _shapeId;
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
  };

  @PostConstruct
  @Refreshable(dependsOn = {
      RefreshableResources.TRANSIT_GRAPH, 
      RefreshableResources.NARRATIVE_DATA})
  public void setup() throws IOException, ClassNotFoundException {
    buildGraph();
  }
  
  private void buildGraph() {
    try {

      final Multimap<AgencyAndId, AgencyAndId> allUniqueShapePointsToBlockId = HashMultimap.create();
      final Multimap<AgencyAndId, BlockTripIndex> blockTripIndicesByShapeId = HashMultimap.create();
      final Multimap<AgencyAndId, BlockLayoverIndex> blockLayoverIndicesByShapeId = HashMultimap.create();
      final Multimap<AgencyAndId, FrequencyBlockTripIndex> frequencyBlockTripIndicesByShapeId = HashMultimap.create();

      final GeometryFactory gf = new GeometryFactory();

      _log.info("generating shapeId & blockConfig to block trips map...");
      for (final BlockEntry blockEntry : _transitGraphDao.getAllBlocks()) {
        for (final BlockConfigurationEntry blockConfig : blockEntry.getConfigurations()) {
          for (final BlockTripEntry blockTrip : blockConfig.getTrips()) {
            final TripEntry trip = blockTrip.getTrip();
            final AgencyAndId shapeId = trip.getShapeId(); 
        	final AgencyAndId blockId = blockEntry.getId();
            
            if (shapeId != null) {            	
            	allUniqueShapePointsToBlockId.put(shapeId, blockId);
            	blockTripIndicesByShapeId.putAll(shapeId, _blockIndexService.getBlockTripIndicesForBlock(blockId));
            	blockLayoverIndicesByShapeId.putAll(shapeId, _blockIndexService.getBlockLayoverIndicesForBlock(blockId));
            	frequencyBlockTripIndicesByShapeId.putAll(shapeId, _blockIndexService.getFrequencyBlockTripIndicesForBlock(blockId));
            }
          }
        }
      }

      _log.info("\tshapePoints=" + allUniqueShapePointsToBlockId.keySet().size());
      
      final List<Geometry> geoms = Lists.newArrayList();
      
      for (final Entry<AgencyAndId, Collection<AgencyAndId>> shapePointsEntry : allUniqueShapePointsToBlockId.asMap().entrySet()) {
        final AgencyAndId shapeId = shapePointsEntry.getKey();
        final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);        
        if (shapePoints == null || shapePoints.isEmpty()) {
          _log.warn("blocks with no shapes: " + shapePointsEntry.getValue());
          continue;
        }

        final Collection<BlockTripIndex> indices = blockTripIndicesByShapeId.get(shapeId);
  	  	final Collection<BlockLayoverIndex> layoverIndices = blockLayoverIndicesByShapeId.get(shapeId);
  	  	final Collection<FrequencyBlockTripIndex> frequencyIndices = frequencyBlockTripIndicesByShapeId.get(shapeId);
                
        final Collection<AgencyAndId> blockIds = shapePointsEntry.getValue();
  	  	if(blockIds.isEmpty()) 
  	  		continue;
  	  	
  	  	List<Coordinate> coords = Lists.newArrayList();
        for (int i = 0; i < shapePoints.getSize() - 1; ++i) {
          final CoordinatePoint from = shapePoints.getPointForIndex(i);
          final CoordinatePoint to = shapePoints.getPointForIndex(i + 1);

          final Coordinate fromJts = new Coordinate(from.getLon(), from.getLat());
          final Coordinate toJts = new Coordinate(to.getLon(), to.getLat());
          
          coords.add(fromJts);
          coords.add(toJts);
        }
        
        final Geometry lineGeo = gf.createLineString(coords.toArray(new Coordinate[coords.size()]));
        lineGeo.setUserData(new TripInfo(shapeId, indices, layoverIndices, frequencyIndices));
        
        geoms.add(lineGeo);
      }

      /*
       * Merge the lines at intersections.
       */
      Geometry superGeom  = JTSFactoryFinder.getGeometryFactory().buildGeometry(geoms).union();
      
      List<LineString> edges = Lists.newArrayList();
      for (int i = 0; i < ((MultiLineString)superGeom).getNumGeometries(); i++) {
        if (superGeom.getGeometryN(i) instanceof LineString) {
          LineString geom = (LineString) superGeom.getGeometryN(i);
          if (geom.getLength() > 1e-5) {
            edges.add(geom);
            edges.add((LineString)geom.reverse());
          }
        }
      }
      
      this.createGraphFromLineStrings(edges);
      
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
    
    _log.info("done.");
  }

  private ObaTrackingGraph() {
  }

}
