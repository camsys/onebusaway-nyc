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

	var options = jQuery.extend({}, defaultMapOptions, mapOptions || {});
	var map = new google.maps.Map(mapNode, options);

	// mta custom tiles
	map.overlayMapTypes.insertAt(0, mtaMapType);

	// styled basemap
	map.mapTypes.set('Transit', transitStyledMapType);
	map.setMapTypeId('Transit');
	
	function addPolyline(routeId, directionId, encodedPolyline) {
		var points = OBA.Util.decodePolyline(encodedPolyline);
		
		var latlngs = jQuery.map(points, function(x) {
	    	return new google.maps.LatLng(x[0], x[1]);
	    });

	    var shape = new google.maps.Polyline({
	    	path: latlngs,
	        strokeColor: "#0000FF",
	        strokeOpacity: 0.5,
	        strokeWeight: 5
	    });
	          
		shape.setMap(map);		
	}

	function addStops(routeId, directionId, stopItems) {
		jQuery.each(stopItems, function(_, stopItem) {
			
		});
		
	}
	
	function updateVehicles(routeId, directionId, stopItems) {
debugger;
	}
	
	return {
		getBounds: function() {
			return map.getBounds();
		},
		
		showRoute: function(route, target) {
			if(target !== null) {
				var item = jQuery(
				"<li>" + 
					"<p>" + 
						route.name + " " + route.description + 
					"</p>" + 
					"<p>" + 
						route.destinations[0].headsign + " <> " + route.destinations[1].headsign + 
					"</p>" + 
				"</li>");
				
				target.append(item);
			}

			jQuery.each(route.destinations, function(_, destination) {
				addPolyline(route.routeId, destination.directionId, destination.polyline);
				addStops(route.routeId, destination.directionId, destination.stopItems);
				updateVehicles(route.routeId, destination.directionId, destination.stopItems);
			});
		},
		
		showLocation: function(lat, lng) {
			var location = new google.maps.LatLng(lat, lng);
			map.panTo(location);
			map.setZoom(16);
		}
	};
};
