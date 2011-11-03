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
	var mtaSubwayMapType = new google.maps.ImageMapType({
		//bounds: new google.maps.LatLngBounds(
		//		new google.maps.LatLng(40.92862373397717,-74.28397178649902),
		//		new google.maps.LatLng(40.48801936882241,-73.68182659149171)
		//),
		getTileUrl: function(coord, zoom) {
			if(!(zoom >= this.minZoom && zoom <= this.maxZoom)) {
				return null;
			}

		    //var projection = map.getProjection();
            //var zoomFactor = Math.pow(2, zoom);
            //var tileCenter = projection.fromPointToLatLng(new google.maps.Point(coord.x * 256 / zoomFactor, coord.y * 256 / zoomFactor));

            //if(!this.bounds.contains(tileCenter)) {
            //	return null;
            //}
			
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
				mapTypeIds: [ "Google", "Transit", "MTA Subway Map" ]
			}
	};

	var map = null;
	var mgr = null;

	var markersArray = [];

	var vehiclesByRoute = {};
	var vehiclesById = {};
	var polylinesByRouteAndDirection = {};
	var stopsAddedForRouteAndDirection = {};
	var stopsById = {};
	var infoWindow = new google.maps.InfoWindow({});
	
	// only one popup open at a time!
	var closeFn = function() {
		if(infoWindow !== null) {
			infoWindow.close();
		}
	};
	google.maps.event.addListener(infoWindow, "closeclick", closeFn);
	

	// POPUPS	
	// create a popup with content from the named URL+params, from the contentFn specified.
	// the bubble will refresh itself when the map is also refreshed.
	function showPopupWithContentFromRequest(marker, url, params, contentFn, userData) {
		var popupOptions = {
    		content: "Loading...",
    		pixelOffset: new google.maps.Size(0, (marker.getIcon().size.height / 2))
    	};
		
		closeFn();
		
		// called to refresh the bubble's content
		google.maps.InfoWindow.prototype.refreshFn = function() {
			jQuery.getJSON(url, params, function(json) {
				infoWindow.setContent(contentFn(json, userData));
			});
		};
    	infoWindow.setOptions(popupOptions);
    	infoWindow.open(map, marker);
    	infoWindow.refreshFn();	
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
					if (marker !== markersArray[i] && markersArray[i] != null) {
						markersArray[i].setMap(null);
						markersArray[i] = null;
					}
				}	
			}
		}	
	}
	
	// return html for a SIRI VM response
	function getVehicleContentForResponse(r) {
		var activity = r.ServiceDelivery.VehicleMonitoringDelivery[0].VehicleActivity[0];

		if(activity === null) {
			return null;
		}

		var vehicleId = activity.MonitoredVehicleJourney.VehicleRef;
		var vehicleIdParts = vehicleId.split("_");
		var vehicleIdWithoutAgency = vehicleIdParts[1];

		var routeId = activity.MonitoredVehicleJourney.LineRef;
		var routeIdParts = routeId.split("_");
		var routeIdWithoutAgency = routeIdParts[1];

		var html = '<div id="popup">';
		
		// header
		html += ' <div class="header vehicle">';
		html += '  <p class="title">' + routeIdWithoutAgency + " " + activity.MonitoredVehicleJourney.PublishedLineName + '</p><p>';
		html += '   <span class="type">Vehicle #' + vehicleIdWithoutAgency + '</span>';

		// update time
		var updateTimestamp = new Date(activity.RecordedAtTime).getTime();
		var updateTimestampReference = new Date(r.ServiceDelivery.ResponseTimestamp).getTime();
		html += '   <span class="updated">Last updated ' + OBA.Util.displayTime((updateTimestampReference - updateTimestamp) / 1000) + '</span>'; 
		
		// (end header)
		html += '  </p>';
		html += ' </div>';
		
		// service available at stop
		if(typeof activity.MonitoredVehicleJourney.MonitoredCall === 'undefined' 
			|| typeof activity.MonitoredVehicleJourney.OnwardCalls === 'undefined') {

			html += '<p class="service">Next stops are not known for this vehicle.</p>';
		} else {		
			var nextStops = [];
			nextStops.push(activity.MonitoredVehicleJourney.MonitoredCall);
			jQuery.each(activity.MonitoredVehicleJourney.OnwardCalls.OnwardCall, function(_, onwardCall) {
				if(nextStops.length >= 3) {
					return false;
				}
				nextStops.push(onwardCall);
			});
		
			html += '<p class="service">Next stops:</p>';
			html += '<ul>';
			jQuery.each(nextStops, function(_, call) {
				html += '<li class="nextStop">' + call.StopPointName + ' <span>';
				html +=   call.Extensions.distances.presentableDistance;
				html += '</span></li>';
			});
			html += '</ul>';
		}
	
		// (end popup)
		html += '</div>';
		
		return html;
	}
	
	function getStopContentForResponse(r, stopItem) {
		var visits = r.ServiceDelivery.StopMonitoringDelivery[0].MonitoredStopVisit;
		
		if(visits === null) {
			return null;
		}
		
		var html = '<div id="popup">';
		
		// header
		html += ' <div class="header stop">';
		html += '  <p class="title">' + stopItem.name + '</p><p>';
		html += '   <span class="type">Stop #' + stopItem.stopIdWithoutAgency + '</span>';

		// update time across all arrivals
		var age = null;
		var updateTimestampReference = new Date(r.ServiceDelivery.ResponseTimestamp).getTime();
		jQuery.each(visits, function(_, monitoredJourney) {
			var updateTimestamp = new Date(monitoredJourney.RecordedAtTime).getTime();
			var thisAge = (updateTimestampReference - updateTimestamp) / 1000;
			if(thisAge > age) {
				age = thisAge;
			}
		});
		if(age !== null) {
			html += '   <span class="updated">Last updated ' + OBA.Util.displayTime(age) + '</span>'; 
		}
		
		// (end header)
		html += '  </p>';
		html += ' </div>';
		
		// service available
		if(visits.length === 0) {
			html += '<p class="service">OneBusAway NYC is not tracking any buses en-route to your location.<br/>Please check back shortly for an update.</p>';
		} else {		
			html += '<p class="service">This stop is served by:</p>';
			html += '<ul>';

			var arrivalsByRouteAndHeadsign = {};
			jQuery.each(visits, function(_, monitoredJourney) {
				var routeId = monitoredJourney.MonitoredVehicleJourney.LineRef;
				var routeIdParts = routeId.split("_");
				var routeIdWithoutAgency = routeIdParts[1];
				
				var key = routeIdWithoutAgency + " " + monitoredJourney.MonitoredVehicleJourney.PublishedLineName;
				if(typeof arrivalsByRouteAndHeadsign[key] === 'undefined') {
					arrivalsByRouteAndHeadsign[key] = [];
				}

				arrivalsByRouteAndHeadsign[key].push(monitoredJourney.MonitoredVehicleJourney.MonitoredCall);
			});
		
			jQuery.each(arrivalsByRouteAndHeadsign, function(routeLabel, monitoredCalls) {
				html += '<li class="route">' + routeLabel + '</li>';

				jQuery.each(monitoredCalls, function(_, monitoredCall) {
					if(_ >= 3) {
						return false;
					}
					html += '<li class="arrival">' + monitoredCall.Extensions.distances.presentableDistance + '</li>';
				});
			});
			html += '</ul>';
		}
		
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
			var stopId = stop.stopId;
			var name = stop.name;
			var latitude = stop.latitude;
			var longitude = stop.longitude;
			var direction = stop.stopDirection;
			
			if(typeof stopsById[stopId] !== 'undefined') {
				return;
			}
			
			var directionKey = direction;
			if(directionKey === null) {
				directionKey = "unknown";
			}
			
			var icon = new google.maps.MarkerImage("img/stop/stop-" + directionKey + ".png",
                new google.maps.Size(21, 21),
                new google.maps.Point(0,0),
                new google.maps.Point(10, 10));
			
			var markerOptions = {
				position: new google.maps.LatLng(latitude, longitude),
	            icon: icon,
	            zIndex: 1,
	            title: name,
	            stopId: stopId
			};

	        var marker = new google.maps.Marker(markerOptions);
	        
	    	google.maps.event.addListener(marker, "click", function(mouseEvent) {
	    		var stopIdParts = stopId.split("_");
	    		var agencyId = stopIdParts[0];
	    		var stopIdWithoutAgency = stopIdParts[1];

	    		showPopupWithContentFromRequest(this, OBA.Config.siriSMUrl, 
	    				{ OperatorRef: agencyId, MonitoringRef: stopIdWithoutAgency, StopMonitoringDetailLevel: "normal" }, 
	    				getStopContentForResponse, stop);
	    	});

	    	// FIXME: route zoom level configuration?
	    	mgr.addMarker(marker, 16, 19);
	    	
	        stopsById[stop.stopId] = marker;
		});
		
		stopsAddedForRouteAndDirection[routeId + "|" + directionId] = true;
	}
	
	// VEHICLES
	
	// takes an array of routeIds or a single routeId (string)
	function updateVehicles(routeId) {
		if(typeof vehiclesByRoute[routeId] === 'undefined') {
			vehiclesByRoute[routeId] = {};
		}
		
		var routeIdParts = routeId.split("_");
		var agencyId = routeIdParts[0];
		var routeIdWithoutAgency = routeIdParts[1];
		
		jQuery.getJSON(OBA.Config.siriVMUrl + "?callback=?", { OperatorRef: agencyId, LineRef: routeIdWithoutAgency }, 
		function(json) {

			var vehiclesByIdInResponse = {};
			jQuery.each(json.ServiceDelivery.VehicleMonitoringDelivery[0].VehicleActivity, function(_, activity) {

				var latitude = activity.MonitoredVehicleJourney.VehicleLocation.Latitude;
				var longitude = activity.MonitoredVehicleJourney.VehicleLocation.Longitude;
				var orientation = activity.MonitoredVehicleJourney.Bearing;
				var headsign = activity.MonitoredVehicleJourney.PublishedLineName;

				var vehicleId = activity.MonitoredVehicleJourney.VehicleRef;
				var vehicleIdParts = vehicleId.split("_");
				var vehicleIdWithoutAgency = vehicleIdParts[1];

				var marker = vehiclesById[vehicleId];

				// create marker if it doesn't exist				
				if(typeof marker === 'undefined' || marker === null) {
					var markerOptions = {
				            zIndex: 2,
							map: map,
							title: routeIdWithoutAgency + " " + headsign,
							vehicleId: vehicleId,
							routeId: routeId
					};

					marker = new google.maps.Marker(markerOptions);
			        
			    	google.maps.event.addListener(marker, "click", function(mouseEvent) {
			    		showPopupWithContentFromRequest(this, OBA.Config.siriVMUrl, 
			    				{ OperatorRef: agencyId, VehicleRef: vehicleIdWithoutAgency, VehicleMonitoringDetailLevel: "calls" }, 
			    				getVehicleContentForResponse, null);
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
					
					vehicleOnMap.setMap(null);
					delete vehiclesById[vehicleOnMap_vehicleId];
					delete vehiclesByRoute[vehicleOnMap_routeId][vehicleOnMap_vehicleId];
				}
			});
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
	map.overlayMapTypes.insertAt(0, mtaSubwayMapType);
	

	// styled basemap
	map.mapTypes.set('Google', GTransitMapType);
	map.mapTypes.set('Transit', transitStyledMapType);
	map.setMapTypeId('Transit');
	
	// Create Subway Tiles control
	var subwayControlDiv = document.createElement('DIV');
	subwayControlDiv.index = 1;
	map.controls[google.maps.ControlPosition.TOP_RIGHT].push(subwayControlDiv);
	
	// Adds a button control to toggle MTA Subway tiles
	function SubwayTilesControl(controlDiv, map) {

	  controlDiv.style.padding = '5px';
	  
	  var controlUI = document.createElement('DIV');
	  controlUI.style.backgroundColor = 'white';
	  controlUI.style.borderStyle = 'solid';
	  controlUI.style.borderWidth = '1px';
	  controlUI.style.cursor = 'pointer';
	  controlUI.style.textAlign = 'center';
	  controlUI.title = 'Click to toggle MTA Subway lines';
	  controlDiv.appendChild(controlUI);

	  var controlText = document.createElement('DIV');
	  controlText.style.fontFamily = 'Arial,sans-serif';
	  controlText.style.fontWeight = 'normal';
	  controlText.style.fontSize = '12px';
	  controlText.style.paddingLeft = '5px';
	  controlText.style.paddingRight = '5px';
	  controlText.style.paddingTop = '3px';
	  controlText.style.paddingBottom = '3px';
	  controlText.innerHTML = '<b>Subway</b>';
	  controlUI.appendChild(controlText);

	  function toggleSubway() {
		  (map.overlayMapTypes.length == 1) ? 
				  map.overlayMapTypes.removeAt(0, mtaSubwayMapType) : map.overlayMapTypes.insertAt(0, mtaSubwayMapType);
	  }
	  google.maps.event.addDomListener(controlUI, 'click', function() { toggleSubway(); });

	}	
	var subwayTilesControl = new SubwayTilesControl(subwayControlDiv, map);
	
	// request list of routes in viewport when user stops moving map
	if(typeof mapMoveCallbackFn === 'function') {
		google.maps.event.addListener(map, "idle", mapMoveCallbackFn);
	}
	
	// Add popup content to marker
	google.maps.Marker.prototype.popupContent = null;

	// timer to update periodically
	setInterval(function() {
		jQuery.each(vehiclesByRoute, function(routeId, vehicles) {
			updateVehicles(routeId);
		});

		if(infoWindow !== null && infoWindow.refreshFn != null) {
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

			updateVehicles(route.routeId);
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
