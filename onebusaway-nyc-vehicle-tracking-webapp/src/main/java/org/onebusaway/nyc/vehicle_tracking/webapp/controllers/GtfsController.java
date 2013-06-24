package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.TransformService;
import org.onebusaway.nyc.vehicle_tracking.model.UserData;
import org.onebusaway.nyc.vehicle_tracking.utility.GeoJsonBuilder;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

@Controller
public class GtfsController {

	private static final Logger logger = Logger.getLogger(GtfsController.class.getName());

	@Autowired
	private TransitGraphDao _transitGraphDao;

	@Autowired
	private ShapePointService _shapePointService;
	
	@Autowired
	private TransformService transformService;

	private PrecisionModel geographicPrecisionModel = new PrecisionModel();
	private GeometryFactory geometryFactory = new GeometryFactory(geographicPrecisionModel);
	
	@RequestMapping("/map.do")
	public ModelAndView getMap() {
		logger.debug("In getMap");
		
		List<String> shapeIds = new ArrayList<String>();
		for (LineString shape : getShapeList()) {
			UserData userData = (UserData) shape.getUserData();
			shapeIds.add((String) userData.getValue(UserData.SHAPE_ID));
		}
	    Map<String, Object> model = new HashMap<String, Object>();
	    model.put("map_center_x", 40.639228);
	    model.put("map_center_y", -74.081154);
	    model.put("shapes", shapeIds);
	    return new ModelAndView("map.jspx", model);		
	}

	@RequestMapping("/raw-shapes.do")
	public @JsonRawValue @ResponseBody String getRawShapes() {

		logger.debug("in getRawShapes()");

		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertLineStrings(getShapeList());
		return json;
	}
	@RequestMapping("/raw-nodes.do")
	public @JsonRawValue @ResponseBody String getRawShapeNodes() {

		logger.debug("in getRawShapeNodes()");

		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertPoints(getNodes(getShapeList()));
		return json;
	}
	@RequestMapping("/intersected-nodes.do")
	public @JsonRawValue @ResponseBody String getIntersectedNodes() {

		logger.debug("in getRawShapeNodes()");

		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertPoints(getNodes(transformService.unprojectShapes(simplifyShapes(getShapeList()))));
		return json;
	}
	@RequestMapping("/intersected-shapes.do")
	public @JsonRawValue @ResponseBody String getIntersectedShapes() {

		logger.debug("in getIntersectedShapes()");

		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertLineStrings(transformService.unprojectShapes(simplifyShapes(getShapeList())));
		return json;
	}
	@RequestMapping("/final-shapes.do")
	public @JsonRawValue @ResponseBody String getFinalShapes() {

		logger.debug("in getFinalShapes()");

		List<LineString> finalShapes = transformService.unprojectShapes(rationalizeShapes(simplifyShapes(getShapeList())));
		// Node them
		getNodes(finalShapes);
		
		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertLineStrings(finalShapes);
		return json;
	}
	@RequestMapping("/final-nodes.do")
	public @JsonRawValue @ResponseBody String getFinalNodes() {

		logger.debug("in getFinalNodes()");

		List<LineString> finalShapes = transformService.unprojectShapes(rationalizeShapes(simplifyShapes(getShapeList())));

		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertPoints(getNodes(finalShapes));
		return json;
	}
	private List<LineString> rationalizeShapes(List<LineString> projectedShapeList) {

		//List<LineString> shapes = transformService.unprojectShapes(projectedShapeList);
		
		final LineIntersector li = new RobustLineIntersector();
	    li.setPrecisionModel(geometryFactory.getPrecisionModel());

	    final MCIndexNoder noder = new MCIndexNoder();
	    noder.setSegmentIntersector(new IntersectionAdder(li));

	    // Convert the line strings to noded segment strings so the noder can do its work
	    List<NodedSegmentString> nodedSegmentStringList = new ArrayList<NodedSegmentString>();
	    for (LineString geom : projectedShapeList) {
	    	final NodedSegmentString nodedSegmentString = new NodedSegmentString(geom.getCoordinates(), null); 
	    	nodedSegmentString.setData(geom.getUserData());
	    	nodedSegmentStringList.add(nodedSegmentString);
	    }
	    // Compute all nodes
	    logger.debug("Calculating nodes");
	    noder.computeNodes(nodedSegmentStringList);
		
	    final SegmentStringDissolver dissolver = new SegmentStringDissolver();
	      
	    Collection nodedSegments = noder.getNodedSubstrings();

	    logger.debug("dissolving nodes");
	    dissolver.dissolve(nodedSegments);

	    logger.debug("dissolved lines=" + dissolver.getDissolved().size());
	    List<LineString> finalShapes = new ArrayList<LineString>();
	    
	    int edgeId = 0;
	    for (final Object obj : dissolver.getDissolved()) {
	    	final NodedSegmentString segment = (NodedSegmentString) obj;
	        if (segment.size() <= 1) {
	        	continue;
	        }
	        final LineString lineString = geometryFactory.createLineString(segment.getCoordinates());
	        UserData userData = new UserData();
	        userData.addProperty(UserData.SHAPE_ID, ((UserData) segment.getData()).getValue(UserData.SHAPE_ID));
	        userData.addProperty(UserData.LENGTH, ((UserData) segment.getData()).getValue(UserData.LENGTH));
	        userData.addProperty(UserData.SEGMENT_ID, edgeId++);
	        userData.addProperty(UserData.SEGMENT_LENGTH, lineString.getLength());
	        lineString.setUserData(userData);
	        finalShapes.add(lineString);
	    }
	    
	    return finalShapes;
	}
	
