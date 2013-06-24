package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.LineString;

@Component
public class TransformService {

	private static final Logger logger = Logger.getLogger(TransformService.class.getName());

	private static final String geoWkt = "GEOGCS[" + "\"WGS 84\"," + "  DATUM[" + "    \"WGS_1984\","
	        + "    SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],"
	        + "    TOWGS84[0,0,0,0,0,0,0]," + "    AUTHORITY[\"EPSG\",\"6326\"]],"
	        + "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
	        + "  UNIT[\"DMSH\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],"
	        + "  AXIS[\"Lat\",NORTH]," + "  AXIS[\"Long\",EAST],"
	        + "  AUTHORITY[\"EPSG\",\"4326\"]]";

	private static final String utmWkt = 
			"PROJCS[\"WGS 84 / UTM zone 18N\"," 
			+ "  GEOGCS[\"WGS 84\","
			+ "    DATUM[\"WGS_1984\","		
			+ "      SPHEROID[\"WGS 84\",6378137,298.257223563,"
			+ "         AUTHORITY[\"EPSG\",\"7030\"]],"
			+ "      AUTHORITY[\"EPSG\",\"6326\"]],"
			+ "   PRIMEM[\"Greenwich\",0,"
            + "     AUTHORITY[\"EPSG\",\"8901\"]],"
            + "     UNIT[\"degree\",0.01745329251994328,"
            + "   AUTHORITY[\"EPSG\",\"9122\"]],"
            + "   AUTHORITY[\"EPSG\",\"4326\"]],"
            + " UNIT[\"metre\",1,"
            + " AUTHORITY[\"EPSG\",\"9001\"]],"
            + "PROJECTION[\"Transverse_Mercator\"],"
            + "PARAMETER[\"latitude_of_origin\",0],"
            + "PARAMETER[\"central_meridian\",-75],"
            + "PARAMETER[\"scale_factor\",0.9996],"
            + "PARAMETER[\"false_easting\",500000],"
            + "PARAMETER[\"false_northing\",0],"
            + "AUTHORITY[\"EPSG\",\"32618\"],"
            + "AXIS[\"Easting\",EAST],"
            + "AXIS[\"Northing\",NORTH]]";

	public TransformService() {
		logger.debug("In Constructor");
	}

	/***
	 * 
	 * @param shapeList
	 * @return
	 */
	public List<LineString> unprojectShapes(List<LineString> shapeList) {

		MathTransform reverseTransform = getTransform(getCoordinateReferenceSystem(utmWkt), getCoordinateReferenceSystem(geoWkt));
		List<LineString> geoms = new ArrayList<LineString>();
		for (LineString lineString : shapeList) {
			try {
				LineString geom = (LineString) JTS.transform(lineString, reverseTransform);
				geom.setUserData(lineString.getUserData());
				geoms.add(geom);
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
		}
		return geoms;
	}
	/***
	 * 
	 * @param shapeList
	 * @return
	 */
	public List<LineString> projectShapes(List<LineString> shapeList) {

		MathTransform forwardTransform = getTransform(getCoordinateReferenceSystem(geoWkt), getCoordinateReferenceSystem(utmWkt));
		List<LineString> geoms = new ArrayList<LineString>();
		for (LineString lineString : shapeList) {
			try {
				LineString geom = (LineString) JTS.transform(lineString, forwardTransform);
				geom.setUserData(lineString.getUserData());
				geoms.add(geom);
				//logger.debug("Before projection = " + lineString.toString());
				//logger.debug("After projection" + geom.toString());
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
		}
		return geoms;
	}
	
	/***
	 * 
	 * @param wkt
	 * @return
	 */
	private CoordinateReferenceSystem getCoordinateReferenceSystem(String wkt) {
		try {
			return CRS.parseWKT(wkt);
		} catch (FactoryException e) {
			e.printStackTrace();
		}		
		return null;
	}
	/***
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private MathTransform getTransform(CoordinateReferenceSystem from, CoordinateReferenceSystem to) {
				
	    try {	    	
	      final MathTransform transform =  CRS.findMathTransform(from, to);
	      return transform;
	    } catch (final NoSuchIdentifierException e) {
	      e.printStackTrace();
	    } catch (final FactoryException e) {
	      e.printStackTrace();
	    }

	    return null;
	}

}
