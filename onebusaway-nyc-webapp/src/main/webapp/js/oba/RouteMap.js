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
         minZoom: 12,
         name: 'Transit',
         isPng: true,
         alt: ''
	});

    var defaultMapOptions = {
      zoom: 13,
      mapTypeControl: false,
	  navigationControlOptions: { style: google.maps.NavigationControlStyle.SMALL },
      center: new google.maps.LatLng(40.65182926199445,-74.0065026164856),
      mapTypeId: 'transit'
    };
	
    var options = jQuery.extend({}, defaultMapOptions, mapOptions || {});

    var map = new google.maps.Map(mapNode, options);
    var fluster = new Fluster2(map);

    fluster.gridSize = 40;
	fluster.styles = {
			0: {
				image: OBA.Config.stopIconFile,
				showCount: false,
				width: 14,
				height: 14
			}
	};
        
    map.mapTypes.set('transit',transitMapType);
    
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
              var directionIdMap = routeIdToShapes[routeId];
              for (var directionId in directionIdMap) {
                  routesToRequest.push([routeId, directionId]);                  
              }
          }
        } else {
          routesToRequest = routeIds;
        }
        
        // we'll be serializing the requests to be one at a time, so this is the base case
        if (routesToRequest.length == 0)
            return;

        var routeId = routesToRequest[0][0];
        var directionId = routesToRequest[0][1];
        var remainingRouteIds = routesToRequest.slice(1);
        var url = OBA.Config.vehiclesUrl + "/" + routeId + ".json";
        jQuery.getJSON(url, {version: 2, key: OBA.Config.apiKey}, function(json) {
            try {
                var tripDetailsList = json.data.list;
                var tripReferencesList = json.data.references.trips;
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
                        if (vehicle.getId() === markerId)
                            alreadyThere = true;
                    });

                    if (!alreadyThere)
                        vehicles.push(vehicleMarker);
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
              // we only show the vehicles that have been predicted and have an id
              var status = tripDetails.status;
              var predicted = status.predicted;
              var vehicleId = status.vehicleId;
              if (!predicted || (!vehicleId)) {
                  return;
              }
              
              // check to make sure that the bus is headed in the right direction
              var vehicleTripId = tripDetails.tripId;
              for (var i = 0; i < tripReferencesList.length; i++) {
                  var tripReference = tripReferencesList[i];
                  if (tripReference.id === vehicleTripId) {
                      var vehicleDirectionId = tripReference.directionId;
                      if (vehicleDirectionId !== directionId)
                          return;
                      break;
                  }
              }

              var latLng = [status.position.lat, status.position.lon];
              var vehicleMarker = vehicleMarkers[vehicleId];
            
              if (vehicleMarker) {
                var latlng = new google.maps.LatLng(latLng[0], latLng[1]);
  
                vehicleMarker.updatePosition(latlng);

                if (!vehicleMarker.isDisplayed())
                    vehicleMarker.addMarker();

                addVehicleMarkerToRouteMap(routeId, directionId, vehicleMarker);
              } else {
                vehicleMarker = OBA.VehicleMarker(vehicleId, latLng, map, { map: map });
                vehicleMarkers[vehicleId] = vehicleMarker;
 
                addVehicleMarkerToRouteMap(routeId, directionId, vehicleMarker);
              }
            }); // each tripDetail
          
          // remove vehicle markers that haven't been listed in this recent update
          var directionIdMap = routeIdsToVehicleMarkers[routeId];
          if (directionIdMap) {
              var vehicles = directionIdMap[directionId];
              if (vehicles) {
                  var vehiclesToKeep = [];
                  for (var i = 0; i < vehicles.length; i++) {
                      var vehicle = vehicles[i];
                      if (vehicle.getId() in vehiclesAdded)
                          vehiclesToKeep.push(vehicle);
                      else
                          vehicle.removeMarker();
                  }
                  routeIdsToVehicleMarkers[routeId][directionId] = vehiclesToKeep;
              }
          }
          
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
                       {version: 2, key: OBA.Config.apiKey, maxCount: 250,
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
                    marker = OBA.StopMarker(stopId, latlng, map);
                    
                    fluster.addMarker(marker.getRawMarker());
                    
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
            
            fluster.initialize();            
    	});
    };

    google.maps.event.addListener(map, "idle", requestStops);   
    
    var containsRoute = function(routeId, directionId) {
        var directionIdMap = routeIds[routeId] || {};
        return directionId in directionIdMap;
    }

    return {
      getMap: function() { return map; },

      containsRoute: containsRoute,

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
        
        // and we have to make sure that the zoom level is close enough so that we can see stops
        var currentZoom = map.getZoom();

        if (currentZoom < 15)
            map.setZoom(15);
      },

      // add and remove shapes also take care of updating the display
      // if this is a problem we can factor this back out
      addRoute: function(routeId, directionId, shape) {
        if (containsRoute(routeId, directionId))
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

        // always make an initial request just for this route
        requestRoutes([[routeId, directionId]]);

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
      },

      removeRoute: function(routeId, directionId) {
        var directionIdMap = routeIdToShapes[routeId] || {};
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
      },

      getRoutes: function() {
		var result = [];
		for (var routeId in routeIds) {
		    var directionIdMap = routeIds[routeId];
		    for (var directionId in directionIdMap) {
		        result.push([routeId, directionId]);
		    }
		}
		return result;
      },
      
      removeAllRoutes: function() {
          for (var routeId in routeIds) {
              var directionIdMap = routeIds[routeId];
              for (var directionId in directionIdMap) {
                  removeRoute(routeId, directionId);
              }
          }
      },
 
      getCount: function() {
          return numberOfRoutes;
      },

      getBounds: function(routeId, directionId) {
          var directionIdMap = routeIdToShapes[routeId] || {};
          var polylines = directionIdMap[directionId];
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
