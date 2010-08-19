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

OBA.Marker = function(entityId, latlng, map, popup, icon) {
    var markerOptions = {
        position: new google.maps.LatLng(latlng[0], latlng[1]),
        map: map
    };

    if (typeof icon !== "undefined") {
        markerOptions.icon = icon;
    }

    var marker = new google.maps.Marker(markerOptions);
    var showPopup = function() { popup.show(marker); };

    google.maps.event.addListener(marker, "click", showPopup);

    return {
        showPopup: showPopup,
        
        hidePopup: function() {
            popup.hide();
        },
        
        addMarker: function() {
            marker.setMap(map);
        },
        
        removeMarker: function() {
            marker.setMap(null);
        },
        
        updatePosition: function(latlng) {
            marker.setPosition(latlng);
        },
        isDisplayed: function() {
            return marker.getMap() != null;
        }
    };
};

OBA.StopMarker = function(stopId, latlng, map) {
    return OBA.Marker(stopId, latlng, map,
        OBA.StopPopup(stopId, map));
};

OBA.VehicleMarker = function(vehicleId, latlng, map) {
    return OBA.Marker(vehicleId, latlng, map,
        OBA.VehiclePopup(vehicleId, map),
        OBA.Config.vehicleIcon);
};
