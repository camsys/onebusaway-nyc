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

OBA.RouteMap = function(mapNode, mapMoveCallbackFn, stopClickCallbackFn) {
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
		opacity:1.0,
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
	var polylinesByRouteAndDirection = {};
	var vehiclesByRouteAndDirection = {};
	var stopsAddedForRoute = {};
	var stopsById = {};

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
		if(typeof stopsAddedForRoute[routeId + "_" + directionId] !== 'undefined') {
			return;
		}

		jQuery.each(stopItems, function(_, stop) {
			if(typeof stopsById[stop.stopId] !== 'undefined') {
				return;
			}
			
			var icon = new google.maps.MarkerImage("img/stop/stop-" + stop.stopDirection + ".png",
                new google.maps.Size(21, 21),
                new google.maps.Point(0,0),
                new google.maps.Point(16, 16));
			
			var markerOptions = {
				position: new google.maps.LatLng(stop.latitude, stop.longitude),
	            icon: icon,
	            map: map,
	            title: stop.name,
	            stopId: stop.stopId
			};

	        var marker = new google.maps.Marker(markerOptions);
	        
	    	if(typeof stopClickCallbackFn === 'function') {
	    		google.maps.event.addListener(marker, "click", stopClickCallbackFn);
	    	}

	        stopsById[stop.stopId] = marker;
		});
		
		stopsAddedForRoute[routeId + "_" + directionId] = true;
	}

	function updateVehicles(routeId, directionId, stopItems) {
	}
	
	// constructor:
	map = new google.maps.Map(mapNode, defaultMapOptions);

	// mta custom tiles
	map.overlayMapTypes.insertAt(0, mtaMapType);

	// styled basemap
	map.mapTypes.set('Transit', transitStyledMapType);
	map.setMapTypeId('Transit');

	// event handlers
	if(typeof mapMoveCallbackFn === 'function') {
		google.maps.event.addListener(map, "idle", mapMoveCallbackFn);
	}

	return {
		// get map viewport
		getBounds: function() {
			return map.getBounds();
		},
		
		// add route, route's stops, and route's vehicles to map
		showRoute: function(route) {
			jQuery.each(route.destinations, function(_, destination) {
				addPolyline(route.routeId, destination.directionId, destination.polyline, route.color);
				addStops(route.routeId, destination.directionId, destination.stops);
				updateVehicles(route.routeId, destination.directionId, destination.stops);
			});
		},

		// move map to given location
		showLocation: function(lat, lng) {
			var location = new google.maps.LatLng(lat, lng);
			map.panTo(location);
			map.setZoom(16);
		}
	};
};
