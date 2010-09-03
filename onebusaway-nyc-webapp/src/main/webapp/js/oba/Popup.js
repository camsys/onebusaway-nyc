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
    var parseStopId = function(stopId) {
        var idx = stopId.indexOf("_");

        if (idx === -1)
            return stopId;

        return stopId.substring(idx + 1);
    };

    var generateStopMarkup = function(json) {
        var stop, routes, arrivals;
        try {
            stop = json.data.references.stops[0];
            routes = json.data.references.routes;
            arrivals = json.data.entry.arrivalsAndDepartures;
        } catch (typeError) {
            OBA.Util.log("invalid stop response from server");
            OBA.Util.log(json);
            return;
        }

        // stop information
        var stopId = stop.id;
        var latlng = [stop.lat, stop.lon];
        var name = stop.name;

        // vehicle location
        var routeToVehicleInfo = {};
        jQuery.each(arrivals, function(_, arrival) {
                var routeId = arrival.routeId;
                var headsign = arrival.tripHeadsign;
                // FIXME stops and distance away
                var stops = 0;
                var feet = 0;

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
                     '<p class="description">Stop ID ' + parseStopId(stopId) + '</p>' + 
                     '<p class="meta">Updated ' + lastUpdate + '.</p>';

        var service = '';
        var notices = '<ul class="notices">';

        if(routes.length > 0) {
            service += '<p>This stop serves:</p><ul>';

            jQuery.each(routes, function(i, route) {
                var routeId  = route.id;
                var shortName = route.shortName;
                var longName = route.longName;

                // FIXME service notices
                var serviceNotice = null;
                if (serviceNotice)
                    notices += '<li>' + serviceNotice + '</li>';

                // routes with a service notice should appear red
                service += (serviceNotice ? '<li class="hasNotice">' : "<li>");

                service += '<a href="#" class="searchLink" rel="' + routeId + '">'
                          + OBA.Util.truncate(routeId + ' - ' + longName, 30) + '</a>';

                // and the distance away for each vehicle for that route
                var vehicleInfos = routeToVehicleInfo[routeId] || [];
                jQuery.each(vehicleInfos, function(_, distanceAway) {
                    service += ' (' + distanceAway.stops + ' stops, ' + distanceAway.feet + ' ft.)';
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
}

OBA.VehiclePopup = function(vehicleId, map) {
    var generateVehicleMarkup = function(json) {
        var vehicle = json.vehicle;
        
        if (!vehicle) 
            return null;
        
        var header = '<p class="header' + ((typeof vehicle.serviceNotice !== 'undefined') ? ' hasNotice' : '') + '">' + OBA.Util.truncate(vehicle.routeId + ' - ' + vehicle.description, 35) + '</p>' +
             '<p class="description">Bus #' + vehicle.vehicleId + '</p>' + 
             '<p class="meta">Updated ' + vehicle.lastUpdate + '.</p>';

        var notices = '<ul class="notices">';

        if(typeof vehicle.serviceNotice !== 'undefined') {
            notices += '<li>' + vehicle.serviceNotice + '</li>';
        }

        notices += '</ul>';
            
        var nextStops = '';
        if(typeof vehicle.nextStops !== 'undefined') {
            nextStops += '<p>Next stops:<ul>';       

            jQuery.each(vehicle.nextStops, function(i, stop) {
                var stopId = stop.stopId;

                nextStops += '<li><a href="#" class="searchLink" rel="' + stopId + '">' + OBA.Util.truncate(stop.name, 30) + '</a> (' + stop.distanceAway.stops + ' stops, ' + stop.distanceAway.feet + ' ft.)</li>';
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
    
    return OBA.Popup(
        map,
        makeJsonFetcher(OBA.Config.vehicleUrl, {vehicleId: vehicleId}),
        generateVehicleMarkup);
};
