//Copyright 2010, OpenPlans
//Licensed under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at

//http://www.apache.org/licenses/LICENSE-2.0

//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

var OBA = window.OBA || {};

OBA.theInfoWindow = null;
OBA.theInfoWindowMarker = null;

OBA.Popup = function(map, fetchFn, bubbleNodeFn) {
	function createWrapper(content) {
		var wrappedContent = jQuery('<div id="popup"></div>')
			.append(content)
			.appendTo("#map");

		wrappedContent = wrappedContent.css("width", 250)
		.css("height", wrappedContent.height());

		return wrappedContent.get(0);
	}

	return {
		refresh: function() {
			if(OBA.theInfoWindow === null) {
				return;
			}

			fetchFn(function(json) {            
				OBA.theInfoWindow.setContent(createWrapper(bubbleNodeFn(json)));     
			});
		},

		show: function(marker) {    
			if(OBA.theInfoWindow) {
				OBA.theInfoWindow.close();
			}

			fetchFn(function(json) {
				OBA.theInfoWindow = new google.maps.InfoWindow();
				OBA.theInfoWindowMarker = marker;

				OBA.theInfoWindow.setContent(createWrapper(bubbleNodeFn(json)));     
				OBA.theInfoWindow.open(map, marker);

				google.maps.event.addListenerOnce(OBA.theInfoWindow, 'closeclick', function() {
					OBA.theInfoWindowMarker = null;
					OBA.theInfoWindow = null;
				});
			});
		}
	};
};

//FIXME utilities? scope wrap to prevent global leak?
function makeJsonFetcher(url, data) {
	return function(callback) {
		jQuery.getJSON(url, data, callback);
	};
}

