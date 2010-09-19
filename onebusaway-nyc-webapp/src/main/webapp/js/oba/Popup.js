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

        // vehicle location
        var routeToVehicleInfo = {};
        jQuery.each(arrivals, function(_, arrival) {
                var routeId = arrival.routeId;
                var arrivalStopId = arrival.stopId;
                var predicted = arrival.predicted;
                if (!predicted || arrivalStopId !== stopId || !(routeId in routeIdMap))
                    return;
                var headsign = arrival.tripHeadsign;
                // FIXME stops and distance away
                var stops = 0;
                var meters = arrival.distanceFromStop;
                var feet = OBA.Util.metersToFeet(meters);

                var vehicleInfo = {headsign: headsign,
                                   stops: stops,
                                   feet: feet};
                if (routeToVehicleInfo[routeId])
                    routeToVehicleInfo[routeId].push(vehicleInfo);
                else
                    routeToVehicleInfo[routeId] = [vehicleInfo];
            });

        // FIXME last update time
        var lastUpdate = "One minute ago";

        var header = '<p class="header">' + name + '</p>' +
                     '<p class="description">Stop ID ' + OBA.Util.parseEntityId(stopId) + '</p>' + 
                     '<p class="meta">Updated ' + lastUpdate + '.</p>';

        var service = '';
        var notices = '<ul class="notices">';

        if(routes.length > 0) {
            service += '<p>This stop serves:</p><ul>';

            jQuery.each(routes, function(i, route) {
                var routeId  = route.id;
                var routeIdDisplay = OBA.Util.parseEntityId(routeId);
                var shortName = route.shortName;
                var longName = route.longName;

                // FIXME service notices
                var serviceNotice = null;
                if (serviceNotice)
                    notices += '<li>' + serviceNotice + '</li>';

                // routes with a service notice should appear red
                service += (serviceNotice ? '<li class="hasNotice">' : "<li>");

                service += '<a href="#" class="searchLink" rel="' + routeIdDisplay + '">'
                          + OBA.Util.truncate(routeIdDisplay + ' - ' + longName, 30) + '</a>';

                // and the distance away for each vehicle for that route
                var vehicleInfos = routeToVehicleInfo[routeId] || [];
                // sort it based on distance
                vehicleInfos.sort(function(a, b) { return a.feet - b.feet; });
                jQuery.each(vehicleInfos, function(_, distanceAway) {
                    // just meter distance for now
//                    service += ' (' + distanceAway.stops + ' stops, ' + distanceAway.feet + ' feet)';
                    var feet = distanceAway.feet;
                    if (feet > 5280) {
                        var distanceMiles = feet / 5280;
                        distanceMiles = distanceMiles.toPrecision(3);
                        service += ' (' + distanceMiles + ' miles)';
                    } else {
                        feet = feet.toPrecision(4);
                        service += ' (' + feet + " feet)";
                    }
                });
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
            var refs = json.data.references;
            var stops = refs.stops;
            var route = refs.routes[0];
        } catch (typeError) {
            OBA.Util.log("invalid response for vehicle details");
            OBA.Util.log(json);
            return jQuery("<span>No data found for: " + vehicleId + "</span>");
        }
        
        // last update date
        var lastUpdateDate = new Date(tripStatus.lastUpdateTime);
        var lastUpdateString = lastUpdateDate.getHours() + ":" + lastUpdateDate.getMinutes();
        
        var header = '<p class="header' + ((typeof tripStatus.serviceNotice !== 'undefined') ? ' hasNotice' : '') + '">' + OBA.Util.truncate(route.id + ' - ' + route.longName, 35) + '</p>' +
             '<p class="description">Bus #' + OBA.Util.parseEntityId(vehicleId) + '</p>' + 
             '<p class="meta">Last updated ' + lastUpdateString + '.</p>';

        var notices = '<ul class="notices">';

        if(typeof tripStatus.serviceNotice !== 'undefined') {
            notices += '<li>' + tripStatus.serviceNotice + '</li>';
        }

        notices += '</ul>';
            
        var nextStops = '';
        if (typeof stops !== 'undefined' && stops.length > 0) {
            nextStops += '<p>Next stops:<ul>';       
            jQuery.each(stops, function(i, stop) {
                var stopId = stop.id;
                var stopName = stop.name;

                // we only have one stop currently
                // this will not work if we get more than one
                // because we reuse the distance information for each

                nextStops += '<li><a href="#" class="searchLink" rel="' + OBA.Util.parseEntityId(stopId) + '">' + OBA.Util.truncate(stopName, 30) + '</a>';
//                nextStops += ' (' + stop.distanceAway.stops + ' stops, ' + stop.distanceAway.feet + ' ft.)</li>';
           });
    
           nextStops += '</ul>';
        }

        bubble = jQuery(header + notices + nextStops);
        
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
        makeJsonFetcher(url, {key: OBA.Config.apiKey, version: 2}),
        generateVehicleMarkup);
};
