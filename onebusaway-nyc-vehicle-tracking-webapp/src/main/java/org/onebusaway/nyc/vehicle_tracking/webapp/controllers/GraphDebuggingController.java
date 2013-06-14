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
package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.util.List;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonRawValue;
import org.codehaus.jackson.annotate.JsonValue;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.shapes.ShapePointService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.opentrackingtools.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.operation.overlay.snap.GeometrySnapper;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

@Controller
public class GraphDebuggingController {
	
	private final Logger _log = LoggerFactory.getLogger(GraphDebuggingController.class);
	
	@Autowired
	private TransitGraphDao _transitGraphDao;
	
	@Autowired
	private ShapePointService _shapePointService;
	
	@RequestMapping("/graph-raw-shapes.do")
	public @JsonRawValue @ResponseBody String getRawShapes() {

		_log.info("in getRawShapes()");

		// Convert the raw geoms to GeoJson
		String json = getGeoJson(getRawGeomList());
		//_log.info(json);
		return json;
	}
	
	private List<LineString> getRawGeomList() {
		
		// Get the set of unique agency and ids
		final Set<AgencyAndId> shapeIds = Sets.newHashSet();
		for (final TripEntry trip : _transitGraphDao.getAllTrips()) {
			final AgencyAndId shapeId = trip.getShapeId();
			shapeIds.add(shapeId);
		}
		_log.info("Found " + shapeIds.size() + " unique agency/ids");
		
		List<LineString> geometryList = Lists.newArrayList();

		int limit = 0;
		// Get the shape for each agency and id
		for (final AgencyAndId shapeId : shapeIds) {
			
			if (limit++ == 10) {
				break;
			}
			
			final ShapePoints shapePoints = _shapePointService.getShapePointsForShapeId(shapeId);
			if (shapePoints == null || shapePoints.isEmpty()) {
				_log.info("shape with no shapepoints: " + shapeId);
				continue;
			}

			final CoordinateList coords = new CoordinateList();
			for (int i = 0; i < shapePoints.getSize(); ++i) {
				final Coordinate nextCoord = new Coordinate(shapePoints.getLons()[i], shapePoints.getLats()[i]);
				coords.add(nextCoord, false);
			}

			if (coords.isEmpty()) {
				_log.info("shape with no length found: " + shapeId);
				continue;
			}

			final LineString lineGeo = JTSFactoryFinder.getGeometryFactory().createLineString(coords.toCoordinateArray());

			lineGeo.setUserData(shapeId);
			
			geometryList.add(lineGeo);
		}
		_log.info("Found " + geometryList.size() + " unique line strings");
		
		return geometryList;
	}

	@RequestMapping("/graph-processed-shapes.do")
	public @JsonRawValue @ResponseBody String getProcessedShapes() {

		final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();		
		List<LineString> geometryList = Lists.newArrayList();

		Coordinate ref = new Coordinate();
		ref.x = 40.639228;
		ref.y = -74.081154;

		for (LineString lineString : getRawGeomList()) {			
			
			try {
				LineString euclidGeo = (LineString) JTS.transform(lineString, GeoUtils.getTransform(ref));
			
				if (!geometryList.isEmpty()) {
					
					GeometrySnapper snapper = new GeometrySnapper(euclidGeo);
					MultiLineString currGeomCollection = gf.createMultiLineString(geometryList.toArray(new LineString[geometryList.size()]));
					euclidGeo = (LineString) snapper.snapTo(currGeomCollection , 10d);
				} 
          
			euclidGeo.setUserData(lineString.getUserData());
			geometryList.add(euclidGeo);
			} catch (Exception e) {
				_log.warn(e.getMessage());
			}
		}
		// Convert the raw geoms to GeoJson
		String json = getGeoJson(geometryList);
		return json;	
	}

	@RequestMapping("/graph-simplified-shapes.do")
	public @JsonRawValue @ResponseBody String getSimplifiedShapes() {

		final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();		
		List<LineString> geometryList = Lists.newArrayList();
		
		Coordinate ref = new Coordinate();
		ref.x = 40.639228;
		ref.y = -74.081154;
		
		for (LineString lineString : getRawGeomList()) {			
			
			try {
				LineString euclidGeo = (LineString) JTS.transform(lineString, GeoUtils.getTransform(ref));
				_log.info("Coord = " + lineString.getCoordinate() + " Projection = " + GeoUtils.getEPSGCodefromUTS(ref));
				
				if (!geometryList.isEmpty()) {
					
					GeometrySnapper snapper = new GeometrySnapper(euclidGeo);
					MultiLineString currGeomCollection = gf.createMultiLineString(geometryList.toArray(new LineString[geometryList.size()]));
					euclidGeo = (LineString) snapper.snapTo(currGeomCollection , 10d);
				} 
	          
				euclidGeo.setUserData(lineString.getUserData());
				geometryList.add(euclidGeo);
			} catch (Exception e) {
				_log.warn(e.getMessage());
			}
		}

		GeometryCollection geomCollection = gf.createGeometryCollection(geometryList.toArray(new Geometry[geometryList.size()]));
		geomCollection = (GeometryCollection) DouglasPeuckerSimplifier.simplify(geomCollection, 2d);	

		List<LineString> simplifiedGeoms = Lists.newArrayList();  
		for (int i = 0; i < geomCollection.getNumGeometries(); i++) {
	          LineString euclidGeo = (LineString) geomCollection.getGeometryN(i);
	          simplifiedGeoms.add(euclidGeo);
		}

		// Convert the raw geoms to GeoJson
		String json = getGeoJson(simplifiedGeoms);
		return json;	
	}

	@RequestMapping("/graph-debug.do")
	public ModelAndView index() {
		
		return new ModelAndView("graph_debug.jspx");
	}
	
	private String getGeoJson(List<LineString> geoms) {
		StringBuilder sb = new StringBuilder();
		sb.append("{ \"type\": \"FeatureCollection\",");
		sb.append("\"features\": [");
		boolean first = true;
		for (final LineString geom : geoms) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}
			sb.append(getGeoJson(geom));
		}
		sb.append("]");
		sb.append("}");
		return sb.toString();
	}
	private String getGeoJson(LineString geom) {
		StringBuilder sb = new StringBuilder();
		sb.append("{ \"type\": \"Feature\", ");
		sb.append("\"geometry\": {");
		sb.append("\"type\": \"LineString\",");
		sb.append("\"coordinates\": [");
		boolean first = true;
		for (Coordinate c : geom.getCoordinates()) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}
			sb.append("[");
			sb.append(c.x);
			sb.append(",");
			sb.append(c.y);
			sb.append("]");
		}
		sb.append("]");
		sb.append("}, \"properties\": {");
		sb.append("}");		
		sb.append("}");
		return sb.toString();
	}
}