OBA.StopPopup = function(stopId, map) {
	var generateStopMarkup = function(json) {
		var stop, arrivals, refs = null;

		try {
			stop = json.data.references.stops[0];
			arrivals = json.data.entry.arrivalsAndDepartures;
			refs = json.data.references;
		} catch (typeError) {
			OBA.Util.log("invalid stop response from server");
			OBA.Util.log(json);
			return;
		}

		var stopId = stop.id;
		var latestUpdate = null;
		var name = stop.name;

		// vehicle location
		var routeToVehicleInfo = {};
		var applicableSituationIds = {};
		var headsignToRoute = {};
		var routeToVehicleCount = 0;
		jQuery.each(arrivals, function(_, arrival) {
			var headsign = arrival.tripHeadsign;
			var arrivalStopId = arrival.stopId;

			if (arrivalStopId !== stopId) {
				return;
			}

			headsignToRoute[headsign] = arrival.routeId;
			if (! routeToVehicleInfo[headsign]) {
				routeToVehicleInfo[headsign] = [];
			}
			routeToVehicleCount++;                

			jQuery.each(arrival.situationIds, function(_, situationId) {
				applicableSituationIds[situationId] = situationId;
			});

			if(OBA.Config.vehicleFilterFunction(arrival.tripStatus) === false) {
				return;          
			}
			
			var predicted = arrival.predicted;
			var routeId = arrival.routeId;
			var updateTime = parseInt(arrival.lastUpdateTime, 10);
			latestUpdate = latestUpdate ? Math.max(latestUpdate, updateTime) : updateTime;

			// only show positive distances
			var meters = arrival.distanceFromStop;
			var feet = OBA.Util.metersToFeet(meters);
			var stops = arrival.numberOfStopsAway;

			var vehicleInfo = {routeId: routeId,
					stops: stops,
					feet: feet,
					predicted: predicted};

			if (routeToVehicleInfo[headsign]) {
				routeToVehicleInfo[headsign].push(vehicleInfo);
			}
		});

		var lastUpdateString = null;        
		if (latestUpdate !== null && latestUpdate !== 0) {
			var lastUpdateDate = new Date(latestUpdate);
			lastUpdateString = OBA.Util.displayTime(lastUpdateDate);
		}

		var header = '<p class="header">' + name + '</p>' +
		'<p class="description">Stop #' + OBA.Util.parseEntityId(stopId) + '</p>' + 
		(lastUpdateString ? '<p class="meta">Last updated at ' + lastUpdateString + '</p>' : '');

		// service notices
		var notices = '<ul class="notices">';

		jQuery.each(refs.situations, function(_, situation) {
			var situationId = situation.id;

			if(situationId in applicableSituationIds) {
				notices += '<li>' + situation.description.value + '</li>';
			}
		});

		notices += '</ul>';

		var service = '<p>This stop serves:</p><ul>';

		jQuery.each(routeToVehicleInfo, function(headsign, vehicleInfos) {
			// sort based on distance
			vehicleInfos.sort(function(a, b) { return a.feet - b.feet; });

			var included = false;
			for (var i = 0; i < Math.min(vehicleInfos.length, 3); i++) {
				var distanceAway = vehicleInfos[i];
				if (distanceAway.stops > 50) {
					continue;
				}

				if(distanceAway.feet < 0) {
					continue;
				}
				
				service += '<li>';
				service += '<a href="#" class="searchLink" rel="' + OBA.Util.parseEntityId(distanceAway.routeId) + '">';
				service += OBA.Util.truncateToWidth(headsign, 100, 11);
				service += '</a>';

				if(distanceAway.stops === 0) {
					service += " (< 1 stop, " + OBA.Util.displayDistance(distanceAway.feet) + ")";
				} else {
					service += " (" + distanceAway.stops + " stop" + ((distanceAway.stops === 1) ? "" : "s") + ", ";
					service += OBA.Util.displayDistance(distanceAway.feet) + ")";
				}

				service += '</li>';

				included = true;
			}

			// no actual vehicles are in the system--but display those that usually stop here...
			if(! included && headsign !== null) {
				service += '<li>';
				service += '<a href="#" class="searchLink" rel="' + OBA.Util.parseEntityId(headsignToRoute[headsign]) + '">';
				service += OBA.Util.truncateToWidth(headsign, 200, 11);
				service += '</a>';
				service += '</li>';
			}
		});

		// default response if no headsigns are available
		if(routeToVehicleCount === 0) {
			var route = json.data.references.routes[0];

			service += '<li>';
			service += '<a href="#" class="searchLink" rel="' + OBA.Util.parseEntityId(route.id) + '">';
			service += OBA.Util.truncateToWidth(route.longName, 100, 11);
			service += '</a>';
			service += '</li>';
		}

		service += '</ul>';

		var bubble = jQuery(header + notices + service);

		bubble.find("a.searchLink").click(function(e) {
			e.preventDefault();

			var id = jQuery(this).attr("rel");
			var searchForm = jQuery("#search form");
			var searchInput = jQuery("#search input[type=text]");

			searchInput.val(id);
			searchForm.submit();

			return false;
		});

		return bubble;
	};

	var url = OBA.Config.stopUrl + "/" + stopId + ".json";
	return OBA.Popup(
			map,
			makeJsonFetcher(url, {version: 2, key: OBA.Config.apiKey, minutesBefore: 5, minutesAfter: 2880}),
			generateStopMarkup);
};

