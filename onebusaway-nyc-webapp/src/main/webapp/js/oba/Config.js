/*
 * Copyright (c) 2011 Metropolitan Transportation Authority
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

var OBA = window.OBA || {};

// This is mostly for IE7, to make sure AJAX/JSON calls are not cached. 
$.ajaxSetup({ cache: false });

OBA.Config = {
		autocompleteUrl: "api/autocomplete",
		searchUrl: "api/search",
		configUrl: "api/config",
		stopsWithinBoundsUrl: "api/stops-within-bounds",
		stopsOnRouteForDirection: "api/stops-on-route-for-direction",
		stopForId: "api/stop-for-id",

		// siriSMUrl and siriVMUrl now moved to config.jspx
		
		refreshInterval: 15000,
		
		// This variable is overwritten by the configuration service--the JS found at configUrl (above)
		staleTimeout: 120,

		// This method is called by the JS found at configUrl (above) when the configuration has finished loading.
		configurationHasLoaded: function() {
		},
		
		// For debugging: set to an ISO time string to make UI request from another time
		time: null,		
		
		// Called by UI functions that want to send events to GA
		analyticsFunction: function(type, value) {
			OBA.Config.analyticsEvent("Desktop Web", type, value);
		},
		
		// Sends a GA4 event via the gtag snippet in the page head. No-op unless GA is configured and
		// TDM display.googleAnalyticsEventsEnabled is true (see WEB-INF/decorators/includes/googleTag.jspx).
		// GA4 event names allow only letters, digits and underscores, so "Stop Marker Click"
		// becomes "stop_marker_click"; the old category and label ride along as parameters.
		analyticsEvent: function(category, action, label) {
			if (typeof gtag !== 'function' || window.obaGoogleAnalyticsEventsEnabled !== true) {
				return;
			}
			var eventName = String(action).toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '').substring(0, 40);
			gtag('event', eventName || 'unknown', { event_category: category, event_label: label });
		},
		
		loadLocationIcons: function() {
			var locationIcons = [], activeLocationIcons = [];
			var size = new google.maps.Size(24, 32), 
				o_point = new google.maps.Point(0,0), 
				mid_point = new google.maps.Point(12, 32);

			var normalLocationIcon = new google.maps.MarkerImage("img/location/location.png",
		            size, o_point, mid_point);
			var activeLocationIcon = new google.maps.MarkerImage("img/location/location_active.png",
					size, o_point, mid_point);
			
			locationIcons[0] = normalLocationIcon;
			activeLocationIcons[0] = activeLocationIcon;
			
			for (var i=1; i < 10; i++) {
				var numberedLocationIcon = new google.maps.MarkerImage("img/location/location_" + i + ".png",
						size, o_point, mid_point);
				var activeNumberedLocationIcon = new google.maps.MarkerImage("img/location/location_active_" + i + ".png",
						size, o_point, mid_point);
				
				locationIcons[i] = numberedLocationIcon;
				activeLocationIcons[i] = activeNumberedLocationIcon;
			}
			var shadowImage = new google.maps.MarkerImage('img/location/shadow.png',
					size, o_point, mid_point);
			
			return [locationIcons, activeLocationIcons, shadowImage];
		}
};
