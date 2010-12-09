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

// the marker associated with any popped up infoWindow
OBA.popupMarker = null;

OBA.Popup = function(map, fetchFn, bubbleNodeFn) {
	var infoWindow = null;
	
	function createWrapper(content) {
		var wrappedContent = jQuery('<div id="popup"></div>')
			.append(content)
			.appendTo("#map");

		wrappedContent = wrappedContent.css("width", 325)
			.css("height", wrappedContent.height());

		return wrappedContent.get(0);
	}

	return {
		hide: function() {
			if(infoWindow !== null) {
				infoWindow.close();
				infoWindow = null;
			}
		},
		
		refresh: function() {
			if(infoWindow === null) {
				return;
			}

			fetchFn(function(json) {            
				if(infoWindow !== null) {
					infoWindow.setContent(createWrapper(bubbleNodeFn(json)));     
				}
			});
		},

		show: function(marker) {    
			if(OBA.popupMarker !== null) {
				var popup = OBA.popupMarker.getPopup();
				
				if(popup) {
					popup.hide();
				}
			}

			var shareLinkDiv = jQuery("#share_link");
			shareLinkDiv.hide();
			
			fetchFn(function(json) {
				infoWindow = new google.maps.InfoWindow();
				infoWindow.setContent(createWrapper(bubbleNodeFn(json)));     
			
				var pixelOffset = null;
				var markerType = marker.getType();
				if(markerType === "stop") {
					pixelOffset = new google.maps.Size(0, OBA.Config.stopIconCenter.y);
				} else if(markerType === "vehicle") {
					pixelOffset = new google.maps.Size(0, OBA.Config.vehicleIconCenter.y);
				}

				infoWindow.setOptions({ zIndex: 100, pixelOffset: pixelOffset });
				infoWindow.open(map, marker.getRawMarker());
				
				google.maps.event.addListenerOnce(infoWindow, 'closeclick', function() {
					OBA.popupMarker = null;
				});
			});
		
			OBA.popupMarker = marker;
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
		var name = stop.name;
		var latestUpdate = null;

		// vehicle location
		var applicableSituationIds = {};
		var routeToVehicleInfo = {};
		var routeToHeadsign = {};
		var routeToVehicleCount = 0;
		jQuery.each(arrivals, function(_, arrival) {
			var routeId = arrival.routeId;
			var arrivalStopId = arrival.stopId;
			var headsign = arrival.tripHeadsign;

			if (arrivalStopId !== stopId || routeId === null) {
				return;
			}
			
			// most common headsign? FIXME
			routeToHeadsign[routeId] = headsign;
			if (! routeToVehicleInfo[routeId]) {
				routeToVehicleInfo[routeId] = [];
			}
			routeToVehicleCount++;                

			jQuery.each(arrival.situationIds, function(_, situationId) {
				applicableSituationIds[situationId] = situationId;
			});

			if(OBA.Config.vehicleFilterFunction("stop", arrival.tripStatus) === false) {
				return;          
			}
			
			var routeId = arrival.routeId;
			var updateTime = parseInt(arrival.lastUpdateTime, 10);
			var meters = arrival.distanceFromStop;
			var feet = OBA.Util.metersToFeet(meters);
			var stops = arrival.numberOfStopsAway;

			latestUpdate = latestUpdate ? Math.max(latestUpdate, updateTime) : updateTime;

			var vehicleInfo = {stops: stops,
								feet: feet};

			if (routeToVehicleInfo[routeId]) {
				routeToVehicleInfo[routeId].push(vehicleInfo);
			}
		});

		var lastUpdateString = null;        
		if (latestUpdate !== null && latestUpdate !== 0) {
			var lastUpdateDate = new Date(latestUpdate);
			lastUpdateString = OBA.Util.displayTime(lastUpdateDate);
		}

		// header
		var header = '<p class="header">' + name + '</p>' +
					'<p><span class="type stop">Stop #' + OBA.Util.parseEntityId(stopId) + '</span> ' + 
					(lastUpdateString ? '<span class="updated">Last updated at ' + lastUpdateString + '</span>' : '') + '</p>';

		// service notices
		var notices = '<ul class="notices">';

		jQuery.each(refs.situations, function(_, situation) {
			var situationId = situation.id;
			if(situationId in applicableSituationIds) {
				notices += '<li>' + situation.description.value + '</li>';
			}
		});

		notices += '</ul>';

		// service
		var service = "";
		if(routeToVehicleCount === 0) {
			service += '<p class="service">No upcoming service is available at this stop.</p>';
		} else {
			service += '<p class="service">This stop is served by:</p><ul>';
	
			jQuery.each(routeToVehicleInfo, function(routeId, vehicleInfos) {
				var headsign = routeToHeadsign[routeId];
	
				service += '<li class="route">';
				service += headsign;
				service += '</li>';
		
				// sort based on distance
				vehicleInfos.sort(function(a, b) { return a.feet - b.feet; });
	
				for (var i = 0; i < Math.min(vehicleInfos.length, 3); i++) {
					var distanceAway = vehicleInfos[i];
	
					if(distanceAway.feet < 0 || distanceAway.stops > OBA.Config.vehicleDisplayMaxStopsAway) {
						continue;
					}
					
					service += '<li class="arrival">';
					
					if(distanceAway.feet <= OBA.Config.atStopThresholdInFeet) {
						service += "<span>at stop</span>";
					} else {
						if(distanceAway.stops === 0) {
							service += "<span>< 1 stop</span>";
							service += "<span>" + OBA.Util.displayDistance(distanceAway.feet, distanceAway.stops) + "</span>";
						} else {
							service += "<span>" + distanceAway.stops + " stop" + ((distanceAway.stops === 1) ? "" : "s") + "</span>";
							service += "<span>" + OBA.Util.displayDistance(distanceAway.feet, distanceAway.stops) + "</span>";
						}
					}
					
					service += '</li>';
				}
			});
	
			service += '</ul>';
		}
		
		// footer
		var footer = OBA.Config.infoBubbleFooterFunction("stop", OBA.Util.parseEntityId(stopId));
		
		return jQuery(header + notices + service + footer);
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

		var lastUpdateDate = new Date(tripStatus.lastUpdateTime);
		var lastUpdateString = OBA.Util.displayTime(lastUpdateDate);
		var isStaleData = (new Date().getTime() - tripStatus.lastUpdateTime >= 1000 * OBA.Config.staleDataTimeout);

		// header
		var header = '<p class="header">' + headsign + '</p>' +
					'<p><span class="type vehicle">Bus #' + OBA.Util.parseEntityId(vehicleId) + '</span> ' +
					'<span class="updated' + ((isStaleData === true) ? " stale" : "") +'">Last updated at ' + lastUpdateString + '</span></p>';

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

		if (nextStops !== null && nextStops.length > 0 && 
				(typeof tripStatus.status !== 'undefined' && tripStatus.status !== 'deviated')) { 
			
			nextStopsMarkup += '<p class="service">Next stops:</p><ul>';

			jQuery.each(nextStops, function(i, stop) {
				var displayStopId = OBA.Util.parseEntityId(stop.id);
				var stopName = stop.name;

				nextStopsMarkup += '<li class="nextStop">';
				nextStopsMarkup += stopName;

				// we only have one stop currently this will not work if we get more than one
				// because we reuse the distance information for each
				var stopsAway = i;
				var feetAway = OBA.Util.metersToFeet(stop.scheduledDistance - distanceAlongTrip);
								
				if(feetAway <= OBA.Config.atStopThresholdInFeet) {
					nextStopsMarkup += "<span>at stop</span>";
				} else {
					if(stopsAway === 0) {
						nextStopsMarkup += "<span>" + OBA.Util.displayDistance(feetAway, 0) + "</span>";
					} else {
						nextStopsMarkup += "<span>" + OBA.Util.displayDistance(feetAway, stopsAway) + "</span>";
					}
				}

				nextStopsMarkup += '</li>';
			});

			nextStopsMarkup += '</ul>';
		} else {
			nextStopsMarkup += '<p class="service">Next stops are unknown for this vehicle.</p>';
		}

		// footer
		var footer = OBA.Config.infoBubbleFooterFunction("route", OBA.Util.parseEntityId(route.id));
		
		return jQuery(header + notices + nextStopsMarkup + footer);
	};

	var url = OBA.Config.vehicleUrl + "/" + vehicleId + ".json";
	return OBA.Popup(
			map,
			makeJsonFetcher(url, {key: OBA.Config.apiKey, version: 2, includeSchedule: true, includeTrip: true}),
			generateVehicleMarkup);
};