OBA.VehiclePopup = function(vehicleId, map) {
	var generateVehicleMarkup = function(json) {
		var tripDetails, tripStatus, stopTimes, refs, stops, route, trip, headsign = null;
		
		try {
			tripDetails = json.data.entry;
			tripStatus = tripDetails.status;
			stopTimes = tripDetails.schedule.stopTimes;
			refs = json.data.references;
			stops = refs.stops;
			route = refs.routes[0];
			trip = refs.trips[0];
			headsign = trip.tripHeadsign;
		} catch (typeError) {
			OBA.Util.log("invalid response for vehicle details");
			OBA.Util.log(json);
			return jQuery("<span>No data found for: " + vehicleId + "</span>");
		}

		stops = stops.slice(0);

		var stopIdsToStops = {};
		jQuery.each(stops, function(_, stop) {
			stopIdsToStops[stop.id] = stop;
		});

		// calculate the distances along the trip for all stops
		jQuery.each(stopTimes, function(_, stopTime) {
			var stopId = stopTime.stopId;
			var stop = stopIdsToStops[stopId];
			stop.scheduledDistance = stopTime.distanceAlongTrip;
		});

		// sort the stops by their distance along the route
		stops.sort(function(a, b) { return a.scheduledDistance - b.scheduledDistance; });

		// find how far along the trip we are given our current vehicle distance
		var distanceAlongTrip = tripStatus.distanceAlongTrip;
		var vehicleDistanceIdx = 0;
		for (var i = 0; i < stops.length; i++) {
			var stop = stops[i];
			var stopDistance = stop.scheduledDistance;
			var distanceDelta = distanceAlongTrip - stopDistance;
			if (distanceDelta <= 0) {
				vehicleDistanceIdx = i;
				break;
			}
		}

		// last update date
		var lastUpdateDate = new Date(tripStatus.lastUpdateTime);
		var lastUpdateString = OBA.Util.displayTime(lastUpdateDate);
		var isStale = (new Date().getTime() - tripStatus.lastUpdateTime >= 1000 * OBA.Config.staleDataTimeout);

		var header = '<p class="header' + ((typeof tripStatus.serviceNotice !== 'undefined') ? ' hasNotice' : '') + '">' + headsign + '</p>' +
		'<p class="description">Bus #' + OBA.Util.parseEntityId(vehicleId) + '</p>' +
		'<p class="meta">Last updated at ' + lastUpdateString + '</p>';

		if(isStale) {
			header += '<p class="meta stale">Location data is out of date and may be unreliable</p>';
		}

		// service notices
		var applicableSituationIds = {};
		jQuery.each(tripStatus.situationIds, function(_, sid) {
			applicableSituationIds[sid] = sid;
		});

		var notices = '<ul class="notices">';

		jQuery.each(refs.situations, function(_, situation) {
			var situationId = situation.id;

			if(situationId in applicableSituationIds) {
				notices += '<li>' + situation.description.value + '</li>';
			}
		});

		notices += '</ul>';

		// next stops
		var nextStops = stops.slice(vehicleDistanceIdx, vehicleDistanceIdx + 3);
		var nextStopsMarkup = '';

		if (nextStops && nextStops.length > 0 && 
				(typeof tripStatus.status !== 'undefined' && tripStatus.status !== 'deviated')) { 
			nextStopsMarkup += '<p>Next stops:</p><ul>';

			jQuery.each(nextStops, function(i, stop) {
				var displayStopId = OBA.Util.parseEntityId(stop.id);
				var stopName = stop.name;

				nextStopsMarkup += '<li>';
				nextStopsMarkup += '<a href="#" class="searchLink" rel="' + displayStopId + '">' 
								+ OBA.Util.truncateToWidth(stopName, 200, 11) + '</a>';

				// we only have one stop currently
				// this will not work if we get more than one
				// because we reuse the distance information for each
				var stopsAway = i;
				var stopsAwayStr = null;                
				var metersDistanceDelta = stop.scheduledDistance - distanceAlongTrip;
				var feet = OBA.Util.metersToFeet(metersDistanceDelta);
				var distanceStr = OBA.Util.displayDistance(feet);

				if(stopsAway === 0) {
					stopsAwayStr = "< 1 stop";
				} else {
					stopsAwayStr = (stopsAway === 1) ? "1 stop" : stopsAway + " stops";
				}

				nextStopsMarkup += ' (' + stopsAwayStr + ', ' + distanceStr + ')';
				nextStopsMarkup += '</li>';
			});

			nextStopsMarkup += '</ul>';
		} else {
			nextStopsMarkup += '<p>Next stops are not known for this vehicle.</p>';
		}

		bubble = jQuery(header + notices + nextStopsMarkup);

		bubble.find("a.searchLink").click(function(e) {
			e.preventDefault();

			var id = jQuery(this).attr("rel");
			var searchForm = jQuery("#search form");
			var searchInput = jQuery("#search input[type=text]");

			searchInput.val(id);
			searchForm.submit();

			return false;
		});

		return bubble;
	};

	var url = OBA.Config.vehicleUrl + "/" + vehicleId + ".json";
	return OBA.Popup(
			map,
			makeJsonFetcher(url, {key: OBA.Config.apiKey, version: 2, includeSchedule: true, includeTrip: true}),
			generateVehicleMarkup);
};
