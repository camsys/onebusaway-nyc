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
			if(!(zoom >= this.minZoom && zoom <= this.maxZoom))
				return null;
			
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
	
	// Make another map using the Google Transit base map (for comparison)
	var GTransitMapType = new google.maps.ImageMapType({
			getTileUrl: function(coord, zoom) {
				return 'http://mt1.google.com/vt/lyrs=m@132,transit|vm:1&hl=en&opts=r&x=' + coord.x + '&y=' + coord.y + '&z=' + zoom + '&s=Galileo'; 
			},
			tileSize: new google.maps.Size(256, 256),
			opacity:1.0,
			maxZoom: 17,
			minZoom: 11,
			name: 'Google',
			isPng: true,
			alt: '' });

	var defaultMapOptions = {
			zoom: 11,
			mapTypeControl: true,
			streetViewControl: false,
			zoomControl: true,
			zoomControlOptions: {
				style: google.maps.ZoomControlStyle.LARGE
			},
			minZoom: 9, 
			maxZoom: 19,
			navigationControlOptions: { style: google.maps.NavigationControlStyle.DEFAULT },
			center: new google.maps.LatLng(40.639228,-74.081154),
			mapTypeControlOptions: {
				mapTypeIds: [ "Google", "Transit" ]
			}
	};

	var map = null;
	var markersArray = [];
	var vehiclesByRoute = {};
	var vehiclesById = {};
	var vehicleUpdateTimersByRoute = {};
	var polylinesByRouteAndDirection = {};
	var stopsAddedForRouteAndDirection = {};
	var stopsById = {};
	var infoWindow = new google.maps.InfoWindow({});
	
	// create a popup with content from the named URL+params, from the contentFn specified.
	// the bubble will refresh itself when the map is also refreshed.
	function showPopupWithContentFromRequest(marker, url, params, contentFn) {
		var popupOptions = {
    		content: "Loading...",
    		pixelOffset: new google.maps.Size(0, (marker.getIcon().size.height / 2))
    	};
    		
		if(infoWindow !== null) {
			infoWindow.close();
		}
		
    	infoWindow.setContent(popupOptions);    	
    	infoWindow.open(map, marker);

    	var refreshFn = function() {
    		jQuery.getJSON(url, params, function(json) {
    			infoWindow.setContent(contentFn(json));
    		});
    	};
    	refreshFn();

    	infoWindow.refreshFn = refreshFn;
	}
	
	// create a popup with location information, such as during disambiguation
	function showPopupWithLocationInformation(marker) {    		
		if (infoWindow !== null) {
			infoWindow.close();
		}
		if (marker.popupContent) {
			infoWindow.setContent(marker.popupContent);    	
			infoWindow.open(map, marker);	
		}
	}
	
	// close any popups on the map
	function closePopupsOnMap() {
		if (infoWindow !== null) {
			infoWindow.close();
		}
	}
	
	// clear all markers from map & delete references, except for passed in marker
	function clearAllMarkersOnMap(marker) {
		if (markersArray !== null) {
			if (marker == null) {
				for (i in markersArray) {
					markersArray[i].setMap(null);
					markersArray[i] = null;
				}	
				markersArray = [];
			} else {
				for (i in markersArray) {
					if (marker !== markersArray[i]) {
						markersArray[i].setMap(null);
						markersArray[i] = null;
					}
				}	
			}
		}	
	}
	
	// return html for a SIRI VM response
	function getVehicleContentForResponse(r) {
		var age =  (r.ServiceDelivery.ResponseTimestamp - r.ServiceDelivery.VehicleMonitoringDelivery.deliveries[0].RecordedAtTime) / 1000;
		var vehicleId = r.ServiceDelivery.VehicleMonitoringDelivery.deliveries[0].MonitoredVehicleJourney.VehicleRef;
		var nextStops = r.ServiceDelivery.VehicleMonitoringDelivery.deliveries[0].MonitoredVehicleJourney.OnwardCalls;

		var nextStopsListHtml = null;
		if(nextStops.length === 0) {
			nextStopsListHtml = "Next stops are not known for this vehicle.";
		} else {
			var nextStopsList = jQuery("<ul></ul>");		
			jQuery.each(nextStops, function(_, stop) {
				if(_ >= 3) {
					return false;
				}
			
				nextStopsList.append("<li>" + stop.StopPointName + " - " + 
					stop.Extensions.Distances.StopsFromCall + "</li>");
			});
			nextStopsListHtml = nextStopsList.html();
		}
		
		return "<p>" + age + "<br/>" + vehicleId + "<br/>" + nextStopsListHtml + "</p>";
	}

	// return html for a SIRI SM response
	function getStopContentForResponse(r) {
		var responseTimestamp = r.ServiceDelivery.ResponseTimestamp;
		var latestVehicleUpdate = null;
		
		var visits = r.ServiceDelivery.stopMonitoringDeliveries[0].visits;

		var nextVisitsListHtml = null;
		if(visits.length === 0) {
			nextVisitsListHtml = "No vehicles en-route to this stop.";
		} else {
			var nextVisitsList = jQuery("<ul></ul>");		
			jQuery.each(visits, function(_, visit) {
				if(visit.RecordedAtTime > latestVehicleUpdate) {
					latestVehicleUpdate = visit.RecordedAtTime;
				}
				
				nextVisitsList.append("<li>" + visit.MonitoredVehicleJourney.LineRef + " - " + 
						visit.MonitoredVehicleJourney.PublishedLineName + 
						" dist. away=" + 
						visit.MonitoredVehicleJourney.MonitoredCall.Extensions.Distances.DistanceFromCall + 
						"</li>");
			});
			nextVisitsListHtml = nextVisitsList.html();
		}
		
		return "<p>" + ((responseTimestamp - latestVehicleUpdate) / 1000) + "<br/>" + nextVisitsListHtml + "</p>";
	}

	function addPolyline(routeId, directionId, encodedPolyline, color) {
		// already on map?
		if(typeof polylinesByRouteAndDirection[routeId + "_" + directionId] !== 'undefined') {
			return;
		}
	
		var points = OBA.Util.decodePolyline(encodedPolyline);
		
		var latlngs = jQuery.map(points, function(x) {
	    	return new google.maps.LatLng(x[0], x[1]);
	    });

	    var shape = new google.maps.Polyline({
	    	path: latlngs,
	        strokeColor: "#" + color,
	        strokeOpacity: 1.0,
	        strokeWeight: 5
	    });
	          
		shape.setMap(map);		

		polylinesByRouteAndDirection[routeId + "_" + directionId] = shape;
	}

	function addStops(routeId, directionId, stopItems) {
		// already on map?
		if(typeof stopsAddedForRouteAndDirection[routeId + "_" + directionId] !== 'undefined') {
			return;
		}

		jQuery.each(stopItems, function(_, stop) {
			if(typeof stopsById[stop.stopId] !== 'undefined') {
				return;
			}
			
			var icon = new google.maps.MarkerImage("img/stop/stop-" + stop.stopDirection + ".png",
                new google.maps.Size(21, 21),
                new google.maps.Point(0,0),
                new google.maps.Point(10, 10));
			
			var markerOptions = {
				position: new google.maps.LatLng(stop.latitude, stop.longitude),
	            icon: icon,
	            zIndex: 1,
	            map: map,
	            title: stop.name,
	            stopId: stop.stopId
			};

	        var marker = new google.maps.Marker(markerOptions);
	        
	    	google.maps.event.addListener(marker, "click", function(mouseEvent) {
	    		var marker = this;

	    		var stopIdParts = marker.stopId.split("_");
	    		var stopIdAgency = stopIdParts[0];
	    		var stopIdWithoutAgency = stopIdParts[1];
	    		
	    		showPopupWithContentFromRequest(marker, OBA.Config.siriSMUrl, 
	    			{ key: OBA.Config.apiKey, 
	    			OperatorRef: stopIdAgency, 
	    			MonitoringRef: stopIdWithoutAgency }, 
	    			getStopContentForResponse);
	    	});

	        stopsById[stop.stopId] = marker;
		});
		
		stopsAddedForRouteAndDirection[routeId + "_" + directionId] = true;
	}

	// update vehicles from SIRI VM call
	function updateVehicles(_routeId) {
		var routeIdParts = _routeId.split("_");
		var agencyId = routeIdParts[0];
		var routeId = routeIdParts[1];
		
		jQuery.getJSON(OBA.Config.siriVMUrl, { key: OBA.Config.apiKey, OperatorRef: agencyId, 
			LineRef: routeId, VehicleMonitoringDetailLevel: "normal" }, 
		function(json) { 
			var vehicles = [];
			jQuery.each(json.ServiceDelivery.VehicleMonitoringDelivery.deliveries, function(_, vehicle) {
				var timestamp = vehicle.RecordedAtTime;
				var headsign = vehicle.MonitoredVehicleJourney.PublishedLineName;
				var vehicleId = vehicle.MonitoredVehicleJourney.VehicleRef;
				var latitude = vehicle.MonitoredVehicleJourney.VehicleLocation.Latitude;
				var longitude = vehicle.MonitoredVehicleJourney.VehicleLocation.Longitude;

				// create marker if it doesn't exist, otherwise just move it
				var existingMarker = vehiclesById[vehicleId];
				if(typeof existingMarker === 'undefined' || existingMarker === null) {
					var icon = new google.maps.MarkerImage("img/vehicle/vehicle-unknown.png",
							new google.maps.Size(51, 51),
							new google.maps.Point(0,0),
							new google.maps.Point(25, 25));
					
					var markerOptions = {
							position: new google.maps.LatLng(latitude, longitude),
							icon: icon,
				            zIndex: 2,
							map: map,
							title: vehicleId + ":" + headsign,
							timestamp: timestamp,
							headsign: headsign,
							vehicleId: vehicleId
					};

					var marker = new google.maps.Marker(markerOptions);
			        
			    	google.maps.event.addListener(marker, "click", function(mouseEvent) {
			    		var marker = this;

			    		var vehicleIdParts = marker.vehicleId.split("_");
			    		var vehicleIdAgency = vehicleIdParts[0];
			    		var vehicleIdWithoutAgency = vehicleIdParts[1];

			    		showPopupWithContentFromRequest(marker, OBA.Config.siriVMUrl, 
			    			{ key: OBA.Config.apiKey, 
			    			OperatorRef: vehicleIdAgency, 
			    			VehicleRef: vehicleIdWithoutAgency,
			    			VehicleMonitoringDetailLevel: "calls" }, 
			    			getVehicleContentForResponse);
			    	});
				
					vehiclesById[vehicleId] = marker;			    
					vehicles.push(marker);
				} else {
					var position = new google.maps.LatLng(latitude, longitude);
					existingMarker.setPosition(position);
					
					vehiclesById[vehicleId] = existingMarker; 
				}
			});
			
			vehiclesByRoute[_routeId] = vehicles;
		});
	}
	
	// constructor:
	map = new google.maps.Map(mapNode, defaultMapOptions);

	// mta custom tiles
	map.overlayMapTypes.insertAt(0, mtaMapType);

	// styled basemap
	map.mapTypes.set('Google', GTransitMapType);
	map.mapTypes.set('Transit', transitStyledMapType);
	map.setMapTypeId('Transit');

	// request list of routes in viewport when user stops moving map
	if(typeof mapMoveCallbackFn === 'function') {
		google.maps.event.addListener(map, "idle", mapMoveCallbackFn);
	}
	
	// Add popup content to marker
	google.maps.Marker.prototype.popupContent = null;

	// popup refresh timer
	setInterval(function() {
		// update any open bubble
		if(infoWindow !== null && typeof infoWindow.refreshFn === 'function') {
			infoWindow.refreshFn();
		}
	}, 5000);

	return {
		// get map viewport
		getBounds: function() {
			return map.getBounds();
		},
		
		// add route, route's stops, and route's vehicles to map
		showRoute: function(route) {
			// add both destinations for route to map

			jQuery.each(route.destinations, function(_, destination) {
				addPolyline(route.routeId, destination.directionId, destination.polyline, route.color);
				addStops(route.routeId, destination.directionId, destination.stops);
			});

			// update vehicles on map
			updateVehicles(route.routeId);
			
			// setup update timer
			if(typeof vehicleUpdateTimersByRoute[route.routeId] === 'undefined') {
				var timer = setInterval(function() {
					updateVehicles(route.routeId);
				}, 5000);
			
				vehicleUpdateTimersByRoute[route.routeId] = timer;
			}
		},

		// move map to given location
		showLocation: function(lat, lng) {
			var location = new google.maps.LatLng(lat, lng);
			map.panTo(location);
			map.setZoom(16);
		},
		
		// move map to given location given google.maps.LatLng
		showLocationFromPoint: function(gLatlng) {
			map.panTo(gLatlng);
			map.setZoom(15);
		},
		
		// create a marker using passed in options
		createMarker: function(latlon, options) {
			
			var marker = new google.maps.Marker({
		        position: latlon, 
		        shadow: options.shadow,
		        icon: options.icon,
		        title: options.name, 
		        map: map
		    }); 
			
			if (options.popup) {	
				marker.popupContent = options.popup;
				
				google.maps.event.addListener(marker, 'click', function() {
					showPopupWithLocationInformation(marker);
				});
			}
			
			markersArray.push(marker);
			
			return marker;
		},
		
		showPopup: function(marker) {
			showPopupWithLocationInformation(marker);
		},
		
		closePopup: function() {
			closePopupsOnMap();
		},
		
		// clear all markers except for passed in marker (can be null)
		clearMarkers: function(marker) {
			clearAllMarkersOnMap(marker);
		}
	};
};
