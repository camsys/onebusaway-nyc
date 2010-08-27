// Copyright 2010, OpenPlans
// Licensed under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

var OBA = window.OBA || {};

OBA.RouteMap = function(mapNode, mapOptions) {

    var defaultMapOptions = {
      zoom: 15,
      mapTypeControl: false,
      center: new google.maps.LatLng(40.70988943430561,-73.96564720877076),
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    var options = jQuery.extend({}, defaultMapOptions, mapOptions || {});

    var map = new google.maps.Map(mapNode, options);

    if (OBA.Config.debug) {
        google.maps.event.addListener(map, "click", function(e) {
          if (console && console.log)
            console.log(e.latLng.lat() + "," + e.latLng.lng());
        });
    }

    // state used for the map
    var routeIdToShapes = {};
    var routeIdsToVehicleMarkers = {};
    var stopMarkers = {};
    var numberOfRoutes = 0;
    var vehicleMarkers = {};
    var isVehiclePolling = false;
    var vehicleTimerId = null;
 
    var requestRoutes = function(routeIds) {
        var routesToSerialize;
 
        if (typeof routeIds === "undefined") {
          // to be more efficient we can store the list of all route ids
          // separately in memory, yagni now
          routesToSerialize = [];
        
          for (var routeId in routeIdToShapes) {
            routesToSerialize.push(routeId);
          }
        } else {
          routesToSerialize = routeIds;
        }
        
        // struts doesn't like the [] request syntax
        // so we serialize the request manually here
        var serializedRoutes = OBA.Util.serializeArray(routesToSerialize, "routeIds");
  
        jQuery.getJSON(OBA.Config.vehiclesUrl, serializedRoutes, function(json) {
          var vehicles = json.vehicles;
          
          if (!vehicles) {
            return;
          }
          
          // helper function to add an element to a map where values are lists
          var addVehicleMarkerToRouteMap = function(routeId, vehicleMarker) {
            var vehicles = routeIdsToVehicleMarkers[routeId];

            if (vehicles) {
              vehicles.push(vehicleMarker);
            } else {
              vehicles = [vehicleMarker];
              routeIdsToVehicleMarkers[routeId] = vehicles;
            }
          };
 
          jQuery.each(vehicles, function(i, vehicleSection) {
            var routeId = vehicleSection.routeId;
  
            jQuery.each(vehicleSection.vehicles, function(i, vehicle) {
              var vehicleMarker = vehicleMarkers[vehicle.vehicleId];
            
              if (vehicleMarker) {
                var latlng = new google.maps.LatLng(vehicle.latLng[0], vehicle.latLng[1]);
  
                vehicleMarker.updatePosition(latlng);
                if (!vehicleMarker.isDisplayed())
                    vehicleMarker.addMarker();
                addVehicleMarkerToRouteMap(routeId, vehicleMarker);
              } else {
                vehicleMarker = OBA.VehicleMarker(vehicle.vehicleId, vehicle.latLng, map);
                vehicleMarkers[vehicle.vehicleId] = vehicleMarker;
 
                addVehicleMarkerToRouteMap(routeId, vehicleMarker);
              }
            }); // each vehicleSection
          }); // each vehicles
        }); // getJSON
    }; // requestRoutes

    var vehiclePollingTask = function() {
        if (!isVehiclePolling) {
            return;
        }

        requestRoutes();

        vehicleTimerId = setTimeout(vehiclePollingTask, OBA.Config.pollingInterval);
    };
    
    var requestStops = function() {
    	var mapBounds = map.getBounds();
    	var minLatLng = mapBounds.getSouthWest();
    	var maxLatLng = mapBounds.getNorthEast();
        jQuery.getJSON(OBA.Config.stopsUrl,
        		{minLat: minLatLng.lat(), minLng: minLatLng.lng(),
        	     maxLat: maxLatLng.lat(), maxLng: maxLatLng.lng()},
        	    function(json) {
            var stops = json.stops;

            if (!stops)
              return;

            // keep track of the new ids that came in so we can remove the old ones
            // that are no longer shown
            var newStopIds = {};
            jQuery.each(stops, function(i, stop) {
                var stopId = stop.stopId;
            	
                newStopIds[stopId] = stopId;
            	
            	var marker = stopMarkers[stopId];
            	if (marker) {
            	    marker.updatePosition(stop.latlng);
            	} else {
            	    marker = OBA.StopMarker(stop.stopId, stop.latlng, map, stop.name);
            	    stopMarkers[stopId] = marker;
            	}
            });
            
            // remove the old markers that aren't currently shown
            for (var stopId in stopMarkers) {
                var marker = stopMarkers[stopId];
                if (!newStopIds[stopId]) {
                    marker.removeMarker();
                    delete stopMarkers[stopId];
                }
            }
         });
    };
    
    google.maps.event.addListener(map, "idle", requestStops);

    return {
      getMap: function() { return map; },

      containsRoute: function(routeId) {
        return routeId in routeIdToShapes;
      },
      
      showStop: function(stopId) {
    	  if (stopMarkers[stopId]) {
    		  // stop marker is already on map, can just display the popup
    	      var stopMarker = stopMarkers[stopId];
    	      stopMarker.showPopup();
    	  } else {
    	      jQuery.getJson(OBA.config.stopUrl, {stopId: stopId}, function(json) {
    	          var stop = json.stop;
    	          if (!stop)
    	              return;
    	          
    	          var marker = OBA.StopMarker(stopId, stop.latlng, map);
    	          stopMarkers[stopId] = marker;
    	          
    	          map.setCenter(new google.maps.LatLng(stop.latlng[0], stop.latlng[1]));
    	      });
    		  // will need to make another json request for the stop lat/lng
    		  // zoom the map there, create the marker, and then display the popup
    	  }
      },
  
      // add and remove shapes also take care of updating the display
      // if this is a problem we can factor this back out
      addRoute: function(routeId, json) {    
        var coords = json.route && json.route.polyLine;
          
        if (! coords)
          return;

        var latlngs = jQuery.map(coords, function(x) {
            return new google.maps.LatLng(x[0], x[1]);
        });

        var shape = new google.maps.Polyline({
              path: latlngs,
              strokeColor: "#0000FF",
              strokeOpacity: 0.5,
              strokeWeight: 5
        });
          
        routeIdToShapes[routeId] = shape;
        shape.setMap(map);
 
        numberOfRoutes += 1;
 
        // always make an initial request just for this route
        requestRoutes([routeId]);
 
        // update the timer task
        if (!isVehiclePolling) {
          isVehiclePolling = true;
          vehicleTimerId = setTimeout(vehiclePollingTask, OBA.Config.pollingInterval);
        }
      },
 
      removeRoute: function(routeId) {
        var shape = routeIdToShapes[routeId];
 
        if (shape) {
          delete routeIdToShapes[routeId];
          numberOfRoutes -= 1;
          shape.setMap(null);
        }
 
        var vehicles = routeIdsToVehicleMarkers[routeId];
  
        if (vehicles) {
          jQuery.each(vehicles, function(i, vehicleMarker) {
            vehicleMarker.removeMarker();
          });
 
          delete routeIdsToVehicleMarkers[routeId];
        }

        if (numberOfRoutes <= 0) {
            isVehiclePolling = false;
            if (vehicleTimerId) {
                clearTimeout(vehicleTimerId);
            }
        }
      },
 
      getCount: function() {
          return numberOfRoutes;
      },

      getBounds: function(routeId) {
          var shape = routeIdToShapes[routeId];
          if (!shape) return null;

          var latlngBounds = new google.maps.LatLngBounds();
          var path = shape.getPath();
          for (var i = 0; i < path.length; i++) {
              var latlng = path.getAt(i);
              latlngBounds.extend(latlng);
          }
          return latlngBounds;
      }
    };
};
