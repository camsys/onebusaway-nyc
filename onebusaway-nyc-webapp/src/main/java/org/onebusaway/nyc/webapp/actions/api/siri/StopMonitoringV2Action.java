/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
import org.apache.struts2.convention.annotation.ParentPackage;
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

import uk.org.siri.siri_2.ErrorDescriptionStructure;
import uk.org.siri.siri_2.MonitoredStopVisitStructure;
import uk.org.siri.siri_2.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri_2.OtherErrorStructure;
import uk.org.siri.siri_2.ServiceDelivery;
import uk.org.siri.siri_2.ServiceDeliveryErrorConditionStructure;
import uk.org.siri.siri_2.Siri;
import uk.org.siri.siri_2.StopMonitoringDeliveryStructure;

@ParentPackage("onebusaway-webapp-api")
public class StopMonitoringV2Action extends OneBusAwayNYCActionSupport
		implements ServletRequestAware, ServletResponseAware {

	private static final long serialVersionUID = 1L;

	private static final String PREV_TRIP = "prevTrip";

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

		List<AgencyAndId> stopIds = new ArrayList<AgencyAndId>();
		String stopIdsErrorString = "";
		if (_request.getParameter("MonitoringRef") != null) {
			try {
				// If the user included an agency id as part of the stop id,
				// ignore any OperatorRef arg
				// or lack of OperatorRef arg and just use the included one.
				AgencyAndId stopId = AgencyAndIdLibrary
						.convertFromString(_request
								.getParameter("MonitoringRef"));
				if (_monitoringActionSupport.isValidStop(stopId, _nycTransitDataService)) {
					stopIds.add(stopId);
				} else {
					stopIdsErrorString += "No such stop: " + stopId.toString()
							+ ".";
				}
			} catch (Exception e) {
				// The user didn't provide an agency id in the MonitoringRef, so
				// use our list of operator refs
				for (String agency : agencyIds) {
					AgencyAndId stopId = new AgencyAndId(agency,
							_request.getParameter("MonitoringRef"));
					if (_monitoringActionSupport.isValidStop(stopId, _nycTransitDataService)) {
						stopIds.add(stopId);
					} else {
						stopIdsErrorString += "No such stop: "
								+ stopId.toString() + ". ";
					}
				}
				stopIdsErrorString = stopIdsErrorString.trim();
			}
			if (stopIds.size() > 0)
				stopIdsErrorString = "";
		} else {
			stopIdsErrorString = "You must provide a MonitoringRef.";
		}

		List<AgencyAndId> routeIds = new ArrayList<AgencyAndId>();
		String routeIdsErrorString = "";
		if (_request.getParameter("LineRef") != null) {
			try {
				// Same as above for stop id
				AgencyAndId routeId = AgencyAndIdLibrary
						.convertFromString(_request.getParameter("LineRef"));
				if (_monitoringActionSupport.isValidRoute(routeId, _nycTransitDataService)) {
					routeIds.add(routeId);
				} else {
					routeIdsErrorString += "No such route: "
							+ routeId.toString() + ".";
				}
			} catch (Exception e) {
				// Same as above for stop id
				for (String agency : agencyIds) {
					AgencyAndId routeId = new AgencyAndId(agency,
							_request.getParameter("LineRef"));
					if (_monitoringActionSupport.isValidRoute(routeId, _nycTransitDataService)) {
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

		int maximumOnwardCalls = 0;
		if (detailLevel != null && detailLevel.equals("calls")) {
			maximumOnwardCalls = Integer.MAX_VALUE;

			try {
				maximumOnwardCalls = Integer.parseInt(_request
						.getParameter("MaximumNumberOfCallsOnwards"));
			} catch (NumberFormatException e) {
				maximumOnwardCalls = Integer.MAX_VALUE;
			}
		}

		int maximumStopVisits = Integer.MAX_VALUE;
		try {
			maximumStopVisits = Integer.parseInt(_request
					.getParameter("MaximumStopVisits"));
		} catch (NumberFormatException e) {
			maximumStopVisits = Integer.MAX_VALUE;
		}

		Integer minimumStopVisitsPerLine = null;
		try {
			minimumStopVisitsPerLine = Integer.parseInt(_request
					.getParameter("MinimumStopVisitsPerLine"));
		} catch (NumberFormatException e) {
			minimumStopVisitsPerLine = null;
		}

		if (_monitoringActionSupport
				.canReportToGoogleAnalytics(_configurationService)) {
			_monitoringActionSupport.reportToGoogleAnalytics(_request,
					"Stop Monitoring", StringUtils.join(stopIds, ","),
					_configurationService);
		}

		// Monitored Stop Visits
		List<MonitoredStopVisitStructure> visits = new ArrayList<MonitoredStopVisitStructure>();
		Map<String, MonitoredStopVisitStructure> visitsMap = new HashMap<String, MonitoredStopVisitStructure>();

		for (AgencyAndId stopId : stopIds) {

			if (!stopId.hasValues())
				continue;

			// Stop ids can only be valid here because we only added valid ones
			// to stopIds.
			List<MonitoredStopVisitStructure> visitsForStop = _realtimeService
					.getMonitoredStopVisitsForStop(stopId.toString(),
							maximumOnwardCalls, responseTimestamp);
			if (visitsForStop != null)
				visits.addAll(visitsForStop);
		}

		List<MonitoredStopVisitStructure> filteredVisits = new ArrayList<MonitoredStopVisitStructure>();

		Map<AgencyAndId, Integer> visitCountByLine = new HashMap<AgencyAndId, Integer>();
		int visitCount = 0;

		for (MonitoredStopVisitStructure visit : visits) {
			MonitoredVehicleJourneyStructure journey = visit
					.getMonitoredVehicleJourney();

			AgencyAndId thisRouteId = AgencyAndIdLibrary
					.convertFromString(journey.getLineRef().getValue());
			String thisDirectionId = journey.getDirectionRef().getValue();

			// user filtering
			if (routeIds.size() > 0 && !routeIds.contains(thisRouteId))
				continue;

			if (directionId != null && !thisDirectionId.equals(directionId))
				continue;

			// visit count filters
			Integer visitCountForThisLine = visitCountByLine.get(thisRouteId);
			if (visitCountForThisLine == null) {
				visitCountForThisLine = 0;
			}

			if (visitCount >= maximumStopVisits) {
				if (minimumStopVisitsPerLine == null) {
					break;
				} else {
					if (visitCountForThisLine >= minimumStopVisitsPerLine) {
						continue;
					}
				}
			}

			// unique stops filters
			if (visit.getMonitoredVehicleJourney() == null
					|| visit.getMonitoredVehicleJourney().getVehicleRef() == null
					|| StringUtils.isBlank(visit.getMonitoredVehicleJourney()
							.getVehicleRef().getValue())) {
				continue;
			} else {
				String visitKey = visit.getMonitoredVehicleJourney()
						.getVehicleRef().getValue();
				if (visitsMap.containsKey(visit.getMonitoredVehicleJourney()
						.getVehicleRef().getValue())) {
					if (visit.getMonitoredVehicleJourney().getProgressStatus() == null) {
						visitsMap.remove(visitKey);
						visitsMap.put(visitKey, visit);
					}
					continue;
				} else {
					visitsMap.put(visit.getMonitoredVehicleJourney()
							.getVehicleRef().getValue(), visit);
				}
			}

			filteredVisits.add(visit);

			visitCount++;
			visitCountForThisLine++;
			visitCountByLine.put(thisRouteId, visitCountForThisLine);
		}
		visits = filteredVisits;

		Exception error = null;
		if (stopIds.size() == 0
				|| (_request.getParameter("LineRef") != null && routeIds.size() == 0)) {
			String errorString = (stopIdsErrorString + " " + routeIdsErrorString)
					.trim();
			error = new Exception(errorString);
		}

		_response = generateSiriResponse(visits, stopIds, error,
				responseTimestamp);

		try {
			this._servletResponse.getWriter().write(getStopMonitoring());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private Siri generateSiriResponse(List<MonitoredStopVisitStructure> visits,
			List<AgencyAndId> stopIds, Exception error, long responseTimestamp) {

		StopMonitoringDeliveryStructure stopMonitoringDelivery = new StopMonitoringDeliveryStructure();
		stopMonitoringDelivery.setResponseTimestamp(DateUtil
				.toXmlGregorianCalendar(responseTimestamp));

		ServiceDelivery serviceDelivery = new ServiceDelivery();
		serviceDelivery.setResponseTimestamp(DateUtil
				.toXmlGregorianCalendar(responseTimestamp));
		serviceDelivery.getStopMonitoringDelivery().add(stopMonitoringDelivery);

		if (error != null) {
			ServiceDeliveryErrorConditionStructure errorConditionStructure = new ServiceDeliveryErrorConditionStructure();

			ErrorDescriptionStructure errorDescriptionStructure = new ErrorDescriptionStructure();
			errorDescriptionStructure.setValue(error.getMessage());

			OtherErrorStructure otherErrorStructure = new OtherErrorStructure();
			otherErrorStructure.setErrorText(error.getMessage());

			errorConditionStructure.setDescription(errorDescriptionStructure);
			errorConditionStructure.setOtherError(otherErrorStructure);

			stopMonitoringDelivery.setErrorCondition(errorConditionStructure);
		} else {
			Calendar gregorianCalendar = new GregorianCalendar();
			gregorianCalendar.setTimeInMillis(responseTimestamp);
			gregorianCalendar.add(Calendar.MINUTE, 1);
			stopMonitoringDelivery
					.setValidUntil(DateUtil
							.toXmlGregorianCalendar(gregorianCalendar
									.getTimeInMillis()));

			stopMonitoringDelivery.getMonitoredStopVisit().addAll(visits);

			serviceDelivery.setResponseTimestamp(DateUtil
					.toXmlGregorianCalendar(responseTimestamp));

			_serviceAlertsHelper.addSituationExchangeToSiriForStops(
					serviceDelivery, visits, _nycTransitDataService, stopIds);
			_serviceAlertsHelper.addGlobalServiceAlertsToServiceDelivery(
					serviceDelivery, _realtimeService);
		}

		Siri siri = new Siri();
		siri.setServiceDelivery(serviceDelivery);

		return siri;
	}

	public String getStopMonitoring() {
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
