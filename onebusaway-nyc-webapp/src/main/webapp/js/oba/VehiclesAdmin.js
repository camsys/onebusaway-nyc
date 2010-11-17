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

(function() {

var OBA = window.OBA || {};

// state to keep track of vehicle id -> latlng
var latlngs = {};

function _get_vehicle_table() {
    return jQuery('#vehicle-table');
}

function addTableSortBehaviors() {
    var table = _get_vehicle_table();
    // for reverse sorting
    var lastSortIndex = -1;
    table.find('th a').each(function(i, a) {
        var keyFn = function(row) {
            var key = jQuery(row).children().slice(i, i+1).text();
            return key;
        };
        jQuery(a).click(function(e) {
            e.preventDefault();
            var rows = table.find('tr').not(":first");
            rows.remove();
            rows = jQuery.makeArray(rows);
            rows.sort(function(a, b) {
                var x = keyFn(a);
                var y = keyFn(b);
                if (x < y) return -1;
                else if (x > y) return 1;
                else return 0;
            });
            if (lastSortIndex === i) {
                rows.reverse();
                lastSortIndex = -1;
            } else {
                lastSortIndex = i;
            }
            table.append(rows);
            // when we re-add the rows we lose behavior
            addShowMapBehavior();
        });
    });
}

function _createMapOverlay() {
    var div = jQuery('<div id="map-overlay"><a href="#">Close</a><div id="google-map"></div></div>');
    return div;
}

function _removePreviousMaps() {
    jQuery("#map-overlay").remove();
}

function _addGoogleMap(lat, lng) {
    var latlng = new google.maps.LatLng(lat, lng);
    var mapOptions = {
      zoom: 16,
      center: latlng,
      mapTypeControl: false,
      navigationControlOptions: { style: google.maps.NavigationControlStyle.SMALL },
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    var map = new google.maps.Map(document.getElementById("google-map"), mapOptions);
    var marker = new google.maps.Marker({
        position: latlng, 
        map: map
    });
}

function _makeMiniMap(lat, lng, x, y) {
    var popup = _createMapOverlay(lat, lng);
    popup.css({top: y, left: x});
    popup.appendTo(jQuery("body"));
    popup.find('a').click(function() {
        popup.remove();
        return false;
    });
    _addGoogleMap(lat, lng);
}

function _vehicleIdForCell(elt) {
    // for a given cell (jquery elt), find the vehicleId for that row
    var td = elt;
    var node = elt.get(0);
    if (node.nodeName.toUpperCase !== "TD") {
        td = elt.parents("td").first();
    }
    if (!td.hasClass("vehicleId")) {
        td = td.siblings(".vehicleId");
    }
    var vehicleId = td.text();
    return vehicleId;
}

function tweakLatLngsAndPrepareData() {
    var table = _get_vehicle_table();
    table.find('.map-location').each(function(i, span) {
        var span = jQuery(span);
        var a = span.siblings('a');
        var location = span.text();
        var fields = location.split(",");
        var lat = fields[0], lng = fields[1];
        var vehicleId = _vehicleIdForCell(span);
        latlngs[vehicleId] = {lat: lat, lng: lng};
        var trimlat = parseFloat(lat).toPrecision("7");
        var trimlng = parseFloat(lng).toPrecision("7");
        span.text(trimlat + "," + trimlng);
    });
}

function addShowMapBehavior() {
    var table = _get_vehicle_table();
    table.find('.map-link').click(function(e) {
        e.preventDefault();
        _removePreviousMaps();
        var td = jQuery(this).parent();
        var tdPosition = td.offset();
        var x = tdPosition.left - 200 - 50;
        var y = tdPosition.top - 100;
        var vehicleId = _vehicleIdForCell(jQuery(this));
        var latlng = latlngs[vehicleId];
        _makeMiniMap(latlng.lat, latlng.lng, x, y);
    });
}

jQuery(document).ready(function() {
    addTableSortBehaviors();
    tweakLatLngsAndPrepareData();
    addShowMapBehavior();
    
    // refresh every 1m
    setTimeout(function() {
    	window.location.href = window.location.href;
    }, 60 * 1000);
});

})();
