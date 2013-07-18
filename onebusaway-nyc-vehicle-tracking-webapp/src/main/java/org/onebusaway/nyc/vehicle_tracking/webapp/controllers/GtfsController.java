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
import org.onebusaway.nyc.vehicle_tracking.impl.Noder;
import org.onebusaway.nyc.vehicle_tracking.impl.TransformService;
import org.onebusaway.nyc.vehicle_tracking.model.UserData;
import org.onebusaway.nyc.vehicle_tracking.utility.GeoJsonBuilder;
import org.onebusaway.nyc.vehicle_tracking.webapp.utils.CustomIntersectionAdder;
import org.onebusaway.nyc.vehicle_tracking.webapp.utils.CustomSegmentStringDissolver;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.noding.SegmentStringDissolver.SegmentStringMerger;

@Controller
public class GtfsController {

	private static final Logger logger = Logger.getLogger(GtfsController.class.getName());

	private PrecisionModel geographicPrecisionModel = new PrecisionModel(PrecisionModel.FLOATING);
	private PrecisionModel projectedPrecisionModel = new PrecisionModel(0.1);
	
	@Autowired
	private TransitGraphDao _transitGraphDao;

	@Autowired
	private ShapePointService _shapePointService;
	
	@Autowired
	private TransformService transformService;
	
