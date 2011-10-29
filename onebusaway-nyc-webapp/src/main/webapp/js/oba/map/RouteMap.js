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

OBA.RouteMap = function(mapNode, mapMoveCallbackFn) {	
	var mtaMapType = new google.maps.ImageMapType({
		getTileUrl: function(coord, zoom) {
			if(!(zoom >= this.minZoom && zoom <= this.maxZoom)) {
				return null;
			}

			var quad = ""; 
		    for (var i = zoom; i > 0; i--){
		        var mask = 1 << (i - 1); 
		        var cell = 0; 
		        if ((coord.x & mask) != 0) 
		            cell++; 
		        if ((coord.y & mask) != 0) 
		            cell += 2; 
		        quad += cell; 
		    } 
			return 'http://tripplanner.mta.info/maps/SystemRoutes_New/' + quad + '.png'; 
		},
		tileSize: new google.maps.Size(256, 256),
		opacity: 0.5,
		maxZoom: 15,
		minZoom: 14,
		name: 'MTA Subway Map',
		isPng: true,
		alt: ''
	});

	var mutedTransitStylesArray = 
		[{
			featureType: "road.arterial",
			elementType: "geometry",
			stylers: [
			          { saturation: -100 },
			          { lightness: 100 },
			          { visibility: "simplified" },
			          { hue: "#ffffff" }
			          ]
		},{
			featureType: "road.highway",
			elementType: "geometry",
			stylers: [
			          { saturation: -80 },
			          { lightness: 60 },
			          { visibility: "on" },
			          { hue: "#0011FF" }
			          ]
		},{
			featureType: "road.local",
			elementType: "geometry",
			stylers: [
			          { saturation: 0 },
			          { lightness: 100 },
			          { visibility: "on" },
			          { hue: "#ffffff" }
			          ]
		},{
			featureType: "road.arterial",
			elementType: "labels",
			stylers: [
			          { lightness: 25 },
			          { saturation: -25 },
			          { visibility: "off" },
			          { hue: "#ddff00" }
			          ]
		},{
			featureType: "road.highway",
			elementType: "labels",
			stylers: [
			          { lightness: 60 },
			          { saturation: -70 },
			          { hue: "#0011FF" },
			          { visibility: "on" }
			          ]
		},{ 
			featureType: "administrative.locality", 
			elementyType: "labels",
			stylers: [ { visibility: "on" }, 
			           { lightness: 50 },
			           { saturation: -80 }, 
			           { hue: "#ffff00" } ] 
		},{ 
			featureType: "administrative.neighborhood", 
			elementyType: "labels",
			stylers: [ { visibility: "on" }, 
			           { lightness: 50 },
			           { saturation: -80 }, 
			           { hue: "#ffffff" } ] 
		},{
			featureType: 'landscape',
			elementType: 'labels',
			stylers: [ {'visibility': 'on'},
			           { lightness: 50 },
			           { saturation: -80 },
			           { hue: "#0099ff" }
			           ]
		},{
			featureType: 'poi',
			elementType: 'labels',
			stylers: [ {'visibility': 'on'},
			           { lightness: 50 },
			           { saturation: -80 },
			           { hue: "#0099ff" }
			           ]
		},{
			featureType: 'water',
			elementType: 'labels',
			stylers: [ {'visibility': 'off'}
			]
		}];

	var transitStyledMapType = 
		new google.maps.StyledMapType(mutedTransitStylesArray, {name: "Transit"});

	var defaultMapOptions = {
			zoom: 11,
			mapTypeControl: true,
			streetViewControl: false,
			zoomControl: true,
			zoomControlOptions: {
				style: google.maps.ZoomControlStyle.LARGE
			},
			minZoom: 11, 
			maxZoom: 19,
			navigationControlOptions: { style: google.maps.NavigationControlStyle.DEFAULT },
			center: new google.maps.LatLng(40.639228,-74.081154),
			mapTypeControlOptions: {
				mapTypeIds: [ google.maps.MapTypeId.ROADMAP, "Transit" ]
			}
	};

	var map = null;
	var mgr = null;
	var infoWindow = null;

	var vehiclesByRoute = {};
	var vehiclesById = {};
	var polylinesByRouteAndDirection = {};
	var stopsAddedForRouteAndDirection = {};
	var updateFunctionsByRoute = {};
	var stopsById = {};

	// POPUPS
	
	// create a popup with content from the named URL+params, from the contentFn specified.
	// the bubble will refresh itself when the map is also refreshed.
	function showPopupWithContentFromRequest(marker, url, params, contentFn) {
		var popupOptions = {
    		content: "Loading...",
    		pixelOffset: new google.maps.Size(0, (marker.getIcon().size.height / 2))
    	};

		// only one open at a time!
		var closeFn = function() {
			if(infoWindow !== null) {
				infoWindow.close();
				infoWindow = null;
			}
		};
		closeFn();
		
    	infoWindow = new google.maps.InfoWindow(popupOptions);    	
    	infoWindow.open(map, marker);

    	google.maps.event.addListener(infoWindow, "closeclick", closeFn);
    	
    	// called to refresh the bubble's content
    	var refreshFn = function() {
    		jQuery.getJSON(url, params, function(json) {
    			infoWindow.setContent(contentFn(json));
    		});
    	};
    	infoWindow.refreshFn = refreshFn;
    	refreshFn();
	}
	
	function getVehicleContentForResponse(r) {
		var vehicle = r.vehicleLocation;
		
		if(vehicle === null) {
			return null;
		}
		
		var html = '<div id="popup">';
		
		// header
		html += ' <div class="header vehicle">';
		html += '  <p class="title">' + vehicle.routeIdWithoutAgency + " " + vehicle.headsign + '</p><p>';
		html += '   <span class="type">Vehicle #' + vehicle.vehicleIdWithoutAgency + '</span>';

		// update time across all arrivals
		var age = null;
		jQuery.each(vehicle.nextStops, function(_, nextStop) {
			var thisAge = Math.floor((nextStop.distanceAway.updateTimestampReference - nextStop.distanceAway.updateTimestamp) / 1000);
			if(thisAge > age) {
				age = thisAge;
			}
		});
		if(age !== null) {
			html += '   <span class="updated">Last updated ' + age + ' seconds ago</span>'; 
		}
		
		// (end header)
		html += '  </p>';
		html += ' </div>';
		
		// service available
		html += '<p class="service">Next stops:</p>';
		html += '<ul>';
		jQuery.each(vehicle.nextStops, function(_, nextStop) {
			html += '<li class="nextStop">' + nextStop.name + ' <span>';
			html += nextStop.distanceAway.presentableDistance;
			html += '</span></li>';
		});
		html += '</ul>';

		// (end popup)
		html += '</div>';
		
		return html;
	}
	
	function getStopContentForResponse(r) {
		var stopResult = r.result;
		
		if(stopResult === null) {
			return null;
		}
		
		var html = '<div id="popup">';
		
		// header
		html += ' <div class="header stop">';
		html += '  <p class="title">' + stopResult.name + '</p><p>';
		html += '   <span class="type">Stop #' + stopResult.stopIdWithoutAgency + '</span>';

		// update time across all arrivals
		var age = null;
		jQuery.each(stopResult.routesAvailable, function(_, route) {
			jQuery.each(route.destinations, function(__, destination) {
				jQuery.each(destination.distanceAways, function(arrivalCount, distanceAway) {
					var thisAge = Math.floor((distanceAway.updateTimestampReference - distanceAway.updateTimestamp) / 1000);
					if(thisAge > age) {
						age = thisAge;
					}
				});
			});
		});
		if(age !== null) {
			html += '   <span class="updated">Last updated ' + age + ' seconds ago</span>'; 
		}
		
		// (end header)
		html += '  </p>';
		html += ' </div>';
		
		// service alerts
		html += '<ul>';
		jQuery.each(stopResult.routesAvailable, function(_, route) {
			jQuery.each(route.destinations, function(__, destination) {
				jQuery.each(destination.serviceAlerts, function(___, alert) {
					html += "<li>" + alert + "</li>";
				});
			});
		});
		html += '</ul>';
		
		// service available
		html += '<p class="service">This stop is served by:</p>';
		html += '<ul>';
		jQuery.each(stopResult.routesAvailable, function(_, route) {
			jQuery.each(route.destinations, function(__, destination) {
				html += '<li class="route">' + route.routeIdWithoutAgency + ' ' + destination.headsign + '</li>';

				jQuery.each(destination.distanceAways, function(arrivalCount, distanceAway) {
					html += '<li class="arrival">' + distanceAway.presentableDistance + '</li>';
				});
			});
		});
		html += '</ul>';

		// (end popup)
		html += '</div>';
		
		return html;
	}
	
	// POLYLINE
	function removePolylines(routeId, directionId) {
		if(typeof polylinesByRouteAndDirection[routeId + "|" + directionId] !== 'undefined') {
			var polylines = polylinesByRouteAndDirection[routeId + "|" + directionId];

			jQuery.each(polylines, function(_, polyline) {
				polyline.setMap(null);
			});
			
			delete polylinesByRouteAndDirection[routeId + "|" + directionId];
		}
	}
	
	function addPolylines(routeId, directionId, encodedPolylines, color) {
		if(typeof polylinesByRouteAndDirection[routeId + "|" + directionId] !== 'undefined') {
			return;
		}

		var polylines = [];
		jQuery.each(encodedPolylines, function(_, encodedPolyline) {
			var points = OBA.Util.decodePolyline(encodedPolyline);
		
			var latlngs = jQuery.map(points, function(x) {
				return new google.maps.LatLng(x[0], x[1]);
			});

			var shape = new google.maps.Polyline({
				path: latlngs,
				strokeColor: "#" + color,
				strokeOpacity: 1.0,
				strokeWeight: 5,
				map: map
			});
		
			polylines.push(shape);
		});	

		polylinesByRouteAndDirection[routeId + "|" + directionId] = polylines;
	}

	// STOPS
	function removeStops(routeId, directionId) {
		if(typeof stopsAddedForRouteAndDirection[routeId + "|" + directionId] !== 'undefined') {
			var stops = stopsAddedForRouteAndDirection[routeId + "|" + directionId];
			
			jQuery.each(stops, function(_, marker) {
				var stopId = marker.stopId;
				
				delete stopsById[stopId];				
				mgr.removeMarker(marker);
				marker.setMap(null);
			});
			
			delete stopsAddedForRouteAndDirection[routeId + "|" + directionId];
		}		
	}
	
	function addStops(routeId, directionId, stopItems) {
		// already on map?
		if(typeof stopsAddedForRouteAndDirection[routeId + "|" + directionId] !== 'undefined') {
			return;
		}

		jQuery.each(stopItems, function(_, stop) {
			if(typeof stopsById[stop.stopId] !== 'undefined') {
				return;
			}
			
			var directionKey = stop.stopDirection;
			if(directionKey === null) {
				directionKey = "unknown";
			}
			
			var icon = new google.maps.MarkerImage("img/stop/stop-" + directionKey + ".png",
                new google.maps.Size(21, 21),
                new google.maps.Point(0,0),
                new google.maps.Point(10, 10));
			
			var markerOptions = {
				position: new google.maps.LatLng(stop.latitude, stop.longitude),
	            icon: icon,
	            zIndex: 1,
	            title: stop.name,
	            stopId: stop.stopId
			};

	        var marker = new google.maps.Marker(markerOptions);
	        
	    	google.maps.event.addListener(marker, "click", function(mouseEvent) {
	    		showPopupWithContentFromRequest(this, OBA.Config.searchResultForStopId, 
	    				{ stopId: stop.stopId }, getStopContentForResponse);
	    	});

	    	// FIXME: route zoom level configuration?
	    	mgr.addMarker(marker, 16, 19);
	    	
	        stopsById[stop.stopId] = marker;
		});
		
		stopsAddedForRouteAndDirection[routeId + "|" + directionId] = true;
	}
	
	// VEHICLES
	function updateVehicles(routeId) {		
		jQuery.getJSON(OBA.Config.vehicleLocationsForRouteId, { routeId: routeId }, 
		function(json) {
			var vehicles = [];
			jQuery.each(json.vehicleLocations, function(_, vehicle) {
				var vehicleId = vehicle.vehicleId;
				var latitude = vehicle.latitude;
				var longitude = vehicle.longitude;
				var orientation = vehicle.orientation;
				var headsign = vehicle.headsign;
				var routeIdWithoutAgency = vehicle.routeIdWithoutAgency;
				
				var marker = vehiclesById[vehicleId];

				// create marker if it doesn't exist				
				if(typeof marker === 'undefined' || marker === null) {
					var markerOptions = {
				            zIndex: 2,
							map: map,
							title: routeIdWithoutAgency + " " + headsign,
							vehicleId: vehicleId
					};

					marker = new google.maps.Marker(markerOptions);
			        
			    	google.maps.event.addListener(marker, "click", function(mouseEvent) {
			    		showPopupWithContentFromRequest(this, OBA.Config.vehicleLocationWithNextStopsForVehicleId, 
			    				{ vehicleId: vehicleId }, getVehicleContentForResponse);
			    	});
				}

				// icon
				var orientationAngle = "unknown";
				if(orientation !== null && orientation !== 'NaN') {
					orientationAngle = Math.floor(orientation / 5) * 5;
				}
					
				var icon = new google.maps.MarkerImage("img/vehicle/vehicle-" + orientationAngle + ".png",
						new google.maps.Size(51, 51),
						new google.maps.Point(0,0),
						new google.maps.Point(25, 25));

				marker.setIcon(icon);

				// position
				var position = new google.maps.LatLng(latitude, longitude);
				marker.setPosition(position);
							    	
				vehicles.push(marker);
				vehiclesById[vehicleId] = marker; 
			});
			
			// remove vehicles from map that are no longer in the response
			var vehiclesCurrentlyOnMap = vehiclesByRoute[routeId];
			if(typeof vehiclesCurrentlyOnMap !== 'undefined') {
				jQuery.each(vehiclesCurrentlyOnMap, function(__, vehicleOnMap) {
					var isStillPresent = false;
					jQuery.each(vehicles, function(___, vehicleInResponse) {
						if(vehicleOnMap.vehicleId === vehicleInResponse.vehicleId) {
							isStillPresent = true;
							return false;
						}
					});
				
					if(!isStillPresent) {
						vehicleOnMap.setMap(null);
						delete vehiclesById[vehicleOnMap.vehicleId];
					}
				});
			}
			
			vehiclesByRoute[routeId] = vehicles;
		});
	}
	
	function removeVehicles(routeId) {
		if(typeof vehiclesByRoute[routeId] !== 'undefined') {
			var vehicles = vehiclesByRoute[routeId];
			
			jQuery.each(vehicles, function(_, marker) {
				var vehicleId = marker.vehicleId;
				
				marker.setMap(null);
				delete vehiclesById[vehicleId];
			});

			delete vehiclesByRoute[routeId];

			delete updateFunctionsByRoute[routeId];
		};
	};
	
	// MISC
	function removeRoutesNotInSet(routeResults) {
		// remove routes not shown anymore
		for(key in polylinesByRouteAndDirection) {
			if(key === null) {
				continue;
			}
			
			var keyParts = key.split("|");
			var routeAndAgencyId = keyParts[0];
			var directionId = keyParts[1];

			// don't remove the routes we just added!
			var removeMe = true;
			jQuery.each(routeResults, function(_, result) {
				if(routeAndAgencyId === result.routeId) {
					removeMe = false;
					return false;
				}				
			});
			
			if(removeMe) {			
				removePolyline(routeAndAgencyId, directionId);
				removeStops(routeAndAgencyId, directionId);
				removeVehicles(routeAndAgencyId);
			}
		}		
	}
		
	//////////////////// CONSTRUCTOR /////////////////////
	map = new google.maps.Map(mapNode, defaultMapOptions);
	mgr = new MarkerManager(map);

	// mta custom tiles
	map.overlayMapTypes.insertAt(0, mtaMapType);

	// styled basemap
	map.mapTypes.set('Transit', transitStyledMapType);
	map.setMapTypeId('Transit');
	
	// request list of routes in viewport when user stops moving map
	if(typeof mapMoveCallbackFn === 'function') {
		google.maps.event.addListener(map, "idle", mapMoveCallbackFn);
	}

	// timer to update periodically
	setInterval(function() {
		jQuery.each(updateFunctionsByRoute, function(key, updateFn) {
			updateFn();
		});

		if(infoWindow !== null) {
			infoWindow.refreshFn();
		}
	}, OBA.Config.refreshInterval);

	return {
		// get map viewport
		getBounds: function() {
			return map.getBounds();
		},
		
		removeAllRoutes: function() {
			removeRoutesNotInSet({});
		},
		
		removeRoutesNotInSet: removeRoutesNotInSet,
		
		// add route, route's stops, and route's vehicles to map
		showRoute: function(route) {
			// add both destinations for route to map
			jQuery.each(route.destinations, function(_, destination) {
				addPolylines(route.routeId, destination.directionId, destination.polylines, route.color);
				addStops(route.routeId, destination.directionId, destination.stops);
			});

			// add refresh function to update list
			if(typeof updateFunctionsByRoute[route.routeId] === 'undefined') {				
				updateVehicles(route.routeId);
				
				updateFunctionsByRoute[route.routeId] = function() {
					updateVehicles(route.routeId);
				};
			}
		},

		// move map to given location
		showLocation: function(lat, lng) {
			var location = new google.maps.LatLng(lat, lng);
			map.panTo(location);
			map.setZoom(16);
		}
	};
};
