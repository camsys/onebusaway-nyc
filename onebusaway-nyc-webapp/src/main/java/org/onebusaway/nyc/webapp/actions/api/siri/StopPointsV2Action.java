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

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.DateUtil;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelperV2;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeServiceV2;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri_2.AnnotatedStopPointStructure;
import uk.org.siri.siri_2.AnnotatedStopPointStructure.Lines;
import uk.org.siri.siri_2.ErrorDescriptionStructure;
import uk.org.siri.siri_2.LineDirectionStructure;
import uk.org.siri.siri_2.MonitoredStopVisitStructure;
import uk.org.siri.siri_2.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri_2.OtherErrorStructure;
import uk.org.siri.siri_2.ServiceDelivery;
import uk.org.siri.siri_2.ServiceDeliveryErrorConditionStructure;
import uk.org.siri.siri_2.Siri;
import uk.org.siri.siri_2.StopMonitoringDeliveryStructure;
import uk.org.siri.siri_2.StopPointsDeliveryStructure;

public class StopPointsV2Action extends OneBusAwayNYCActionSupport implements
		ServletRequestAware, ServletResponseAware {
	private static final long serialVersionUID = 1L;

	private static final String PREV_TRIP = "prevTrip";
	private static final int COORDINATES_COUNT = 4;

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

		String directionId = _request.getParameter("DirectionRef");

		// We need to support the user providing no agency id which means 'all
		// agencies'.
		// So, this array will hold a single agency if the user provides it or
		// all
		// agencies if the user provides none. We'll iterate over them later
		// while
		// querying for vehicles and routes
		List<String> agencyIds = new ArrayList<String>();

		// Try to get the agency id passed by the user
		String agencyId = _request.getParameter("OperatorRef");

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

		// Check for Bounding Box
		String boundsErrorString = "";
		String boundingBox = _request.getParameter("BoundingBox");
		CoordinateBounds bounds = null;
		if (boundingBox != null) {
			String[] coordinates = boundingBox.split(",");
			if (coordinates.length >= COORDINATES_COUNT) {
				bounds = new CoordinateBounds(
						Double.parseDouble(coordinates[0]),
						Double.parseDouble(coordinates[1]),
						Double.parseDouble(coordinates[2]),
						Double.parseDouble(coordinates[3]));
			}
		}

		if (bounds == null) {
			boundsErrorString += "You must provide " + COORDINATES_COUNT
					+ " coordinates.";
		}

		List<AgencyAndId> routeIds = new ArrayList<AgencyAndId>();
		String routeIdsErrorString = "";
		if (_request.getParameter("LineRef") != null) {
			try {
				AgencyAndId routeId = AgencyAndIdLibrary
						.convertFromString(_request.getParameter("LineRef"));
				if (_monitoringActionSupport.isValidRoute(routeId,
						_nycTransitDataService)) {
					routeIds.add(routeId);
				} else {
					routeIdsErrorString += "No such route: "
							+ routeId.toString() + ".";
				}
			} catch (Exception e) {
				for (String agency : agencyIds) {
					AgencyAndId routeId = new AgencyAndId(agency,
							_request.getParameter("LineRef"));
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
			if (routeIds.size() > 0)
				routeIdsErrorString = "";
		}

		String detailLevel = _request.getParameter("StopMonitoringDetailLevel");

		/*
		 * if (_monitoringActionSupport
		 * .canReportToGoogleAnalytics(_configurationService)) {
		 * _monitoringActionSupport.reportToGoogleAnalytics(_request,
		 * "Stop Monitoring", StringUtils.join(stopIds, ","),
		 * _configurationService); }
		 */

		// Annotated Stop Points
		List<AnnotatedStopPointStructure> stopPoints = _realtimeService
				.getAnnotatedStopPointStructuresForCoordinates(bounds,
						detailLevel, responseTimestamp);
		Map<String, MonitoredStopVisitStructure> stopPointsMap = new HashMap<String, MonitoredStopVisitStructure>();

		List<AnnotatedStopPointStructure> filteredStopPoints = new ArrayList<AnnotatedStopPointStructure>();
		List<LineDirectionStructure> filteredLineDirections = new ArrayList<LineDirectionStructure>();

		for (AnnotatedStopPointStructure stopPoint : stopPoints) {
			// TODO - LCARABALLLO - Is there a better way to do this
			// conversion/filtering
			List<LineDirectionStructure> lineDirections = (List<LineDirectionStructure>) (Object) stopPoint
					.getLines().getLineRefOrLineDirection();
			for (LineDirectionStructure lineDirection : lineDirections) {

				AgencyAndId thisRouteId = AgencyAndIdLibrary
						.convertFromString(lineDirection.getDirectionRef()
								.getValue());
				String thisDirectionId = lineDirection.getLineRef().getValue();

				// user filtering
				if (routeIds.size() > 0 && !routeIds.contains(thisRouteId))
					continue;
				if (directionId != null && !thisDirectionId.equals(directionId))
					continue;
				filteredLineDirections.add(lineDirection);
			}

			if (filteredLineDirections.size() == 0)
				continue;

			filteredStopPoints.add(stopPoint);

		}
		stopPoints = filteredStopPoints;

		Exception error = null;
		if (bounds == null
				|| (_request.getParameter("LineRef") != null && routeIds.size() == 0)) {
			String errorString = (boundsErrorString + " " + routeIdsErrorString)
					.trim();
			error = new Exception(errorString);
		}

		_response = generateSiriResponse(stopPoints, error, responseTimestamp);

		try {
			this._servletResponse.getWriter().write(getStopPoints());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private Siri generateSiriResponse(
			List<AnnotatedStopPointStructure> stopPoints, Exception error,
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

			stopPointsDelivery.setResponseTimestamp(DateUtil
					.toXmlGregorianCalendar(responseTimestamp));

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