	private List<LineString> simplifyShapes(List<LineString> shapeList) {
		
		// Project the raw shapes
		List<LineString> projectedGeoms = transformService.projectShapes(shapeList);
		List<LineString> simpleGeoms = new ArrayList<LineString>();
		
		logger.debug("Before simplfyShape there are " + shapeList.size() + " line strings and " + getNodes(shapeList).size() + " nodes.");

		GeometryCollection geomCollection = geometryFactory.createGeometryCollection(projectedGeoms.toArray(new Geometry[projectedGeoms.size()]));
		geomCollection = (GeometryCollection) DouglasPeuckerSimplifier.simplify(geomCollection, 5d);	

		List<LineString> simplifiedGeoms = new ArrayList<LineString>();  
		for (int i = 0; i < geomCollection.getNumGeometries(); i++) {
	          LineString euclidGeo = (LineString) geomCollection.getGeometryN(i);
	          euclidGeo.setUserData(projectedGeoms.get(i).getUserData());
	          simplifiedGeoms.add(euclidGeo);
		}

		for (LineString geom : simplifiedGeoms) {
	        double cumulativeLength = 0d;
	        int segmentNumber = 0;

	        final MCIndexNoder gn = new MCIndexNoder();
	        LineIntersector li = new RobustLineIntersector();
	        li.setPrecisionModel(geometryFactory.getPrecisionModel());
	        gn.setSegmentIntersector(new IntersectionAdder(li));

	        gn.computeNodes(Collections.singletonList(new NodedSegmentString(geom.getCoordinates(), null)));
          
	        for (final Object obj : gn.getNodedSubstrings()) {

	            final NodedSegmentString rawNodedSubLine = (NodedSegmentString) obj;

	            if (rawNodedSubLine.getCoordinates().length <= 1) {
	            	continue;
	            }
	            
	            final LineString subLine = geometryFactory.createLineString(rawNodedSubLine.getCoordinates());
	            cumulativeLength += subLine.getLength();

	            UserData userData = new UserData();
	            userData.addProperty(UserData.SHAPE_ID, ((UserData) geom.getUserData()).getValue(UserData.SHAPE_ID));
	            userData.addProperty(UserData.LENGTH, geom.getLength()); // This will now be in meters as it has been projected
	            userData.addProperty(UserData.SEGMENT_ID, segmentNumber++);
	            userData.addProperty(UserData.SEGMENT_LENGTH, subLine.getLength());
	            userData.addProperty(UserData.CUMULATIVE_LENGTH, cumulativeLength);
	            subLine.setUserData(userData);
	            simpleGeoms.add(subLine);	            
	        }
	    }
		logger.debug("After simplfyShape there are " + simpleGeoms.size() + " line strings and " + getNodes(simpleGeoms).size() + " nodes.");
		return simpleGeoms;
	}
	/***
	 * 
	 * @param shapeList
	 * @return
	 */
	private List<Point> getNodes(List<LineString> shapeList) {

		// Get the set of unique end points for all the line strings
		Set<Coordinate> end_points = new HashSet<Coordinate>();
		
		for (LineString lineString : shapeList) {
			end_points.add(lineString.getStartPoint().getCoordinate());
			end_points.add(lineString.getEndPoint().getCoordinate());
		}
		logger.debug("Found " + end_points.size() + " nodes");
		
		// Convert the end points to point geometries
		List<Point> nodes = new ArrayList<Point>();
		
		int nodeid = 0;
		for (Coordinate coord : end_points) {
			Point node = geometryFactory.createPoint(coord);
			UserData userData = new UserData();
			userData.addProperty(UserData.NODE_ID, nodeid++);
			node.setUserData(userData);
			nodes.add(node);
		}
		
		// UPdate the node info on the lines
		for (LineString lineString : shapeList) {
			Point fromNode = findNode(nodes, lineString.getStartPoint().getCoordinate());
			Point toNode = findNode(nodes, lineString.getEndPoint().getCoordinate());
			UserData userData = (UserData) lineString.getUserData();
			userData.addProperty(UserData.FROM_NODE, ((UserData) fromNode.getUserData()).getValue(UserData.NODE_ID)); 
			userData.addProperty(UserData.TO_NODE, ((UserData) toNode.getUserData()).getValue(UserData.NODE_ID)); 
		}
		
		return nodes;
	}
	private Point findNode(List<Point> nodes, Coordinate coord) {
		for (Point node : nodes) {
			if (coord.equals2D(node.getCoordinate())) {
				return node;
			}
		}
		return null;
	}
	private List<LineString> getShapeList() {

		// Get the set of unique agency and ids
		final Set<AgencyAndId> shapeIds = Sets.newHashSet();
		for (final TripEntry trip : _transitGraphDao.getAllTrips()) {
			final AgencyAndId shapeId = trip.getShapeId();
			shapeIds.add(shapeId);
		}
		
		logger.info("Found " + shapeIds.size() + " unique agency/ids");

		List<LineString> geometryList = Lists.newArrayList();

		int limit = 0;
		// Get the shape for each agency and id
		for (final AgencyAndId shapeId : shapeIds) {
			
			if (limit++ == 10) {
				break;
			}
			
			final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);
			if (shapePoints == null || shapePoints.isEmpty()) {
				logger.info("shape with no shapepoints: " + shapeId);
				continue;
			}

			final CoordinateList coords = new CoordinateList();
			for (int i = 0; i < shapePoints.getSize(); ++i) {
				final Coordinate nextCoord = new Coordinate(shapePoints.getLats()[i], shapePoints.getLons()[i]);
				coords.add(nextCoord, false);
			}

			if (coords.isEmpty()) {
				logger.info("shape with no length found: " + shapeId);
				continue;
			}

			final LineString lineString = geometryFactory.createLineString(coords.toCoordinateArray());
			UserData userData = new UserData();
			userData.addProperty(UserData.SHAPE_ID, shapeId.getId());
			userData.addProperty(UserData.LENGTH, lineString.getLength());
			lineString.setUserData(userData);

			geometryList.add(lineString);
		}
		logger.info("Found " + geometryList.size() + " unique line strings");

		return geometryList;
	}
}
