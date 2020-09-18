/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.api.actions.api.siri;

import javax.servlet.http.HttpServletRequest;

import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.api.actions.api.siri.impl.GoogleAnalyticsApiHelper;

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

		String googleAnalyticsSiteId = configService.getConfigurationValueAsString("display.googleAnalyticsSiteId",
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

	
}