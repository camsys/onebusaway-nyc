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
        var stop = json.stop;
        
        if (!stop) 
            return null;
        
        var header = '<p class="header">' + stop.name + '</p>' +
                     '<p class="description">Stop ID ' + stop.stopId + '</p>' + 
                     '<p class="meta">Updated ' + stop.lastUpdate + '.</p>';
            
        var service = '';           
        var notices = '<ul class="notices">';
        if(typeof stop.routesAvailable !== 'undefined') {
            service += '<p>This stop serves:</p><ul>';       

            jQuery.each(stop.routesAvailable, function(i, route) {
                var routeId  = route.routeId;

                if(typeof route.serviceNotice !== 'undefined') {
                    notices += '<li>' + route.serviceNotice + '</li>';
                }
                
                // routes with a service notice should appear red
                var liHeading = (typeof route.serviceNotice === "undefined")
                        ? "<li>"
                		: '<li class="hasNotice">';
                
                jQuery.each(route.distanceAway, function(j, distanceAway) {
                	service += liHeading;
                    service += '<a href="#" class="searchLink" rel="' + routeId + '">' + OBA.Util.truncate(routeId + ' - ' + route.description, 30) + '</a> (' + distanceAway.stops + ' stops, ' + distanceAway.feet + ' ft.)</li>';
                });
           });
           
           service += '</ul>';
        }
        
        notices += '</ul>';
    
        bubble = jQuery(header + notices + service);

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
        makeJsonFetcher(OBA.Config.stopUrl, {stopId: stopId}),
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
