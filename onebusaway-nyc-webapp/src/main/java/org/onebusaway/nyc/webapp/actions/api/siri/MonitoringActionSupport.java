package org.onebusaway.nyc.webapp.actions.api.siri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.StopBean;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;
import com.dmurph.tracking.VisitorData;

public class MonitoringActionSupport {

	protected JGoogleAnalyticsTracker _googleAnalytics = null;
	private Boolean _reportToGoogleAnalytics = null;

	public MonitoringActionSupport() {
	}

	protected void setupGoogleAnalytics(HttpServletRequest request,
			ConfigurationService configService) {
		GoogleAnalyticsApiHelper gaApiHelper = new GoogleAnalyticsApiHelper(
				configService);

		String googleAnalyticsSiteId = configService
				.getConfigurationValueAsString("display.googleAnalyticsSiteId",
						null);

		try {
			if ((googleAnalyticsSiteId != null)
					&& (gaApiHelper.reportToGoogleAnalytics(request
							.getParameter("key")))) {
				VisitorData visitorData = VisitorData.newVisitor();
				visitorData.newRequest();
				AnalyticsConfigData config = new AnalyticsConfigData(
						googleAnalyticsSiteId, visitorData);
				_googleAnalytics = new JGoogleAnalyticsTracker(config,
						GoogleAnalyticsVersion.V_4_7_2);
			}
		} catch (Exception e) {
			// discard
		}
	}

	protected void reportToGoogleAnalytics(HttpServletRequest request,
			String event, String gaLabel, ConfigurationService configService) {
		GoogleAnalyticsApiHelper gaApiHelper = new GoogleAnalyticsApiHelper(
				configService);
		if (_googleAnalytics != null
				&& gaApiHelper.reportToGoogleAnalytics(request
						.getParameter("key"))) {
			try {
				_googleAnalytics.trackEvent("API", event, gaLabel);
			} catch (Exception e) {
				// discard
			}
		}
	}

	protected boolean canReportToGoogleAnalytics(
			ConfigurationService configService) {
		if (_reportToGoogleAnalytics == null) {
			_reportToGoogleAnalytics = "true".equals(configService
					.getConfigurationValueAsString(
							"display.reportToGoogleAnalytics", "false"));
		}
		return Boolean.TRUE.equals(_reportToGoogleAnalytics);
	}

	protected boolean isValidRoute(AgencyAndId routeId,
			NycTransitDataService nycTransitDataService) {
		if (routeId != null
				&& routeId.hasValues()
				&& nycTransitDataService.getRouteForId(routeId.toString()) != null) {
			return true;
		}
		return false;
	}

	protected boolean isValidStop(AgencyAndId stopId,
			NycTransitDataService nycTransitDataService) {
		try {
			StopBean stopBean = nycTransitDataService
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

}