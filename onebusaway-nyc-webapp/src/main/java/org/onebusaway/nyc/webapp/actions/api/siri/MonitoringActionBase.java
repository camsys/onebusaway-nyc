package org.onebusaway.nyc.webapp.actions.api.siri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeServiceV2;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

public class MonitoringActionBase extends OneBusAwayNYCActionSupport{

	private static final long serialVersionUID = -8569334328795108218L;
	
	public static final String LINE_REF = "LineRef";
	public static final String MONITORING_REF = "MonitoringRef";
	public static final String DIRECTION_REF = "DirectionRef";
	public static final String OPERATOR_REF = "Operator";
	public static final String BOUNDING_BOX = "BoundingBox";
	public static final String CIRCLE = "Circle";
	public static final String STOP_POINTS_DETAIL_LEVEL = "StopPointsDetailLevel";
	public static final String UPCOMING_SCHEDULED_SERVICE = "hasUpcomingScheduledService";
	public static final String MAX_ONWARD_CALLS = "MaximumNumberOfCallsOnwards";
	public static final String MAX_STOP_VISITS = "MaximumStopVisits";
	public static final String MIN_STOP_VISITS = "MinimumStopVisitsPerLine";

	@Autowired
	protected NycTransitDataService _nycTransitDataService;

	@Autowired
	protected RealtimeServiceV2 _realtimeService;
	
	@Autowired
	protected ConfigurationService _configurationService;

	protected boolean isValidRoute(AgencyAndId routeId) {
		if (routeId != null
				&& routeId.hasValues()
				&& _nycTransitDataService.getRouteForId(routeId.toString()) != null) {
			return true;
		}
		return false;
	}

	protected boolean isValidStop(AgencyAndId stopId) {
		try {
			StopBean stopBean = _nycTransitDataService
					.getStop(stopId.toString());
			if (stopBean != null)
				return true;
		} catch (Exception e) {
			// This means the stop id is not valid.
		}
		return false;
	}

	protected List<String> getAgencies(HttpServletRequest request,
			 NycTransitDataService nycTransitDataService) {
		String agencyId = request.getParameter("OperatorRef");
		List<String> agencyIds = new ArrayList<String>();
		if (agencyId != null) {
			// The user provided an agancy id so, use it
			agencyIds.add(agencyId);
		} else {
			// They did not provide an agency id, so interpret that an any/all
			// agencies.
			Map<String, List<CoordinateBounds>> agencies = nycTransitDataService
					.getAgencyIdsWithCoverageArea();
			agencyIds.addAll(agencies.keySet());
		}
		return agencyIds;
	}
	
	protected List<String> processAgencyIds(String agencyId){
		List<String> agencyIds = new ArrayList<String>();
		
		// Try to get the agency id passed by the user
		if (agencyId != null) {
			// The user provided an agancy id so, use it
			agencyIds.add(agencyId);
		} else {
			// They did not provide an agency id, so interpret that an any/all
			// agencies.
			Map<String, List<CoordinateBounds>> agencies = _nycTransitDataService
					.getAgencyIdsWithCoverageArea();
			agencyIds.addAll(agencies.keySet());
		}
		
		return agencyIds;
	}
	
	protected String processRouteIds(String lineRef, List<AgencyAndId> routeIds, List<String> agencyIds) {
		String routeIdsErrorString = "";
		if (lineRef != null) {
			try {
				AgencyAndId routeId = AgencyAndIdLibrary
						.convertFromString(lineRef);
				if (this.isValidRoute(routeId)) {
					routeIds.add(routeId);
				} else {
					routeIdsErrorString += "No such route: "
							+ routeId.toString() + ".";
				}
			} catch (Exception e) {
				for (String agency : agencyIds) {
					AgencyAndId routeId = new AgencyAndId(agency, lineRef);
					if (this.isValidRoute(routeId)) {
						routeIds.add(routeId);
					} else {
						routeIdsErrorString += "No such route: "
								+ routeId.toString() + ". ";
					}
				}
				routeIdsErrorString = routeIdsErrorString.trim();
			}
			if (routeIds.size() > 0) {

				routeIdsErrorString = "";
			}
		}
		
		return routeIdsErrorString;
	}
	
	
	protected String processStopIds(String monitoringRef, List<AgencyAndId> stopIds, List<String> agencyIds){
		
	    String stopIdsErrorString = "";
	    if (monitoringRef != null) {
	      try {
	        // If the user included an agency id as part of the stop id, ignore any OperatorRef arg
	        // or lack of OperatorRef arg and just use the included one.
	        AgencyAndId stopId = AgencyAndIdLibrary.convertFromString(monitoringRef);
	        if (isValidStop(stopId)) {
	          stopIds.add(stopId);
	        } else {
	          stopIdsErrorString += "No such stop: " + stopId.toString() + ".";
	        }
	      } catch (Exception e) {
	        // The user didn't provide an agency id in the MonitoringRef, so use our list of operator refs
	        for (String agency : agencyIds) {
	          AgencyAndId stopId = new AgencyAndId(agency, monitoringRef);
	          if (isValidStop(stopId)) {
	            stopIds.add(stopId);
	          } else {
	            stopIdsErrorString += "No such stop: " + stopId.toString() + ". ";
	          }
	        }
	        stopIdsErrorString = stopIdsErrorString.trim();
	      }
	      if (stopIds.size() > 0) stopIdsErrorString = "";
	    } else {
	      stopIdsErrorString = "You must provide a MonitoringRef.";
	    }
	    
	    return stopIdsErrorString;
	}
	
	
	protected boolean isValidBoundsDistance(CoordinateBounds bounds, double maxRadius){
		if(bounds != null){
		 CoordinateBounds maxBounds = SphericalGeometryLibrary.bounds(
				 bounds.getMinLat(), bounds.getMinLon(), maxRadius);

		 double maxLatSpan = (maxBounds.getMaxLat() - maxBounds.getMinLat());
		 double maxLonSpan = (maxBounds.getMaxLon() - maxBounds.getMinLon());
		 
		 double latSpan = (bounds.getMaxLat() - bounds.getMinLat());
	     double lonSpan = (bounds.getMaxLon() - bounds.getMinLon());
		 
		 if (latSpan < maxLatSpan && lonSpan < maxLonSpan) {
			 return true;
		      
		 }
		}
		return false;
	}
	
	protected CoordinateBounds getBounds(String boundingCoordinates) throws NumberFormatException{
		CoordinateBounds bounds = null;
		if (boundingCoordinates != null) {
			String[] coordinates = boundingCoordinates.split(",");
			
			if (coordinates.length >= 4) {

				CoordinateBounds userBounds = new CoordinateBounds(
						Double.parseDouble(coordinates[0]), 
						Double.parseDouble(coordinates[1]),
						Double.parseDouble(coordinates[2]),
						Double.parseDouble(coordinates[3]));
	
				bounds = SphericalGeometryLibrary.boundsFromLatLonOffset(
						userBounds.getMinLat(),
						userBounds.getMinLon(),
						(userBounds.getMaxLat() - userBounds.getMinLat()) / 2, 
						(userBounds.getMaxLon() - userBounds.getMinLon()) / 2);
				
			}
			else if(coordinates.length == 3){	
				
				bounds = SphericalGeometryLibrary.bounds(
						Double.parseDouble(coordinates[0]), 
						Double.parseDouble(coordinates[1]), 
						Double.parseDouble(coordinates[2]));
				
			}

		}
		return bounds;
	}

	

}