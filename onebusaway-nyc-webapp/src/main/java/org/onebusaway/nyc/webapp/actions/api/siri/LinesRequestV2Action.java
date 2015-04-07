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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.DateUtil;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriUpcomingServiceExtension;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.api.siri.impl.ServiceAlertsHelperV2;
import org.onebusaway.nyc.webapp.actions.api.siri.impl.SiriSupportV2.Filters;
import org.onebusaway.nyc.webapp.actions.api.siri.model.DetailLevel;
import org.onebusaway.nyc.webapp.actions.api.siri.service.RealtimeServiceV2;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri_2.AnnotatedLineStructure;
import uk.org.siri.siri_2.AnnotatedStopPointStructure;
import uk.org.siri.siri_2.ErrorDescriptionStructure;
import uk.org.siri.siri_2.ExtensionsStructure;
import uk.org.siri.siri_2.LinesDeliveryStructure;
import uk.org.siri.siri_2.OtherErrorStructure;
import uk.org.siri.siri_2.ServiceDeliveryErrorConditionStructure;
import uk.org.siri.siri_2.Siri;

public class LinesRequestV2Action extends MonitoringActionBase implements
		ServletRequestAware, ServletResponseAware {
	private static final long serialVersionUID = 1L;

	private static final double MAX_BOUNDS_RADIUS = 500;
	private static final String LINE_REF = "LineRef";
	private static final String DIRECTION_REF = "LineDirectionRef";
	private static final String OPERATOR_REF = "Operator";
	private static final String BOUNDING_BOX = "BoundingBox";
	private static final String CIRCLE = "Circle";
	private static final String LINES_DETAIL_LEVEL = "LinesDetailLevel";
	private static final String UPCOMING_SCHEDULED_SERVICE = "hasUpcomingScheduledService";

	private Siri _response;

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
		boolean validBoundDistance = true;

		// get the detail level parameter or set it to default if not specified
		DetailLevel detailLevel;
		if (_request.getParameter(LINES_DETAIL_LEVEL) == null) {
			detailLevel = DetailLevel.NORMAL;
		} else {
			detailLevel = DetailLevel.valueOf(_request.getParameter(LINES_DETAIL_LEVEL).toUpperCase());
		}

		// User Parameters
		String boundingBox = _request.getParameter(BOUNDING_BOX);
		String circle = _request.getParameter(CIRCLE);
		String lineRef = _request.getParameter(LINE_REF);
		String directionId = _request.getParameter(DIRECTION_REF);
		String agencyId = _request.getParameter(OPERATOR_REF);
		String hasUpcomingScheduledService = _request
				.getParameter(UPCOMING_SCHEDULED_SERVICE);

		// Error Strings
		String routeIdsErrorString = "";
		String boundsErrorString = "";

		/*
		 * We need to support the user providing no agency id which means 'all
		 * agencies'. So, this array will hold a single agency if the user
		 * provides it or all agencies if the user provides none. We'll iterate
		 * over them later while querying for vehicles and routes
		 */
		List<String> agencyIds = processAgencyIds(agencyId);

		List<AgencyAndId> routeIds = new ArrayList<AgencyAndId>();

		routeIdsErrorString = processRouteIds(lineRef, routeIds, agencyIds);

		// Calculate Bounds
		try {
			if (StringUtils.isNotBlank(circle)) {
				bounds = getBounds(circle);
				
				if (bounds != null && !isValidBoundsDistance(bounds, MAX_BOUNDS_RADIUS)) {
					boundsErrorString += "Provided values exceed allowed search radius of "
							+ MAX_BOUNDS_RADIUS + "m. ";
					validBoundDistance = false;
				}
				
			} else if (StringUtils.isNotBlank(boundingBox)) {
				bounds = getBounds(boundingBox);
				
				if (bounds != null && !isValidBoundBoxDistance(bounds, MAX_BOUNDS_RADIUS)) {
					boundsErrorString += "Provided values exceed allowed search radius of "
							+ MAX_BOUNDS_RADIUS + "m. ";
					validBoundDistance = false;
				}
			}
			
		} catch (NumberFormatException nfe) {
			boundsErrorString += "One or more coordinate values contain a non-numeric value. ";
		}

		// Check for case where only LineRef was provided
		if (routeIds.size() > 0) {
			useLineRefOnly = true;
		} else if(bounds == null) {
			boundsErrorString += "You must provide at least "
					+ MonitoringActionSupport.MIN_COORDINATES
					+ " BoundingBox or Circle coordinates or a LineRef value.";
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
		filters.put(Filters.UPCOMING_SCHEDULED_SERVICE, hasUpcomingScheduledService);

		// Annotated Lines
		List<AnnotatedLineStructure> lines = new ArrayList<AnnotatedLineStructure>();
		Map<Boolean, List<AnnotatedLineStructure>> linesMap;

		// Error Handler
		Exception error = null;
		if ((bounds == null && !useLineRefOnly)
				|| (lineRef != null && routeIds.size() == 0)
				|| !validBoundDistance) {
			String errorString = (boundsErrorString + " " + routeIdsErrorString)
					.trim();
			error = new Exception(errorString);
		} else {

			if (useLineRefOnly) {
				linesMap = _realtimeService
						.getAnnotatedLineStructures(routeIds, detailLevel,
								responseTimestamp, filters);
			} else {
				linesMap = _realtimeService
						.getAnnotatedLineStructures(bounds, detailLevel,
								responseTimestamp, filters);
			}

			for (Map.Entry<Boolean, List<AnnotatedLineStructure>> entry : linesMap
					.entrySet()) {
				upcomingServiceAllStops = entry.getKey();
				lines.addAll(entry.getValue());
			}
		}

		_response = generateSiriResponse(lines, upcomingServiceAllStops,
				error, responseTimestamp);

		try {
			this._servletResponse.getWriter().write(getLines());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private Siri generateSiriResponse(
			List<AnnotatedLineStructure> lines,
			Boolean hasUpcomingScheduledService, Exception error,
			long responseTimestamp) {

		LinesDeliveryStructure linesDelivery = new LinesDeliveryStructure();

		linesDelivery.setResponseTimestamp(DateUtil
				.toXmlGregorianCalendar(responseTimestamp));

		if (error != null) {
			ServiceDeliveryErrorConditionStructure errorConditionStructure = new ServiceDeliveryErrorConditionStructure();

			ErrorDescriptionStructure errorDescriptionStructure = new ErrorDescriptionStructure();
			errorDescriptionStructure.setValue(error.getMessage());

			OtherErrorStructure otherErrorStructure = new OtherErrorStructure();
			otherErrorStructure.setErrorText(error.getMessage());

			errorConditionStructure.setDescription(errorDescriptionStructure);
			errorConditionStructure.setOtherError(otherErrorStructure);

			linesDelivery.setErrorCondition(errorConditionStructure);
		} else {
			Calendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.setTimeInMillis(responseTimestamp);
			gregorianCalendar.add(Calendar.MINUTE, 1);
			linesDelivery
					.setValidUntil(DateUtil
							.toXmlGregorianCalendar(gregorianCalendar
									.getTimeInMillis()));

			linesDelivery.getAnnotatedLineRef().addAll(lines);

			if (hasUpcomingScheduledService != null) {
				// siri extensions
				ExtensionsStructure upcomingServiceExtensions = new ExtensionsStructure();
				upcomingServiceExtensions.setAny(hasUpcomingScheduledService);
				SiriUpcomingServiceExtension upcomingService = new SiriUpcomingServiceExtension();
				upcomingService
						.setUpcomingScheduledService(hasUpcomingScheduledService);
				upcomingServiceExtensions.setAny(upcomingService);
				linesDelivery.setExtensions(upcomingServiceExtensions);
			}

			linesDelivery.setResponseTimestamp(DateUtil
					.toXmlGregorianCalendar(responseTimestamp));

		}

		Siri siri = new Siri();
		siri.setLinesDelivery(linesDelivery);

		return siri;
	}

	public String getLines() {
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
