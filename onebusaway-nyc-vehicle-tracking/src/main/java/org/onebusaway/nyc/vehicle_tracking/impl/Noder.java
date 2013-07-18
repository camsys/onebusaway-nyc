package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentIntersector;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.noding.SegmentStringDissolver.SegmentStringMerger;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

/**
 * 
 * @author julianjray
 *
 */
public class Noder {

	private static final Logger logger = Logger.getLogger(Noder.class.getName());

	// The geometry factory must have a precision model that is appropriate for the 
	// scale of the geometries
	private GeometryFactory geometryFactory;
	
	private List<Geometry> subgraph;
	
	private SegmentIntersector segmentIntersector;

	private SegmentStringMerger segmentStringMerger;
	
	/**
	 * 
	 * @param geomList
	 * @param geometryFactory
	 */
	public Noder(List<LineString> geomList, GeometryFactory geometryFactory) {
		this.geometryFactory = geometryFactory;
		this.subgraph = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			Geometry newGeom = geometryFactory.createGeometry(geom);
			newGeom.setUserData(geom.getUserData());
			this.subgraph.add(newGeom);
		}
	}

	public SegmentIntersector getSegmentIntersector() {
		return segmentIntersector;
	}

	public void setSegmentIntersector(SegmentIntersector segmentIntersector) {
		this.segmentIntersector = segmentIntersector;
	}
	
	public SegmentStringMerger getSegmentStringMerger() {
		return segmentStringMerger;
	}

	public void setSegmentStringMerger(SegmentStringMerger segmentStringMerger) {
		this.segmentStringMerger = segmentStringMerger;
	}

	/***
	 * 
	 * @param subgraph
	 * @return
	 */
	public List<Geometry> getNodedLineStrings(boolean mergeLines) {

	    logger.debug("    Processing " + subgraph.size() + " lines.");

	    final LineIntersector li = new RobustLineIntersector();
	    li.setPrecisionModel(geometryFactory.getPrecisionModel());

	    final MCIndexNoder noder = new MCIndexNoder();
	    if (segmentIntersector != null) {
	    	noder.setSegmentIntersector(segmentIntersector);
	    } else {
	    	noder.setSegmentIntersector(new IntersectionAdder(li));
	    }
	    
	    // Convert the line strings to noded segment strings so the noder can do its work
	    List<NodedSegmentString> nodedSegmentStringList = new ArrayList<NodedSegmentString>();
	    for (Geometry geom : subgraph) {
	    	final NodedSegmentString nodedSegmentString = new NodedSegmentString(geom.getCoordinates(), null); 
	    	nodedSegmentString.setData(geom.getUserData());
	    	nodedSegmentStringList.add(nodedSegmentString);
	    }
	    
	    // Compute all nodes
	    logger.debug("    Calculating nodes");
	    noder.computeNodes(nodedSegmentStringList);
		
	    logger.debug("    Removing duplicate lines. There are " + noder.getNodedSubstrings().size() + " lines to process.");
	    SegmentStringDissolver dissolver = null;	      
	    if (segmentStringMerger != null) {
	    	logger.debug("    Using custom merger.");
	    	dissolver = new SegmentStringDissolver(segmentStringMerger);
	    } else {
	    	dissolver = new SegmentStringDissolver();
	    }
	    dissolver.dissolve(noder.getNodedSubstrings());

	    List<Geometry> rawShapes = new ArrayList<Geometry>();
	    
	    for (final Object obj : dissolver.getDissolved()) {
	    	final NodedSegmentString segment = (NodedSegmentString) obj;
	        if (segment.size() <= 1) {
	        	continue;
	        }
	        final LineString lineString = geometryFactory.createLineString(segment.getCoordinates());
			// Make sure it is not degenerate
			Coordinate[] coords = CoordinateArrays.removeRepeatedPoints(lineString.getCoordinates());
			if (coords.length > 1) {
		        Object userData = segment.getData();
		        lineString.setUserData(userData);
	        	rawShapes.add(lineString);
			} else {
				logger.debug("    Found degenerate line.");					
			}	        
	    }
		logger.debug("    Found " + rawShapes.size() + " dissolved lines");

		if (mergeLines) {
			List<Geometry> finalShapes = new ArrayList<Geometry>();

			LineMerger lineMerger = new LineMerger();
		    try {
		    	lineMerger.add(rawShapes);
		    } catch (Exception e) {
		    	e.printStackTrace();
		    	logger.warn("Error proceessing geometries: " + e.getMessage());
		    	for (Geometry geom : rawShapes) {
		    		logger.warn(geom);
		    	}
		    }
		    for (Object obj : lineMerger.getMergedLineStrings()) {
				Geometry geom = (Geometry) obj;
				finalShapes.add(geom);	    	
		    }
			logger.debug("    Found " + finalShapes.size() + " merged lines");
			
			return finalShapes;
		} else {
			return rawShapes;
		}
	}
}
