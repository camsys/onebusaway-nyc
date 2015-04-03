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

OBA.RouteMap = function(mapNode, initCallbackFn, serviceAlertCallbackFn) {	

	var map = null;

	var locationMarker = null;
	var disambiguationMarkers = [];

	var vehiclesByRoute = {};
	var vehiclesById = {};
	var polylinesByRoute = {};
	var hoverPolylinesByRoute = {};
	var stopsById = {};

	var siriVMRequestsByRouteId = {};
	var stopsWithinBoundsRequest = null;
	
	// when hovering over a route in "region" view
	var hoverPolyline = null;
	
	// when hovering over a stop in route view
	var highlightedStop = null;

	// icons for disambiguation markers
	var locationIconArrays = OBA.Config.loadLocationIcons();
	var locationIcons = locationIconArrays[0], activeLocationIcons = locationIconArrays[1], iconShadow = locationIconArrays[2];    
	var normalLocationIcon = locationIcons[0], activeLocationIcon = activeLocationIcons[0];
	
	L.Icon.Default.imagePath = 'img/leaflet';
	
	// POLYLINE
	function removePolylines(routeId) {
		if(typeof polylinesByRoute[routeId] !== 'undefined') {
			var hoverPolyines = hoverPolylinesByRoute[routeId];

			jQuery.each(hoverPolyines, function(_, polyline) {
				map.removeLayer(polyline);
			});

			var polylines = polylinesByRoute[routeId];

			jQuery.each(polylines, function(_, polyline) {
				map.removeLayer(polyline);
			});
			
			delete polylinesByRoute[routeId];
			delete hoverPolylinesByRoute[routeId];
		}
	}
	
	function addPolylines(routeId, encodedPolylines, color) {
		if(typeof polylinesByRoute[routeId] === 'undefined') {
			polylinesByRoute[routeId] = [];
			hoverPolylinesByRoute[routeId] = [];
		}

		jQuery.each(encodedPolylines, function(_, encodedPolyline) {
			var points = OBA.Util.decodePolyline(encodedPolyline);
		
			var latlngs = jQuery.map(points, function(x) {
				return new L.LatLng(x[0], x[1]);
			});

			var options = {
				color: "#" + color,
				opacity: 1.0,
				weight: 3,
				clickable: false
			};
			
			var shape = new L.Polyline(latlngs, options);			
			shape.addTo(map);
			
			var hoverOptions = {
					color: "#" + color,
					opacity: 0.6,
					weight: 10,
					clickable: false
			};

			var hoverShape = new L.Polyline(latlngs, hoverOptions);
			
			polylinesByRoute[routeId].push(shape);
			hoverPolylinesByRoute[routeId].push(hoverShape);
		});	
	}

	// STOPS
	function removeStops(preserveStopsInView) {
		jQuery.each(stopsById, function(_, marker) {
			var stopId = marker.stopId;
			/*if(stopId === OBA.Popups.getPopupStopId()) {
				return true;
			}*/
			if(preserveStopsInView && map.getBounds().contains(marker.getLatLng())) {
				return true;
			}
			delete stopsById[stopId];
			marker.setOpacity(0);
		});
	}
	
	function addStop(stop, successFn) {
		var stopId = stop.id;
		var marker;
		if(typeof stopsById[stopId] !== 'undefined') {
			marker = stopsById[stopId];
	        if(typeof successFn !== 'undefined' && successFn !== null) {
	        	successFn(marker);
	        }
	        marker.setOpacity(1);
		}
		else {
			var name = stop.name;
			var latitude = stop.latitude;
			var longitude = stop.longitude;
			var direction = stop.stopDirection;		
			var directionKey = direction;
			if(directionKey === null) {
				directionKey = "unknown";
			}
			var icon = new L.Icon({iconUrl: "img/stop/stop-" + directionKey + ".png",	iconSize: [21, 21]});
			var defaultOpacity = (map.getZoom() < 16) ? 0 : 1;
			var markerOptions = {
				icon: icon,
				title: name,
				stopId: stopId,
				opacity: defaultOpacity,
				map: map
			};
	        marker = new L.Marker([latitude, longitude], markerOptions);
	        marker.addEventListener("click", function(mouseEvent, routeFilter) {
	    	   var stopIdParts = stopId.split("_");
	    	   var stopIdWithoutAgency = stopIdParts[1];
	    	   OBA.Config.analyticsFunction("Stop Marker Click", stopIdWithoutAgency);
	    	   OBA.Popups.showPopupWithContentFromRequest(map, this, OBA.Config.stopForId, 
				   { stopId: stopId },
				   OBA.Popups.getStopContentForResponse, 
				   routeFilter);
	    	});
	        stopsById[stopId] = marker;
	        if(typeof successFn !== 'undefined' && successFn !== null) {
	        	successFn(marker);
	        }
	        map.addLayer(marker);
		}
        return marker;
	}
	
	// VEHICLES
	function updateVehicles(routeId) {
		if(typeof vehiclesByRoute[routeId] === 'undefined') {
			vehiclesByRoute[routeId] = {};
		}
		
		var routeIdParts = routeId.split("_");
		var agencyId = routeIdParts[0];
		var routeIdWithoutAgency = routeIdParts[1];
		
		var params = { OperatorRef: agencyId, LineRef: routeIdWithoutAgency };		

		if(OBA.Config.time !== null) {
			params.time = OBA.Config.time;
		}
		
		if(typeof siriVMRequestsByRouteId[routeId] !== 'undefined' && siriVMRequestsByRouteId[routeId] !== null) {
			siriVMRequestsByRouteId[routeId].abort();
		}
		siriVMRequestsByRouteId[routeId] = jQuery.getJSON(OBA.Config.siriVMUrl + "&callback=?", params, 
		function(json) {
			if(json != undefined) {
				// service alerts
				if(typeof serviceAlertCallbackFn === 'function') {
					if(typeof json.Siri.ServiceDelivery.SituationExchangeDelivery !== 'undefined' && json.Siri.ServiceDelivery.SituationExchangeDelivery.length > 0) {
						serviceAlertCallbackFn(routeId, 
							json.Siri.ServiceDelivery.SituationExchangeDelivery[0].Situations.PtSituationElement);
					}
				}
				
				// service delivery
				var vehiclesByIdInResponse = {};
				jQuery.each(json.Siri.ServiceDelivery.VehicleMonitoringDelivery[0].VehicleActivity, function(_, activity) {
					var latitude = activity.MonitoredVehicleJourney.VehicleLocation.Latitude;
					var longitude = activity.MonitoredVehicleJourney.VehicleLocation.Longitude;
					var orientation = activity.MonitoredVehicleJourney.Bearing;
					var headsign = activity.MonitoredVehicleJourney.DestinationName;
					var routeName = activity.MonitoredVehicleJourney.PublishedLineName;
	
					var vehicleId = activity.MonitoredVehicleJourney.VehicleRef;
					var vehicleIdParts = vehicleId.split("_");
					var vehicleIdWithoutAgency = vehicleIdParts[1];
					var marker = vehiclesById[vehicleId];
					
					// has route been removed while in the process of updating?
					if(typeof vehiclesByRoute[routeId] === 'undefined') {
						return false;
					}
					
					// create marker if it doesn't exist				
					if(typeof marker === 'undefined' || marker === null) {
						var markerOptions = {
							riseOffset: 3,
							map: map,
							title: "Vehicle " + vehicleIdWithoutAgency + ", " + routeName + " to " + headsign,
							vehicleId: vehicleId,
							routeId: routeId,
							map: map
						};
	
						marker = new L.Marker([latitude, longitude], markerOptions);
						marker.addTo(map);
				        
				    	marker.addEventListener("click", function(mouseEvent) {
				    		OBA.Config.analyticsFunction("Vehicle Marker Click", vehicleIdWithoutAgency);
	
			    		OBA.Popups.showPopupWithContentFromRequest(map, this, OBA.Config.siriVMUrl + "&callback=?", 
			    				{ OperatorRef: agencyId, VehicleRef: vehicleIdWithoutAgency, MaximumNumberOfCallsOnwards: "3", VehicleMonitoringDetailLevel: "calls" }, 
			    				OBA.Popups.getVehicleContentForResponse, null);
				    	});
					} else{
						map.addLayer(marker);
					}
	
					// icon
					var orientationAngle = "unknown";
					if(orientation !== null && orientation !== 'NaN') {
						orientationAngle = Math.floor(orientation / 5) * 5;
					}
					var icon = new L.Icon({iconUrl: "img/vehicle/vehicle-" + orientationAngle + ".png", iconSize: [51, 51]});
	
					marker.setIcon(icon);
	
					// position
					var position = new L.LatLng(latitude, longitude);
					marker.setLatLng(position);
								    	
					// (mark that this vehicle is still in the response)
					vehiclesByIdInResponse[vehicleId] = true;
	
					// maps used to keep track of marker
					vehiclesByRoute[routeId][vehicleId] = marker;
					vehiclesById[vehicleId] = marker; 
				});
				
				// remove vehicles from map that are no longer in the response, for all routes in the query
				jQuery.each(vehiclesById, function(vehicleOnMap_vehicleId, vehicleOnMap) {
					if(typeof vehiclesByIdInResponse[vehicleOnMap_vehicleId] === 'undefined') {
						var vehicleOnMap_routeId = vehicleOnMap.routeId;
						
						// the route of the vehicle on the map wasn't in the query, so don't check it.
						if(routeId !== vehicleOnMap_routeId) {
							return;
						}
						
						map.removeLayer(vehicleOnMap);
						delete vehiclesById[vehicleOnMap_vehicleId];
						delete vehiclesByRoute[vehicleOnMap_routeId][vehicleOnMap_vehicleId];
					}
				});
			}
		});
	}
	
	function removeVehicles(routeId) {
		if(typeof vehiclesByRoute[routeId] !== 'undefined') {
			var vehicles = vehiclesByRoute[routeId];
			delete vehiclesByRoute[routeId];
			
			jQuery.each(vehicles, function(_, marker) {
				var vehicleId = marker.vehicleId;
				map.removeLayer(marker);
				delete vehiclesById[vehicleId];
			});
		}
	}
	
	// MISC	
	function removeDisambiguationMarkers() {
		jQuery.each(disambiguationMarkers, function(_, marker) {
			map.removeLayer(marker);
		});
	}
	
	function removeHoverPolyline() {
		if(hoverPolyline !== null) {
			jQuery.each(hoverPolyline, function(_, polyline) {
				map.removeLayer(polyline);
			});
		}
		
		hoverPolyline = null;
	}
	
	function unhighlightStop() {
		if(highlightedStop !== null) {
			var previousOpacity = highlightedStop.options.previousOpacity;
			if(OBA.Popups.getPopupStopId() !== highlightedStop.options.stopId) {
				highlightedStop.setOpacity(previousOpacity);
				highlightedStop.options.previousOpacity = 0;
			}
			highlightedStop.setIcon(highlightedStop.options.previousIcon);
			highlightedStop.options.previousIcon = null;
		}
		highlightedStop = null;
	}

	//////////////////// CONSTRUCTOR /////////////////////
	map = new OBA.LeafletMapWrapper(document.getElementById("map"));
	
	// If there is no configured map center and zoom...
	// Zoom/pan the map to the area specified from our configuration JavaScript that gets its
	// values from the server dynamically on page load.
	if (!OBA.Config.mapCenterLat || !OBA.Config.mapCenterLon || !OBA.Config.mapZoom) {
		var swCorner = new L.LatLng(OBA.Config.mapBounds.swLat, OBA.Config.mapBounds.swLon);
		var neCorner = new L.LatLng(OBA.Config.mapBounds.neLat, OBA.Config.mapBounds.neLon);
		var bounds = new L.LatLngBounds(swCorner, neCorner);
		map.fitBounds(bounds);
	}
	
	// refresh the stops on the map after the user is done panning
	map.addEventListener("moveend", function() {
		// request list of stops in viewport when user stops moving map
		if(map.getZoom() < 16) {
			removeStops(false);
		} else {
			if(stopsWithinBoundsRequest !== null) {
				stopsWithinBoundsRequest.abort();
			}
			var southwest = map.getBounds().getSouthWest();
			var northeast = map.getBounds().getNorthEast();
			var bounds = southwest.lat+','+southwest.lng+','+northeast.lat+','+northeast.lng;
			stopsWithinBoundsRequest = jQuery.getJSON(OBA.Config.stopsWithinBoundsUrl + "?callback=?", { bounds: bounds },
			function(json) {
				removeStops(true);
				jQuery.each(json.stops, function(_, stop) {
					addStop(stop, null);					
				});
			});
		}
	});
	
	// timer to update data periodically 
	setInterval(function() {
		jQuery.each(vehiclesByRoute, function(routeId, vehicles) {
			updateVehicles(routeId);
		});
	}, OBA.Config.refreshInterval);

	//////////////////// PUBLIC INTERFACE /////////////////////
	return {
		// STOP HOVER 
		highlightStop: function(stopResult) {
			unhighlightStop();
			
			var stopMarker = stopsById[stopResult.id];
			if(typeof stopMarker === 'undefined') {
				stopMarker = addStop(stopResult, null);
			}
			var direction = stopResult.stopDirection;		
			var directionKey = direction;

			if(directionKey === null) {
				directionKey = "unknown";
			}
			var highlightedIcon = new L.Icon({iconUrl: "img/stop/stop-" + directionKey + "-active.png", iconSize: [21, 21]});
			
			stopMarker.options.previousIcon = stopMarker.options.icon;
			stopMarker.setIcon(highlightedIcon);

			stopMarker.options.previousOpacity = stopMarker.options.opacity;
			stopMarker.setOpacity(1);
			
			highlightedStop = stopMarker;
		},
		
		unhighlightStop: unhighlightStop,
		
		// ROUTE HOVER

		// these methods are for routes that are *not* on the map yet
		removeHoverPolyline: removeHoverPolyline,
		
		showHoverPolyline: function(encodedPolylines, color) {
			hoverPolyline = [];
			jQuery.each(encodedPolylines, function(_, encodedPolyline) {
				var points = OBA.Util.decodePolyline(encodedPolyline);
			
				var latlngs = jQuery.map(points, function(x) {
					return new L.LatLng(x[0], x[1]);
				});

				var shape = new L.Polyline({
					path: latlngs,
					strokeColor: "#" + color,
					strokeOpacity: 0.7,
					strokeWeight: 3,
					map: map
				});

				var hoverShape = new L.Polyline({
					path: latlngs,
					strokeColor: "#" + color,
					strokeOpacity: 0.6,
					strokeWeight: 10,
					map: map
				});

				hoverPolyline.push(shape);
				hoverPolyline.push(hoverShape);
			});
		},
		
		// these methods are for routes *already on* the map
		highlightRoute: function(routeId) {
			var polylines = hoverPolylinesByRoute[routeId];
			if(polylines !== null) {
				jQuery.each(polylines, function(_, polyline) {
					map.addLayer(polyline);
				});
			}
		},
		
		unhighlightRoute: function(routeId) {
			var polylines = hoverPolylinesByRoute[routeId];
			
			if(polylines !== null) {
				jQuery.each(polylines, function(_, polyline) {
					map.removeLayer(polyline);
				});
			}
		},
		
		// ROUTE/STOP DISPLAY
		addStop: addStop,

		addRoute: function(routeResult) {
			// already on map
			if(typeof polylinesByRoute[routeResult.id] !== 'undefined') {
				return;
			}

			jQuery.each(routeResult.directions, function(_, direction) {
				addPolylines(routeResult.id, direction.polylines, routeResult.color);
			});

			updateVehicles(routeResult.id);
		},
		
		reset: function() {
			OBA.Popups.reset();
			
			removeHoverPolyline();
			removeDisambiguationMarkers();
			
			if(locationMarker !== null) {
				map.removeLayer(locationMarker);
			}
			
			jQuery.each(polylinesByRoute, function(routeAndAgencyId, _) {
				if(routeAndAgencyId === null) {
					return;
				}

				removePolylines(routeAndAgencyId);
				removeVehicles(routeAndAgencyId);
				removeStops(false);
			});
		},
		
		panToRoute: function(routeId) {
			var polylines = polylinesByRoute[routeId];

			if(polylines === null) {
				return;
			}
			
			var newBounds = new L.LatLngBounds();
			jQuery.each(polylines, function(_, polyline) {
				if (typeof polyline !== 'undefined') {
					var coordinates = polyline.getLatLngs();
					for (var k=0; k < coordinates.length; k++) {
						var coordinate = coordinates[k];
						newBounds.extend(coordinate);
					}
				}
			});
			map.fitBounds(newBounds);
		},

		showPopupForStopId: function(stopId, routeFilter) {
			var stopMarker = stopsById[stopId];

			if(typeof stopMarker === 'undefined') {
				return false;
			}
			
			stopMarker.setOpacity(1);

			stopMarker.fire("click", null, routeFilter);
		},
		
		// LOCATION SEARCH
		addLocationMarker: function(latlng, address, neighborhood) {
			var markerOptions = {
				position: latlng,
		        icon: normalLocationIcon,
		        riseOffset: 2,
		        title: address,
		        shadow: iconShadow,
		        map: map
			};
			
			locationMarker = new L.Marker(latlng, markerOptions);

			locationMarker.addEventListener("click", function(mouseEvent) {
				var content = '<h3><b>' + address + '</b></h3>';

				if (neighborhood !== null) {
		    		content += neighborhood;
		    	}
	    		
				OBA.Popups.showPopupWithContent(map, locationMarker, content);
	    	});

			locationMarker.addEventListener("mouseover", function(mouseEvent) {
		    	locationMarker.setIcon(activeLocationIcon);
	    	});
	    	
			locationMarker.addEventListener("mouseout", function(mouseEvent) {
		    	locationMarker.setIcon(normalLocationIcon);
	    	});
		},
		
		showLocation: function(latlon) {
			map.panTo(latlon);			
			map.setZoom(16);
		},
		
		showBounds: function(bounds) {
			map.fitBounds(bounds);
		},
		
		// DISAMBIGUATION
		addDisambiguationMarker: function(latlng, address, neighborhood, i) {
			var locationIcon = (i !== undefined && i < 10) ? locationIcons[i] : normalLocationIcon;
			var markerOptions = {
					position: latlng,
		            icon: locationIcon,
		            zIndex: 2,
		            title: address,
		            shadow: iconShadow,
		            map: map
			};

		    var marker = new L.Marker(latlng, markerOptions);
		    disambiguationMarkers.push(marker);

		    marker.addEventListener("click", function(mouseEvent) {
	    		var content = '<h3><b>' + address + '</b></h3>';

	    		if(neighborhood !== null) {
	    			content += neighborhood;
	    		}

	    		OBA.Popups.showPopupWithContent(map, marker, content);
	    	});

	    	return marker;
		},
		
		highlightDisambiguationMarker: function(marker, i) {
			marker.setAnimation(L.Animation.BOUNCE);

			if(i !== undefined) {
				marker.setIcon(activeLocationIcons[i]);
			} else {
				marker.setIcon(activeLocationIcon);
			}
		},

		unhighlightDisambiguationMarker: function(marker, i) {
			marker.setAnimation(null);

			if(i !== undefined) {
				marker.setIcon(locationIcons[i]);
			} else {
				marker.setIcon(normalLocationIcon);
			}
		},
		
		// WIZARD
		registerMapListener: function(listener, fx) {
			return map.addEventListener(listener, fx);
		},

		unregisterMapListener: function(registeredName) {
			if (map.hasEventListeners(registeredName)){
				return map.removeEventListener(registeredName);
			}
		},
		
		registerStopBubbleListener: function(obj, trigger) {
			return OBA.Popups.registerStopBubbleListener(obj, trigger);
		},

		unregisterStopBubbleListener: function() {
			return OBA.Popups.unregisterStopBubbleListener();
		},
		
		setup: function() {
			// start adding things to map once it's ready...
			if(typeof initCallbackFn === 'function') {
				initCallbackFn();
			}
		}
	};
};