	private String[] shapes_failing_validation = {
	               "M020153",
	               "M040216",
	               "M040217",
	               "M040218",
	               "M040224",
	               "M040226",
	               "M040229",
	               "M040230",
	               "M040232",
	               "M040233",
	               "M040235",
	               "M050188",
	               "M050203",
	               "M050204",
	               "M050205",
	               "M050206",
	               "M050207",
	               "M050208",
	               "M050209",
	               "M050210",
	               "M070073",
	               "M090081",
	               "M090083",
	               "M100056",
	               "M100057",
	               "M1040069",
	               "M1040070",
	               "M110108",
	               "M1160054",
	               "M14A0003",
	               "M200063",
	               "M200064",
	               "M230016",
	               "M340054",
	               "M350054",
	               "M350055",
	               "M600073",
	               "M860079",
	               "M860080",
	               "M980150",
	               "M980155"
		};
			
	
	@RequestMapping("/map.do")
	public ModelAndView getMap(@RequestParam(required = false, defaultValue = "false") boolean useCustomNoder) {
		logger.info("In getMap useCustomNoder = " + useCustomNoder);
		
		List<String> shapeIds = new ArrayList<String>();
		for (LineString shape : getShapeList()) {
			UserData userData = (UserData) shape.getUserData();
			shapeIds.add((String) userData.getValue(UserData.SHAPE_ID));
		}
	    Map<String, Object> model = new HashMap<String, Object>();
	    model.put("map_center_x", 40.639228);
	    model.put("map_center_y", -74.081154);
	    model.put("use_custom_noder", useCustomNoder);
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
		String json = GeoJsonBuilder.convertPoints(getNodes(getShapeList(), geographicPrecisionModel));
		return json;
	}
	@RequestMapping("/intersected-nodes.do")
	public @JsonRawValue @ResponseBody String getIntersectedNodes() {

		logger.debug("in getRawShapeNodes()");

		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertPoints(getNodes(transformService.unprojectShapes(simplifyShapes(getShapeList())), geographicPrecisionModel));
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
	public @JsonRawValue @ResponseBody String getFinalShapes(@RequestParam(required = false, defaultValue = "false") boolean useCustomNoder) {
		logger.info("In getFinalShapes() useCustomNoder = " + useCustomNoder);

		List<LineString> finalShapes = transformService.unprojectShapes(rationalizeShapes(simplifyShapes(getShapeList()), useCustomNoder));
		// Node them
		getNodes(finalShapes, geographicPrecisionModel);
		
		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertLineStrings(finalShapes);
		return json;
	}
	@RequestMapping("/final-nodes.do")
	public @JsonRawValue @ResponseBody String getFinalNodes(@RequestParam(required = false, defaultValue = "false") boolean useCustomNoder) {

		logger.info("In getFinalNodes() useCustomNoder = " + useCustomNoder);

		List<LineString> finalShapes = transformService.unprojectShapes(rationalizeShapes(simplifyShapes(getShapeList()),useCustomNoder));

		// Convert the raw geoms to GeoJson
		String json = GeoJsonBuilder.convertPoints(getNodes(finalShapes, geographicPrecisionModel));
		return json;
	}
	private List<LineString> rationalizeShapes(List<LineString> projectedShapeList, boolean useCustomNoder) {

		GeometryFactory geometryFactory = new GeometryFactory(projectedPrecisionModel);
		
		//List<LineString> shapes = transformService.unprojectShapes(projectedShapeList);
		logger.info("UseCustomNoder = " + useCustomNoder);

		final LineIntersector li = new RobustLineIntersector();
	    li.setPrecisionModel(projectedPrecisionModel);

	    Noder noder = new Noder(projectedShapeList, geometryFactory);
	    if (useCustomNoder) {
	    	noder.setSegmentIntersector(new CustomIntersectionAdder(li));
	    	noder.setSegmentStringMerger(new CustomSegmentStringDissolver.CustomSegmentStringMerger());
	    }
	    
	    List<LineString> finalShapes = new ArrayList<LineString>();
	    
	    int edgeId = 0;
	    for (final Geometry rawNodedLineString : noder.getNodedLineStrings(true)) {

	    	final LineString lineString = geometryFactory.createLineString(rawNodedLineString.getCoordinates());
	        UserData userData = new UserData();
	        //userData.addProperty(UserData.SHAPE_ID, ((UserData) lineString.getUserData()).getValue(UserData.SHAPE_ID));
	        //userData.addProperty(UserData.LENGTH, ((UserData) lineString.getUserData()).getValue(UserData.LENGTH));
	        //userData.addProperty(UserData.SEGMENT_ID, edgeId++);
	        //userData.addProperty(UserData.SEGMENT_LENGTH, lineString.getLength());
	        //lineString.setUserData(userData);
	        finalShapes.add(lineString);
	    }
	    
	    return finalShapes;
	}
	
	private List<LineString> simplifyShapes(List<LineString> shapeList) {
		
		// Project the raw shapes to UTMs so all precision is now w/r/t meters.
		List<LineString> projectedGeoms = transformService.projectShapes(shapeList);
		List<LineString> simpleGeoms = new ArrayList<LineString>();
		
		logger.info("Before simplfyShape there are " + shapeList.size() + " line strings and " + getNodes(shapeList, projectedPrecisionModel).size() + " nodes.");

		GeometryFactory geometryFactory = new GeometryFactory(projectedPrecisionModel);
		//GeometryCollection geomCollection = geometryFactory.createGeometryCollection(projectedGeoms.toArray(new Geometry[projectedGeoms.size()]));
		//geomCollection = (GeometryCollection) DouglasPeuckerSimplifier.simplify(geomCollection, 5d);	

		//List<LineString> simplifiedGeoms = new ArrayList<LineString>();  
		//for (int i = 0; i < geomCollection.getNumGeometries(); i++) {
	    //      LineString euclidGeo = (LineString) geomCollection.getGeometryN(i);
	    //      euclidGeo.setUserData(projectedGeoms.get(i).getUserData());
	    //      simplifiedGeoms.add(euclidGeo);
		//}

		ArrayList<LineString> elemList = new ArrayList<LineString>();
		for (LineString geom : projectedGeoms) {
			elemList.clear();
			elemList.add(geom);
			Noder noder = new Noder(elemList, geometryFactory);
			
	        double cumulativeLength = 0d;
	        int segmentNumber = 0;
	        for (Geometry rawNodedSubLine : noder.getNodedLineStrings(true)) {
	        	
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
		logger.info("After simplfyShape there are " + simpleGeoms.size() + " line strings and " + getNodes(simpleGeoms, projectedPrecisionModel).size() + " nodes.");
		return simpleGeoms;
	}
	/***
	 * 
	 * @param shapeList
	 * @return
	 */
	private List<Point> getNodes(List<LineString> shapeList, PrecisionModel precisionModel) {

		GeometryFactory geometryFactory = new GeometryFactory(precisionModel);
		
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
			if (userData == null) {
				userData = new UserData();
				lineString.setUserData(userData);
			}
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
	/**
	 * 
	 * @return
	 */
	private List<LineString> getShapeList() {

		GeometryFactory geometryFactory = new GeometryFactory(geographicPrecisionModel);
		
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
			if (shapeId == null) {
				continue;
			}
			if (! is_valid_shape(shapeId.getId())) {
				continue;
			}
			/*
			if (limit++ == 300) {
				break;
			}
			*/
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
	
	private boolean is_valid_shape(String shapeId) {
		for (String s : shapes_failing_validation) {
			if (shapeId.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
