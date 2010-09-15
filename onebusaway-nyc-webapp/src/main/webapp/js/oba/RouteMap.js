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
	var transitMapType = new google.maps.ImageMapType({
	     getTileUrl: function(coord, zoom) {
	         return 'http://mt1.google.com/vt/lyrs=m@132,transit|vm:1&hl=en&opts=r&x=' + coord.x + '&y=' + coord.y + '&z=' + zoom + '&s=Galileo'; 
	     },
         tileSize: new google.maps.Size(256, 256),
         opacity:1.0,
         maxZoom: 17,
         minZoom: 15,
         name: 'Transit',
         isPng: true,
         alt: ''
	});

    var defaultMapOptions = {
      zoom: 12,
      mapTypeControl: false,
	  navigationControlOptions: { style: google.maps.NavigationControlStyle.SMALL },
      center: new google.maps.LatLng(40.70988943430561,-73.96564720877076),
      mapTypeId: 'transit'
    };
	
    var options = jQuery.extend({}, defaultMapOptions, mapOptions || {});

    var map = new google.maps.Map(mapNode, options);

    map.mapTypes.set('transit',transitMapType);
    
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
        var routesToRequest;
 
        if (typeof routeIds === "undefined") {
          // to be more efficient we can store the list of all route ids
          // separately in memory, yagni now
          routesToRequest = [];
        
          for (var routeId in routeIdToShapes) {
            routesToRequest.push(routeId);
          }
        } else {
          routesToRequest = routeIds;
        }
        
        // we'll be serializing the requests to be one at a time, so this is the base case
        if (routesToRequest.length == 0)
            return;

        var routeId = routesToRequest[0];
        var remainingRouteIds = routesToRequest.slice(1);
        var url = OBA.Config.vehiclesUrl + "/" + routeId + ".json";
        jQuery.getJSON(url, {version: 2, key: OBA.Config.apiKey}, function(json) {
            try {
                var tripDetailsList = json.data.list;
            } catch (typeError) {
                OBA.Util.log("unknown server vehicles response");
                OBA.Util.log(json);
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
 
          jQuery.each(tripDetailsList, function(i, tripDetails) {
              // we only show the vehicles that have been predicted and have an id
              var status = tripDetails.status;
              var predicted = status.predicted;
              var vehicleId = status.vehicleId;
              if (!predicted || (!vehicleId)) {
                  OBA.Util.log("not predicted or no vehicle id: skipping");
                  return;
              }

              var latLng = [status.position.lat, status.position.lon];
              var vehicleMarker = vehicleMarkers[vehicleId];
            
              if (vehicleMarker) {
                var latlng = new google.maps.LatLng(latLng[0], latLng[1]);
  
                vehicleMarker.updatePosition(latlng);

                if (!vehicleMarker.isDisplayed())
                    vehicleMarker.addMarker();

                addVehicleMarkerToRouteMap(routeId, vehicleMarker);
              } else {
                vehicleMarker = OBA.VehicleMarker(vehicleId, latLng, map);
                vehicleMarkers[vehicleId] = vehicleMarker;
 
                addVehicleMarkerToRouteMap(routeId, vehicleMarker);
              }
            }); // each tripDetail
          // handle the remaining route ids
          // this is done in this way to serialize the requests to the server
          requestRoutes(remainingRouteIds);
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
        var minLatLng = mapBounds.getSouthWest(), centerLatLng = mapBounds.getCenter();
        var latSpan = Math.abs(centerLatLng.lat() - minLatLng.lat()) * 2;
        var lonSpan = Math.abs(centerLatLng.lng() - minLatLng.lng()) * 2;
        jQuery.getJSON(OBA.Config.stopsUrl,
                       {version: 2, key: OBA.Config.apiKey,
                        lat: centerLatLng.lat(), lon: centerLatLng.lng(), latSpan: latSpan, lonSpan: lonSpan
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
      addRoute: function(routeId, shape) {
        if (routeId in routeIdToShapes)
            return;

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
                strokeWeight: 5
            });
            polyline.setMap(map);
            polylines.push(polyline);
        });


        routeIdToShapes[routeId] = polylines;

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
        var polylines = routeIdToShapes[routeId];

        if (polylines) {
          delete routeIdToShapes[routeId];
          numberOfRoutes -= 1;
          jQuery.each(polylines, function(_, polyline) {
              polyline.setMap(null);
          });
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
          var polylines = routeIdToShapes[routeId];
          if (!polylines) return null;

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
