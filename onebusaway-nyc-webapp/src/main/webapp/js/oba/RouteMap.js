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
      zoom: 12,
      mapTypeControl: false,
	  navigationControlOptions: { style: google.maps.NavigationControlStyle.SMALL },
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
	var routeIds = {};
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
              var alreadyThere = false;
              var markerId = vehicleMarker.getId();

              jQuery.each(vehicles, function(i, vehicle) {
                if (vehicle.getId() === markerId)
                  alreadyThere = true;
              });

              if (!alreadyThere)
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
        // calculate the request lat/lon and spans to use for the request
        var mapBounds = map.getBounds();
        var minLatLng = mapBounds.getSouthWest(), maxLatLng = mapBounds.getNorthEast();
        var lat = minLatLng.lat(), lon = minLatLng.lng();
        var latSpan = Math.abs(maxLatLng.lat() - lat) * 2;
        var lonSpan = Math.abs(maxLatLng.lng() - lon) * 2;

        jQuery.getJSON(OBA.Config.stopsUrl,
                       {version: 2, key: OBA.Config.apiKey,
                        lat: lat, lon: lon, latSpan: latSpan, lonSpan: lonSpan
                        },
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
                    var latlng = [stop.lat, stop.lon];
                    var name = stop.name;

                newStopIds[stopId] = stopId;

                var marker = stopMarkers[stopId];

                if (marker) {
                    marker.updatePosition(new google.maps.LatLng(latlng[0], latlng[1]));
                } else {
                    marker = OBA.StopMarker(stopId, latlng, map, name);
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
            var url = OBA.Config.stopUrl + "/" + stopId + ".json";
            jQuery.getJSON(url, {version: 2, key: OBA.Config.apiKey}, function(json) {
                var stop;
                try {
                    stop = json.data.references.stops[0];
                } catch (typeError) {
                    OBA.Util.log("invalid stop response from server");
                    return;
                }

                var stopId = stop.id;
                var latlng = [stop.lat, stop.lon];
                var marker = OBA.StopMarker(stopId, latlng, map);
                stopMarkers[stopId] = marker;

                map.setCenter(new google.maps.LatLng(latlng[0], latlng[1]));

                marker.showPopup();
            });
        }
      },

      // add and remove shapes also take care of updating the display
      // if this is a problem we can factor this back out
      addRoute: function(routeId, json) {
        if (routeId in routeIdToShapes)
            return;

        var encodedPolylines = json && json.polylines;
        if (!encodedPolylines)
          return;

        // polylines is a list of encoded polylines from the server
        // here we decode them and put them into one list of lat lngs
        var latlngs = [];
        jQuery.each(encodedPolylines, function(_, encodedPolyline) {
            var latlngList = OBA.Util.decodePolyline(encodedPolyline.points);
            var googleLatLngList = jQuery.map(latlngList, function(latlng) {
                return new google.maps.LatLng(latlng[0], latlng[1]);
            });
            jQuery.merge(latlngs, googleLatLngList);
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

		routeIds[routeId] = 1;
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

		delete routeIds[routeId];
      },

	  // FIXME
      getRoutes: function() {
		var a = new Array();
		
		for(var i in routeIds)
			a.push(i);
	
		return a;
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
