/*
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var OBA = window.OBA || {};

/*
 * This is mostly for IE7, to make sure AJAX/JSON calls are not cached. 
 */
$.ajaxSetup({ cache: false });

OBA.Config = {
		// print debug messages to firebug console?
		debug: false,

		// urls to fetch various data
		searchUrl: "search.action",
		routeShapeUrl: "/onebusaway-api-webapp/api/where/stops-for-route",
		stopsUrl: "/onebusaway-api-webapp/api/where/stops-for-location.json",
		stopUrl: "/onebusaway-api-webapp/api/where/arrivals-and-departures-for-stop",
		vehiclesUrl:"/onebusaway-api-webapp/api/where/trips-for-route",
		vehicleUrl: "/onebusaway-api-webapp/api/where/trip-for-vehicle",

		// default agency ID
		agencyId: "MTA NYCT",
		
		// google analytics reporting ID
		googleAnalyticsId: 'UA-XXXXXXXX-X',
		
		// a parameter that allows us to "go back in time" by locking all API
		// requests to a certain time, making it possible to recreate the state
		// of the system at a particular moment in time
		//time: 1295479980,

		// milliseconds to wait in-between polls for bus locations
		pollingInterval: 5000,

		// these are both overridden by server-side configuration via
		// client-side code (from /config)--these are just reasonable defaults.
		staleDataTimeout: 120,
		hideTimeout: 300,
		
		// clock skew between server and client--overridden by client-side
		// code upon page load (from /config)
		clockSkew: 0,
		
		// Time, in minutes, that we'll look into the past for a scheduled
		// arrival at a stop.  If a bus is running more than X minutes
		// late, it won't get listed in the arrival list. Also change in NycPresentationService.java, line 523-
		arrivalsMinutesBefore: 60,
		arrivalsMinutesAfter: 90,

		// display convention thresholds
		showDistanceInStopsThresholdInStops: 3,
		atStopThresholdInFeet: 100,
		arrivingThresholdInFeet: 500,
		arrivingThresholdInStops: 0,

		// marker images used
		// (the path below needs to be absolute!)
		vehicleIconFilePrefix: "/img/vehicle/vehicle",
		vehicleIconFileType: "png",
		vehicleIconSize: 51,
		vehicleIconCenter: 25,

		// (the path below needs to be absolute!)
		stopIconFilePrefix: "/img/stop/stop",
		stopIconFileType: "png",
		stopIconSize: 21,
		stopIconCenter: 10,

		// api key used for webapp
		apiKey: "TEST",

		vehicleFilterFunction: function(type, tripStatus) {
		    // UI states here:
		    // https://spreadsheets.google.com/ccc?key=0Al2nqv1nCD71dGt5SkpHajRQZmdLaVZScnhoYVhiZWc&hl=en#gid=0
			
			// don't show non-realtime trips (row 8)
			if(tripStatus === null || tripStatus.predicted === false) {
				return false;
			}
			
			var status = ((typeof tripStatus.status !== 'undefined' && tripStatus.status !== '') ? tripStatus.status : null);
			var phase = ((typeof tripStatus.phase !== 'undefined' && tripStatus.phase !== '') ? tripStatus.phase : null);

			// hide disabled vehicles (row 7)
			if(status !== null && status.toLowerCase() === 'disabled') {
				return false;
			}

			// hide deviated vehicles in stop popup
			if(type === "stop" && status !== null && status.toLowerCase() === 'deviated') {
				return false;
			}
			
			// hide deadheading vehicles (except within a block) (row 3)
			// hide vehicles at the depot (row 1)
			if(phase !== null && phase.toLowerCase() !== 'in_progress' 
					&& phase.toLowerCase() !== 'layover_before' 
					&& phase.toLowerCase() !== 'layover_during') {	
				return false;
			}
			
			var now = OBA.Util.getTime();
			if(typeof OBA.Config.time !== 'undefined' && OBA.Config.time !== null) {
				now = OBA.Config.time * 1000;
			}
			
			// hide data >= hideTimeout seconds old (row 5)
			if(typeof tripStatus.lastUpdateTime !== 'undefined' 
				&& now - tripStatus.lastUpdateTime >= 1000 * OBA.Config.hideTimeout) {
				return false;
			}

			return true;
		},
		
		infoBubbleFooterFunction: function(type, query) {
			var html = '';

			if(type === "stop")	{
				html += '<div class="footer">';
				html += '<span class="header">At the bus stop... </strong></span>';
				
				html += 'Text <strong>MTA ' + query + '</strong> to <strong>41411</strong> ';
				html += 'or check <a href="http://' + window.location.hostname + ((window.location.port !== '') ? ':' + window.location.port: '') + '/m/?q=' + query + '">this stop</a> on your smartphone!';

				html += '</div>';
				
			} else if(type === "route") {
				html += '<div class="footer">';
				html += '<span class="header">At the bus stop... </strong></span>';

				html += 'Check <a href="http://' + window.location.hostname + ((window.location.port !== '') ? ':' + window.location.port : '') + '/m/?q=' + query + '">this route</a> on your smartphone!';

				html += '</div>';				

			} else if(type === "sign") {
				html += 'Text <strong>MTA ' + query + '</strong> to <strong>41411</strong> ';
				html += 'or check this stop on your smartphone at <strong>http://' + window.location.hostname + ((window.location.port !== '') ? ':' + window.location.port : '') + '/m/?q=' + query + '</strong>';
			}
			
			return html;
		},
		
		analyticsFunction: function(type, value) {
			_gaq.push(['_trackEvent', "Desktop Web", type, value]);
		}
};
