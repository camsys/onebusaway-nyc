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

OBA.theInfoWindow = null;

OBA.Popup = function(map, fetchFn, bubbleNodeFn) {
    return {
        show: function(marker) {    
            if(OBA.theInfoWindow)
                OBA.theInfoWindow.close();
            
            fetchFn(function(json) {
                OBA.theInfoWindow = new google.maps.InfoWindow();
    
                // we need to append this node to the map for the size to be calculated properly
                wrappedContent = jQuery('<div id="popup"></div>')
                                    .append(bubbleNodeFn(json))
                                    .appendTo("#map");
                                                        
                wrappedContent = wrappedContent.css("width", 250).css("height", wrappedContent.height());
                        
                OBA.theInfoWindow.setContent(wrappedContent.get(0));                
                OBA.theInfoWindow.open(map, marker);
            });
        }
    };
};

// utilities? scope wrap to prevent global leak?
function makeJsonFetcher(url, data) {
    return function(callback) {
        jQuery.getJSON(url, data, callback);
    };
}

OBA.StopPopup = function(stopId, map) {
    var generateStopMarkup = function(json) {
        var stop, routeIds, routeIdMap, routeReferences, routes, arrivals;
        try {
            stop = json.data.references.stops[0];
            routeIds = stop.routeIds;
            routeReferences = json.data.references.routes;
            arrivals = json.data.entry.arrivalsAndDepartures;
        } catch (typeError) {
            OBA.Util.log("invalid stop response from server");
            OBA.Util.log(json);
            return;
        }
        routeIdMap = {};
        jQuery.each(routeIds, function(_, routeId) {
            routeIdMap[routeId] = routeId;
        });
        routes = [];
        jQuery.each(routeReferences, function(_, routeReference) {
            if (routeReference.id in routeIdMap) {
                routes.push(routeReference);
            }
        });

        // stop information
        var stopId = stop.id;
        var latlng = [stop.lat, stop.lon];
        var name = stop.name;
        var latestUpdate = null;

        // vehicle location
        var routeToVehicleInfo = {};
        jQuery.each(arrivals, function(_, arrival) {
                var routeId = arrival.routeId;
                var arrivalStopId = arrival.stopId;
                var predicted = arrival.predicted;
                if (!predicted || arrivalStopId !== stopId || !(routeId in routeIdMap))
                    return;
                var headsign = arrival.tripHeadsign;
                var updateTime = parseInt(arrival.lastUpdateTime);
                latestUpdate = latestUpdate ? Math.max(latestUpdate, updateTime) : updateTime;
                // only show positive distances
                var meters = arrival.distanceFromStop;
                if (meters < 0)
                    return;
                var feet = OBA.Util.metersToFeet(meters);
                var stops = arrival.numberOfStopsAway + 1;

                var vehicleInfo = {headsign: headsign,
                                   stops: stops,
                                   feet: feet};
                if (routeToVehicleInfo[routeId])
                    routeToVehicleInfo[routeId].push(vehicleInfo);
                else
                    routeToVehicleInfo[routeId] = [vehicleInfo];
            });

        // last update time
        var lastUpdateDate = new Date(latestUpdate);
        var lastUpdateString = OBA.Util.displayTime(lastUpdateDate);


        var header = '<p class="header">' + name + '</p>' +
                     '<p class="description">Stop ID ' + OBA.Util.parseEntityId(stopId) + '</p>' + 
                     '<p class="meta">Updated ' + lastUpdateString + '.</p>';

        var service = '';
        var notices = '<ul class="notices">';

        if(routes.length > 0) {
            service += '<p>This stop serves:</p><ul>';

            jQuery.each(routes, function(_, route) {
                var routeId  = route.id;
                var routeIdDisplay = OBA.Util.parseEntityId(routeId);
                var shortName = route.shortName;
                var longName = route.longName;

                // FIXME service notices
                var serviceNotice = null;
                if (serviceNotice)
                    notices += '<li>' + serviceNotice + '</li>';

                // and the distance away for each vehicle for that route
                var vehicleInfos = routeToVehicleInfo[routeId] || [];
                // sort it based on distance
                vehicleInfos.sort(function(a, b) { return a.feet - b.feet; });
                for (var i = 0; i < Math.min(vehicleInfos.length, 3); i++) {
                    // routes with a service notice should appear red
                    service += (serviceNotice ? '<li class="hasNotice">' : "<li>");

                    service += '<a href="#" class="searchLink" rel="' + routeIdDisplay + '">'
                              + OBA.Util.truncate(routeIdDisplay + ' - ' + longName, 30) + '</a>';

                    var distanceAway = vehicleInfos[i];
                    service += " (" + distanceAway.stops + " stop" + ((distanceAway.stops === 1) ? "" : "s") + ", "; 
                    service += OBA.Util.displayDistance(distanceAway.feet) + ")";
                }
                if (vehicleInfos.length === 0) {
                    // need to print out the route name if we don't have any arrivals though
                    service += (serviceNotice ? '<li class="hasNotice">' : "<li>");

                    service += '<a href="#" class="searchLink" rel="' + routeIdDisplay + '">'
                              + OBA.Util.truncate(routeIdDisplay + ' - ' + longName, 30) + '</a>';
                }
                service += '</li>';
           });

           service += '</ul>';
        }

        notices += '</ul>';

        var bubble = jQuery(header + notices + service);

        bubble.find("a.searchLink").click(function(e) {
            e.preventDefault();

            var id = jQuery(this).attr("rel");
            var searchForm = jQuery("#search form");
            var searchInput = jQuery("#search input[type=text]");

            searchInput.val(id);
            searchForm.submit();

            return false;
        });

        return bubble;
    };

    var url = OBA.Config.stopUrl + "/" + stopId + ".json";
    return OBA.Popup(
        map,
        makeJsonFetcher(url, {version: 2, key: OBA.Config.apiKey}),
        generateStopMarkup);
};

