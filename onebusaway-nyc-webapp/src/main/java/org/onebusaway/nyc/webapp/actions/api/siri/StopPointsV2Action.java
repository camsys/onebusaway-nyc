package org.onebusaway.nyc.webapp.actions.api.siri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.DateUtil;
import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportV2;
import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportV2.Filters;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelperV2;
import org.onebusaway.nyc.presentation.model.DetailLevel;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeServiceV2;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriUpcomingServiceExtension;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri_2.AnnotatedStopPointStructure;
import uk.org.siri.siri_2.ErrorDescriptionStructure;
import uk.org.siri.siri_2.ExtensionsStructure;
import uk.org.siri.siri_2.OtherErrorStructure;
import uk.org.siri.siri_2.ServiceDeliveryErrorConditionStructure;
import uk.org.siri.siri_2.Siri;
import uk.org.siri.siri_2.StopPointsDeliveryStructure;

public class StopPointsV2Action extends OneBusAwayNYCActionSupport implements
		ServletRequestAware, ServletResponseAware {
	private static final long serialVersionUID = 1L;

	private static final int MIN_COORDINATES = 3;
	
	private static final String LINE_REF = "LineRef";
	private static final String DIRECTION_REF = "DirectionRef";
	private static final String OPERATOR_REF = "Operator";
	private static final String BOUNDING_BOX = "BoundingBox";
	private static final String CIRCLE = "Circle";
	private static final String STOP_POINTS_DETAIL_LEVEL = "StopPointsDetailLevel";
	private static final String UPCOMING_SCHEDULED_SERVICE = "hasUpcomingScheduledService";

	@Autowired
	public NycTransitDataService _nycTransitDataService;

	@Autowired
	private RealtimeServiceV2 _realtimeService;

	@Autowired
	private ConfigurationService _configurationService;

	private Siri _response;

	private ServiceAlertsHelperV2 _serviceAlertsHelper = new ServiceAlertsHelperV2();

	private HttpServletRequest _request;

	private HttpServletResponse _servletResponse;

	// See urlrewrite.xml as to how this is set. Which means this action doesn't
	// respect an HTTP Accept: header.
	private String _type = "xml";

	private MonitoringActionSupport _monitoringActionSupport = new MonitoringActionSupport();

	public void setType(String type) {
		_type = type;
	}

	@Override
	public String execute() {

		long responseTimestamp = getTime();
		_monitoringActionSupport.setupGoogleAnalytics(_request,
				_configurationService);

		_realtimeService.setTime(responseTimestamp);
		
		boolean useLineRefOnly = false;
		Boolean upcomingServiceAllStops = null;
		
		CoordinateBounds bounds = null;
		String boundCoordinates;
		
		//get the detail level parameter or set it to default if not specified
	    DetailLevel detailLevel;
	    if(_request.getParameter("StopMonitoringDetailLevel") == null){
	    	detailLevel = DetailLevel.NORMAL;
	    }else{
	    	detailLevel = DetailLevel.valueOf(_request.getParameter(STOP_POINTS_DETAIL_LEVEL));
	    }	

		// User Parameters
		String boundingBox = _request.getParameter(BOUNDING_BOX);
		String circle = _request.getParameter(CIRCLE);
		String lineRef = _request.getParameter(LINE_REF);
		String directionId = _request.getParameter(DIRECTION_REF);
		String agencyId = _request.getParameter(OPERATOR_REF);
		String hasUpcomingScheduledService = _request.getParameter(UPCOMING_SCHEDULED_SERVICE);

		// Error Strings
		String routeIdsErrorString = "";
		String boundsErrorString = "";
		
		
		/* 
		 * We need to support the user providing no agency id which means 'all
		agencies'. So, this array will hold a single agency if the user provides it or
		all agencies if the user provides none. We'll iterate over them later while
		querying for vehicles and routes
		*/
		List<String> agencyIds = processAgencyIds(agencyId);
		
		List<AgencyAndId> routeIds = new ArrayList<AgencyAndId>();
		
		routeIdsErrorString =  processRouteIds(lineRef, routeIds, agencyIds);

		
		if(StringUtils.isNotBlank(circle))
			boundCoordinates = circle;
		else{
			boundCoordinates = boundingBox;
		}
		
		try{
			bounds = getBounds(boundCoordinates,boundsErrorString);
		}
		catch (NumberFormatException nfe){
			boundsErrorString += "One or more invalid Coordinate values was provided.";
		}

		// Check for case where only LineRef was provided
		if (bounds == null) {
			if (routeIds.size() > 0) {
				useLineRefOnly = true;
			} else {
				boundsErrorString += "You must provide at least " + MIN_COORDINATES
						+ " BoundingBox or Circle coordinates or a LineRef value.";
			}
		}
	
		
		// TODO LCARABALLO GoogleAnalytics?
		/*
		 * if (_monitoringActionSupport
		 * .canReportToGoogleAnalytics(_configurationService)) {
		 * _monitoringActionSupport.reportToGoogleAnalytics(_request,
		 * "Stop Monitoring", StringUtils.join(stopIds, ","),
		 * _configurationService); }
		 */

		// Setup Filters
		Map<Filters, String> filters = new HashMap<Filters, String>();
		filters.put(Filters.DIRECTION_REF, directionId);
		filters.put(Filters.LINE_REF, lineRef);
		filters.put(Filters.UPCOMING_SCHEDULED_SERVICE,hasUpcomingScheduledService);

		// Annotated Stop Points
		List<AnnotatedStopPointStructure> stopPoints = new ArrayList<AnnotatedStopPointStructure>();
		Map<Boolean, List<AnnotatedStopPointStructure>> stopPointsMap;
		
		if (useLineRefOnly) {
			stopPointsMap = _realtimeService.getAnnotatedStopPointStructures(
					routeIds, detailLevel, responseTimestamp, filters);
		} else {
			stopPointsMap = _realtimeService.getAnnotatedStopPointStructures(
					bounds, detailLevel, responseTimestamp, filters);			
		}
		
		for (Map.Entry<Boolean, List<AnnotatedStopPointStructure>> entry : stopPointsMap.entrySet()) {
			upcomingServiceAllStops= entry.getKey();
			stopPoints.addAll(entry.getValue());
		}
		
		Exception error = null;
		if ((bounds == null && !useLineRefOnly)
				|| (_request.getParameter("LineRef") != null && routeIds.size() == 0)) {
			String errorString = (boundsErrorString + " " + routeIdsErrorString)
					.trim();
			error = new Exception(errorString);
		}
		
		_response = generateSiriResponse(stopPoints, upcomingServiceAllStops, error, responseTimestamp);

		try {
			this._servletResponse.getWriter().write(getStopPoints());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	

	private Siri generateSiriResponse(
			List<AnnotatedStopPointStructure> stopPoints, Boolean hasUpcomingScheduledService, Exception error,
			long responseTimestamp) {

		StopPointsDeliveryStructure stopPointsDelivery = new StopPointsDeliveryStructure();
		
		stopPointsDelivery.setResponseTimestamp(DateUtil
				.toXmlGregorianCalendar(responseTimestamp));

		if (error != null) {
			ServiceDeliveryErrorConditionStructure errorConditionStructure = new ServiceDeliveryErrorConditionStructure();

			ErrorDescriptionStructure errorDescriptionStructure = new ErrorDescriptionStructure();
			errorDescriptionStructure.setValue(error.getMessage());

			OtherErrorStructure otherErrorStructure = new OtherErrorStructure();
			otherErrorStructure.setErrorText(error.getMessage());

			errorConditionStructure.setDescription(errorDescriptionStructure);
			errorConditionStructure.setOtherError(otherErrorStructure);

			stopPointsDelivery.setErrorCondition(errorConditionStructure);
		} else {
			Calendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.setTimeInMillis(responseTimestamp);
			gregorianCalendar.add(Calendar.MINUTE, 1);
			stopPointsDelivery
					.setValidUntil(DateUtil
							.toXmlGregorianCalendar(gregorianCalendar
									.getTimeInMillis()));
			
			stopPointsDelivery.getAnnotatedStopPointRef().addAll(stopPoints);
			
			if(hasUpcomingScheduledService != null){
				// siri extensions
				ExtensionsStructure upcomingServiceExtensions = new ExtensionsStructure();
				SiriUpcomingServiceExtension upcomingService = new SiriUpcomingServiceExtension();
				upcomingService.setUpcomingScheduledService(hasUpcomingScheduledService);
				upcomingServiceExtensions.setAny(upcomingService);
				
				stopPointsDelivery.setExtensions(upcomingServiceExtensions);
			}
			
			stopPointsDelivery.setResponseTimestamp(DateUtil
					.toXmlGregorianCalendar(responseTimestamp));

			// TODO - LCARABALLO Do I still need serviceAlertsHelper?
			/*
			 * _serviceAlertsHelper.addSituationExchangeToSiriForStops(
			 * serviceDelivery, visits, _nycTransitDataService, stopIds);
			 * _serviceAlertsHelper.addGlobalServiceAlertsToServiceDelivery(
			 * serviceDelivery, _realtimeService);
			 */
		}

		Siri siri = new Siri();
		siri.setStopPointsDelivery(stopPointsDelivery);

		return siri;
	}

	public String getStopPoints() {
		try {
			if (_type.equals("xml")) {
				this._servletResponse.setContentType("application/xml");
				return _realtimeService.getSiriXmlSerializer()
						.getXml(_response);
			} else {
				this._servletResponse.setContentType("application/json");
				return _realtimeService.getSiriJsonSerializer().getJson(
						_response, _request.getParameter("callback"));
			}
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	private List<String> processAgencyIds(String agencyId){
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
	
	private String processRouteIds(String lineRef, List<AgencyAndId> routeIds, List<String> agencyIds) {
		String routeIdsErrorString = "";
		if (lineRef != null) {
			try {
				AgencyAndId routeId = AgencyAndIdLibrary
						.convertFromString(lineRef);
				if (_monitoringActionSupport.isValidRoute(routeId,
						_nycTransitDataService)) {
					routeIds.add(routeId);
				} else {
					routeIdsErrorString += "No such route: "
							+ routeId.toString() + ".";
				}
			} catch (Exception e) {
				for (String agency : agencyIds) {
					AgencyAndId routeId = new AgencyAndId(agency, lineRef);
					if (_monitoringActionSupport.isValidRoute(routeId,
							_nycTransitDataService)) {
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

	private CoordinateBounds getBounds(String boundingCoordinates, String boundsErrorString) throws NumberFormatException{
		CoordinateBounds bounds = null;
		if (boundingCoordinates != null) {
			String[] coordinates = boundingCoordinates.split(",");

			if (coordinates.length > MIN_COORDINATES) {

				bounds = SphericalGeometryLibrary.boundsFromLatLonOffset(
						Double.parseDouble(coordinates[0]), 
						Double.parseDouble(coordinates[1]),
						Double.parseDouble(coordinates[2])/2,
						Double.parseDouble(coordinates[3])/2);

			}
			else if(coordinates.length == MIN_COORDINATES){
				return SphericalGeometryLibrary.bounds(
						Double.parseDouble(coordinates[0]), 
						Double.parseDouble(coordinates[1]),
						Double.parseDouble(coordinates[2]));
			}

		}
		return bounds;
	}
	
	
	
	/*private CoordinateBounds getSearchBounds() {

	    if (_radius > 0) {
	      return SphericalGeometryLibrary.bounds(_lat, _lon, _radius);
	    } else if (_latSpan > 0 && _lonSpan > 0) {
	      return SphericalGeometryLibrary.boundsFromLatLonOffset(_lat, _lon,
	          _latSpan / 2, _lonSpan / 2);
	    } else {
	      if (_query != null)
	        return SphericalGeometryLibrary.bounds(_lat, _lon,
	            DEFAULT_SEARCH_RADIUS_WITH_QUERY);
	      else
	        return SphericalGeometryLibrary.bounds(_lat, _lon,
	            DEFAULT_SEARCH_RADIUS_WITHOUT_QUERY);
	    }
	  }
	
	*/
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this._request = request;
	}

	@Override
	public void setServletResponse(HttpServletResponse servletResponse) {
		this._servletResponse = servletResponse;
	}

	public HttpServletResponse getServletResponse() {
		return _servletResponse;
	}
}
