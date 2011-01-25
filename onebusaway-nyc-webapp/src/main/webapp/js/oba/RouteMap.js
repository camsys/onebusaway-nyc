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

OBA.RouteMap = function(mapNode, mapOptions) {
	var transitMapType = new google.maps.ImageMapType({
		getTileUrl: function(coord, zoom) {
			return 'http://mt1.google.com/vt/lyrs=m@132,transit|vm:1&hl=en&opts=r&x=' + coord.x + '&y=' + coord.y + '&z=' + zoom + '&s=Galileo'; 
		},
		tileSize: new google.maps.Size(256, 256),
		opacity:1.0,
		maxZoom: 17,
		minZoom: 12,
		name: 'Transit',
		isPng: true,
		alt: ''
	});

	var defaultMapOptions = {
		zoom: 12,
		mapTypeControl: false,
		navigationControlOptions: { style: google.maps.NavigationControlStyle.SMALL },
		center: new google.maps.LatLng(40.65182926199445,-74.0065026164856),
		streetViewControl: false,		
		mapTypeId: 'transit'
	};

	var options = jQuery.extend({}, defaultMapOptions, mapOptions || {});
	var map = new google.maps.Map(mapNode, options);

	// hide any open popups if user drags/zooms the owning marker outside of map viewport
	var dismissPopups = function() {
		if(OBA.popupMarker !== null) {
			var mapBounds = map.getBounds();
			var markerPosition = OBA.popupMarker.getPosition();
			if(markerPosition !== null && !mapBounds.contains(markerPosition)) {
				var markerPopup = OBA.popupMarker.getPopup();			
				if(markerPopup !== null) {
					markerPopup.hide();
				}
			}
		}
	};
	google.maps.event.addListener(map, 'dragend', dismissPopups); 
	google.maps.event.addListener(map, 'zoom_changed', dismissPopups); 

	map.mapTypes.set('transit',transitMapType);

	// state used for the map
	var routeIds = {};
	var routeIdToShapes = {};
	var routeIdsToVehicleMarkers = {};
	var vehicleMarkers = {};
	var stopMarkers = {};
	var numberOfRoutes = 0;
	var isVehiclePolling = false;
	var vehicleTimerId = null;

	var fluster = new Fluster2(map, false, stopMarkers);
	fluster.initialize();            

	var requestRoutes = function() {
		jQuery.each(routeIds, function(routeId, directionIds) {
			if(typeof directionIds !== 'object') {
				directionIds = [directionIds];
			}
			
			if (directionIds.length === 0) {
				return;
			}
		
			var url = OBA.Config.vehiclesUrl + "/" + routeId + ".json";
			var tripDetailsList, tripReferencesList = null;
			var params = {version: 2, key: OBA.Config.apiKey, includeStatus: true};
			
			if(typeof OBA.Config.time !== 'undefined' && OBA.Config.time !== null) {
				params.time = OBA.Config.time * 1000;
			}
			
			jQuery.getJSON(url, params, function(json) {
				try {
					tripDetailsList = json.data.list;
					tripReferencesList = json.data.references.trips;
				} catch (typeError) {
					OBA.Util.log("unknown server vehicles response");
					OBA.Util.log(json);
					return;
				}

				// keep track of the vehicles that are added for this direction
				// this is will be a set of vehicleIds
				var vehiclesAdded = {};

				// helper function to add an element to a map where values are lists
				var addVehicleMarkerToRouteMap = function(routeId, directionId, vehicleMarker) {
					var directionIdMap = routeIdsToVehicleMarkers[routeId];

					if (directionIdMap) {
						var vehicles = directionIdMap[directionId];

						if (vehicles) {
							var alreadyThere = false;
							var markerId = vehicleMarker.getId();

							jQuery.each(vehicles, function(i, vehicle) {
								if (vehicle.getId() === markerId) {
									alreadyThere = true;
									return;
								}
							});

							if (!alreadyThere) {
								vehicles.push(vehicleMarker);
							}
						} else {
							directionIdMap[directionId] = [vehicleMarker];
						}
					} else {
						var directionIdMap = {};
						directionIdMap[directionId] = [vehicleMarker];
						routeIdsToVehicleMarkers[routeId] = directionIdMap;
					}

					// keep track of all vehicles that have been added
					// so we know which we should remove from the list
					vehiclesAdded[vehicleMarker.getId()] = true;
				};

				jQuery.each(tripDetailsList, function(i, tripDetails) {
					var status = tripDetails.status;
					var vehicleId = status.vehicleId;

					if(vehicleId === null || status === null || 
							OBA.Config.vehicleFilterFunction("vehicle", status) === false) {
						return;
					}
				
					// check to make sure that the bus is headed in the right direction
					var vehicleDirectionId = null;
					var vehicleTripId = tripDetails.tripId;
					for (var i = 0; i < tripReferencesList.length; i++) {
						var tripReference = tripReferencesList[i];					
						if (tripReference.id === vehicleTripId) {
							var tripReferenceDirectionId = tripReference.directionId;
							jQuery.each(directionIds, function(requestDirectionId, _) {
								if(tripReferenceDirectionId === requestDirectionId) {
									vehicleDirectionId = tripReferenceDirectionId;
									return;
								}
							});

							break;
						}						
					}

					// this vehicle is not on a trip in the proper direction; skip
					if(vehicleDirectionId === null) {
						return;
					}					
					// if we're here, we're adding this vehicle to the map...

					var latLng = [status.position.lat, status.position.lon];
					var vehicleMarker = vehicleMarkers[vehicleId];

					if (vehicleMarker) {
						var latlng = new google.maps.LatLng(latLng[0], latLng[1]);

						vehicleMarker.updatePosition(latlng);
						vehicleMarker.updateOrientation(status.orientation);

						if (!vehicleMarker.isDisplayed()) {
							vehicleMarker.addMarker();
						}

						addVehicleMarkerToRouteMap(routeId, vehicleDirectionId, vehicleMarker);
					} else {
						vehicleMarker = OBA.VehicleMarker(vehicleId, latLng, status.orientation, map, { map: map });
						vehicleMarkers[vehicleId] = vehicleMarker;

						addVehicleMarkerToRouteMap(routeId, vehicleDirectionId, vehicleMarker);
					}
				}); // each tripDetail
				
				// remove vehicle markers on this route that haven't been listed in this recent update
				var directionIdMap = routeIdsToVehicleMarkers[routeId];
				if (directionIdMap) {
					jQuery.each(directionIdMap, function(directionId, vehicles) {
						if(vehicles === null) {
							return;
						}						
						var newVehicleList = [];
						for (var i = 0; i < vehicles.length; i++) {
							var vehicle = vehicles[i];
							if (!(vehicle.getId() in vehiclesAdded)) {
								vehicle.removeMarker();
							} else {
								newVehicleList.push(vehicle);
							}
						}									
						routeIdsToVehicleMarkers[routeId][directionId] = newVehicleList;
					});
				}				
			}); // getJSON
		}); // each routeIdsToRequest
	};

	var vehiclePollingTask = function() {
		if (!isVehiclePolling) {
			return;
		}

		requestRoutes();

		// refresh any open popups
		if(OBA.popupMarker !== null) {
			OBA.popupMarker.refreshPopup();
		}
		
		vehicleTimerId = setTimeout(vehiclePollingTask, OBA.Config.pollingInterval);
	};

	var requestStops = function() {
		// calculate the request lat/lon and spans to use for the request
		var mapBounds = map.getBounds();
		var minLatLng = mapBounds.getSouthWest(), centerLatLng = mapBounds.getCenter();
		var latSpan = Math.abs(centerLatLng.lat() - minLatLng.lat()) * 2;
		var lonSpan = Math.abs(centerLatLng.lng() - minLatLng.lng()) * 2;

		jQuery.getJSON(OBA.Config.stopsUrl, {version: 2, key: OBA.Config.apiKey, maxCount: 250,
			lat: centerLatLng.lat(), lon: centerLatLng.lng(), latSpan: latSpan, lonSpan: lonSpan},
				function(json) {
					var stops;
					try {
						stops = json.data.list;
					} catch (typeError) {
						OBA.Util.log("invalid response from server: ");
						OBA.Util.log(json);
						return;
					}

					// keep track of the new ids that came in so we can remove the old ones
					// that are no longer shown
					var newStopIds = {};

					jQuery.each(stops, function(i, stop) {
						var stopId = stop.id;
						var direction = stop.direction;
						var latlng = [stop.lat, stop.lon];

						newStopIds[stopId] = stopId;

						var marker = stopMarkers[stopId];

						if (! marker) {
							marker = OBA.StopMarker(stopId, latlng, direction, map);

							fluster.addMarker(marker);
							stopMarkers[stopId] = marker;
						}
					});

					fluster.refresh();
				});
	};

	google.maps.event.addListener(map, "idle", requestStops);   

	var containsRoute = function(routeId, directionId) {
		var directionIdMap = routeIds[routeId] || {};
		return directionId in directionIdMap;
	};

	return {
		containsRoute: containsRoute,

		getMap: function() { return map; },

		showStop: function(stopId) {   
			var mapBounds = map.getBounds();

			if (stopMarkers[stopId]) {
				// stop marker is already on map, can just display the popup
				var marker = stopMarkers[stopId];
				marker.setMap(map);

				if(! mapBounds.contains(marker.getPosition())) {
					map.setCenter(marker.getPosition());
				}

				marker.showPopup();
			} else {
				var url = OBA.Config.stopUrl + "/" + stopId + ".json";
				var params = {version: 2, key: OBA.Config.apiKey, minutesBefore: OBA.Config.arrivalsMinutesBefore, 
						minutesAfter: OBA.Config.arrivalsMinutesAfter};
				
				if(typeof OBA.Config.time !== 'undefined' && OBA.Config.time !== null) {
					params.time = OBA.Config.time * 1000;
				}
				
				jQuery.getJSON(url, params, function(json) {
					var stop = null;
					try {
						stop = json.data.references.stops[0];
					} catch (typeError) {
						OBA.Util.log("invalid stop response from server");
						return;
					}

					var stopId = stop.id;
					var latlng = [stop.lat, stop.lon];
					var direction = stop.direction;
					var marker = OBA.StopMarker(stopId, latlng, direction, map);
					marker.setMap(map);

					fluster.addMarker(marker);
					stopMarkers[stopId] = marker;

					if(! mapBounds.contains(marker.getPosition())) {
						map.setCenter(marker.getPosition());
					}

					marker.showPopup();
				});
			}
		},

		addRoute: function(routeId, directionIds, successFn) {
			if(typeof directionIds !== 'object') {
				directionIds = [directionIds];
			}
			
			var url = OBA.Config.routeShapeUrl + "/" + routeId + ".json";
			jQuery.getJSON(url, {version: 2, key: OBA.Config.apiKey}, function(json) {
				jQuery.each(directionIds, function(_, directionId) {
					if (containsRoute(routeId, directionId)) {
						return;
					}
			
					var shape;
					var stopGroupings = json.data.entry.stopGroupings;
					var directionStopGrouping = null;
					jQuery.each(stopGroupings, function(_, stopGrouping) {
						if (stopGrouping.type === "direction") {
							directionStopGrouping = stopGrouping;
						}
					});
					if (directionStopGrouping === null) {
						OBA.Util.log("Could not find direction stop grouping");
						OBA.Util.log(json);
						return;
					}
					var stopGroups = directionStopGrouping.stopGroups;
					shape = null;
					for ( var i = 0; i < stopGroups.length; i++) {
						var stopGroup = stopGroups[i];
						if (stopGroup.id === directionId) {
							shape = stopGroup.polylines;
							break;
						}
					}
					if (shape === null) {
						OBA.Util.log("Could not find shape for route direction: " + directionId);
						OBA.Util.log(json);
						return;
					}

					// polylines is a list of encoded polylines from the server
					// here we decode them and put them into one list of lat lngs
					var polylines = [];
					jQuery.each(shape, function(_, encodedPolyline) {
						var latlngs = OBA.Util.decodePolyline(encodedPolyline.points);
						var googleLatLngs = jQuery.map(latlngs, function(latlng) {
							return new google.maps.LatLng(latlng[0], latlng[1]);
						});

						var polyline = new google.maps.Polyline({
							path: googleLatLngs,
							strokeColor: "#0000FF",
							strokeOpacity: 0.5,
							strokeWeight: 5,
							zIndex: 50
						});

						polyline.setMap(map);
						polylines.push(polyline);
					});

					var directionIdToShapes = routeIdToShapes[routeId];
					if (!directionIdToShapes) {
						directionIdToShapes = {};
						routeIdToShapes[routeId] = directionIdToShapes;
					}
					directionIdToShapes[directionId] = polylines;
					numberOfRoutes += 1;

					// update the timer task
					if (!isVehiclePolling) {
						isVehiclePolling = true;
						vehicleTimerId = setTimeout(vehiclePollingTask, OBA.Config.pollingInterval);
					}

					var directionIdMap = routeIds[routeId];
					if (!directionIdMap) {
						directionIdMap = {};
						routeIds[routeId] = directionIdMap;
					}
					directionIdMap[directionId] = 1;

					if(typeof successFn === 'function') {
						successFn(routeId, directionId);
					}
				}); // each directionId
				
				requestRoutes();
			}); // getJSON
		},

		removeRoute: function(routeId, directionIds) {
			var directionIdMap = routeIdToShapes[routeId] || {};

			if(typeof directionIds !== 'object') {
				directionIds = [directionIds];
			}
			
			jQuery.each(directionIds, function(_, directionId) {
				var polylines = directionIdMap[directionId];

				if (polylines) {
					delete directionIdMap[directionId];
					numberOfRoutes -= 1;
					jQuery.each(polylines, function(_, polyline) {
						polyline.setMap(null);
					});
				}

				var vehicleDirectionMap = routeIdsToVehicleMarkers[routeId] || {};
				var vehicles = vehicleDirectionMap[directionId];

				if (vehicles) {
					jQuery.each(vehicles, function(i, vehicleMarker) {
						vehicleMarker.removeMarker();
					});

					delete vehicleDirectionMap[directionId];
				}

				if (numberOfRoutes <= 0) {
					isVehiclePolling = false;
					if (vehicleTimerId) {
						clearTimeout(vehicleTimerId);
					}
				}

				var directionIdMap = routeIds[routeId];
				if (directionIdMap) {
					delete directionIdMap[directionId];
				}
			});
		},

		getCount: function() {
			return numberOfRoutes;
		},

		getBounds: function(routeId, directionId) {
			var directionIdMap = routeIdToShapes[routeId] || {};
			var polylines = directionIdMap[directionId];

			if (!polylines) { 
				return null; 
			}

			var latlngBounds = new google.maps.LatLngBounds();

			jQuery.each(polylines, function(_, polyline) {
				var path = polyline.getPath();
				for (var i = 0; i < path.length; i++) {
					var latlng = path.getAt(i);
					latlngBounds.extend(latlng);
				}
			});

			return latlngBounds;
		}
	};
};