OBA.VehiclePopup = function(vehicleId, map) {
    var generateVehicleMarkup = function(json) {
        try {
            var tripDetails = json.data.entry;
            var tripStatus = tripDetails.status;
            var stopTimes = tripDetails.schedule.stopTimes;
            var refs = json.data.references;
            var stops = refs.stops;
            var route = refs.routes[0];
            var trip = refs.trips[0];
            var headsign = trip.tripHeadsign;
        } catch (typeError) {
            OBA.Util.log("invalid response for vehicle details");
            OBA.Util.log(json);
            return jQuery("<span>No data found for: " + vehicleId + "</span>");
        }

        // last update date
        var lastUpdateDate = new Date(tripStatus.lastUpdateTime);
        var lastUpdateString = OBA.Util.displayTime(lastUpdateDate);

        var stops = stops.slice(0);
        
        var stopIdsToStops = {};
        jQuery.each(stops, function(_, stop) {
            stopIdsToStops[stop.id] = stop;
        });
        
        // calculate the distances along the trip for all stops
        jQuery.each(stopTimes, function(_, stopTime) {
            var stopId = stopTime.stopId;
            var stop = stopIdsToStops[stopId];
            stop.scheduledDistance = stopTime.distanceAlongTrip;
        });
        
        // sort the stops by their distance along the route
        stops.sort(function(a, b) {
            return a.scheduledDistance - b.scheduledDistance;
        });
        
        // find how far along the trip we are given our current vehicle distance
        var distanceAlongTrip = tripStatus.distanceAlongTrip;
        var vehicleDistanceIdx = 0;
        for (var i = 0; i < stops.length; i++) {
            var stop = stops[i];
            var stopDistance = stop.scheduledDistance;
            var distanceDelta = distanceAlongTrip - stopDistance;
            if (distanceDelta <= 0) {
                vehicleDistanceIdx = i;
                break;
            }
        }
        
        // and we take the next 3 stops for display
        var nextStops = stops.slice(vehicleDistanceIdx, vehicleDistanceIdx + 3);

        var header = '<p class="header' + ((typeof tripStatus.serviceNotice !== 'undefined') ? ' hasNotice' : '') + '">' + OBA.Util.truncate(headsign, 28) + '</p>' +
             '<p class="description">Bus #' + OBA.Util.parseEntityId(vehicleId) + '</p>' + 
             '<p class="meta">Last updated ' + lastUpdateString + '.</p>';

        var notices = '<ul class="notices">';

        if(typeof tripStatus.serviceNotice !== 'undefined') {
            notices += '<li>' + tripStatus.serviceNotice + '</li>';
        }

        notices += '</ul>';
            
        var nextStopsMarkup = '';
        
        if (nextStops && nextStops.length > 0) {
            nextStopsMarkup += '<p>Next stops:<ul>';       
            jQuery.each(nextStops, function(i, stop) {
                var displayStopId = OBA.Util.parseEntityId(stop.id);
                var stopName = stop.name;

                // we only have one stop currently
                // this will not work if we get more than one
                // because we reuse the distance information for each
                var stopsAway = i+1;
                var stopsAwayStr = (stopsAway === 1) ? "1 stop" : stopsAway + " stops";
                var metersDistanceDelta = stop.scheduledDistance - distanceAlongTrip;
                var feet = OBA.Util.metersToFeet(metersDistanceDelta);
                var distanceStr = OBA.Util.displayDistance(feet);

                nextStopsMarkup += '<li><a href="#" class="searchLink" rel="' + displayStopId + '">' + OBA.Util.truncate(stopName, 30) + '</a>';
                nextStopsMarkup += ' (' + stopsAwayStr + ' ' + distanceStr + ')</li>';
           });
    
           nextStopsMarkup += '</ul>';
        }

        bubble = jQuery(header + notices + nextStopsMarkup);
        
        bubble.find("a.searchLink").click(function(e) {
            e.preventDefault();

            var id = jQuery(this).attr("rel");
            var searchForm = jQuery("#search form");
            var searchInput = jQuery("#search input[type=text]");

            searchInput.val(id);
            searchForm.submit();

            return false;
        });
        
        return bubble;
    };
    
    var url = OBA.Config.vehicleUrl + "/" + vehicleId + ".json";
    return OBA.Popup(
        map,
        makeJsonFetcher(url, {key: OBA.Config.apiKey, version: 2, includeSchedule: true, includeTrip: true}),
        generateVehicleMarkup);
};
