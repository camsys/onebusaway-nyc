package org.onebusaway.nyc.vehicle_tracking.utility;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.model.UserData;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class GeoJsonBuilder {
	
	/***
	 * 
	 * @param geoms
	 * @return
	 */
	public static String convert(List<Geometry> geoms) {
		StringBuilder sb = new StringBuilder();
		initiateFeatureCollection(sb);
		boolean first = true;
		for (final Geometry geom : geoms) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}
			add(sb, geom);
		}
		completeFeatureCollection(sb);
		return sb.toString();
	}
	/***
	 * 
	 * @param geoms
	 * @return
	 */
	public static String convertLineStrings(List<LineString> geoms) {
		StringBuilder sb = new StringBuilder();
		initiateFeatureCollection(sb);
		boolean first = true;
		for (final LineString geom : geoms) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}
			add(sb, geom);
		}
		completeFeatureCollection(sb);
		return sb.toString();
	}
	/***
	 * 
	 * @param geoms
	 * @return
	 */
	public static String convertPoints(List<Point> geoms) {
		StringBuilder sb = new StringBuilder();
		initiateFeatureCollection(sb);
		boolean first = true;
		for (final Point geom : geoms) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}
			add(sb, geom);
		}
		completeFeatureCollection(sb);
		return sb.toString();
	}
	/**
	 * 
	 * @param sb
	 */
	public static void completeFeatureCollection(StringBuilder sb) {
		
		sb.append("]");
		sb.append("}");

	}
	/**
	 * 
	 * @param sb
	 */
	public static void initiateFeatureCollection(StringBuilder sb) {
		
		sb.append("{ \"type\": \"FeatureCollection\",");
		sb.append("\"features\": [");

	}
	/***
	 * 
	 * @param geom
	 * @return
	 */
	private static void add(StringBuilder sb, Geometry geom) {
		if (geom instanceof LineString) {
			add(sb, (LineString) geom);
		} else if (geom instanceof Point) {
			add(sb, (Point) geom);			
		}
	}
	
	private static void add(StringBuilder sb, Point geom) {
		sb.append("{ \"type\": \"Feature\", ");
		sb.append("\"geometry\": {");
		sb.append("\"type\": \"Point\",");
		sb.append("\"coordinates\": [");
		sb.append(geom.getY());
		sb.append(",");
		sb.append(geom.getX());
		sb.append("]");
		sb.append("}, \"properties\": {");
		add(sb, (UserData) geom.getUserData());
		sb.append("}");		
		sb.append("}");
	}	
	private static void add(StringBuilder sb, LineString geom) {
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
			sb.append(c.y);
			sb.append(",");
			sb.append(c.x);
			sb.append("]");
		}
		sb.append("]");
		sb.append("}, \"properties\": {");
		add(sb, (UserData) geom.getUserData());
		sb.append("}");		
		sb.append("}");
	}	
	private static void add(StringBuilder sb, UserData userData) {
		if (null != userData) {
			boolean first = true;
			for (String name : userData.getNames()) {
				if (first) {
					first = false;
				} else {
					sb.append(",");
				}
				sb.append("\"");
				sb.append(name);
				sb.append("\":\"");
				sb.append(userData.getValue(name));
				sb.append("\"");
			}
		}
	}
}
