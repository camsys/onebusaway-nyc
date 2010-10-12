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

OBA.Marker = function(entityId, latlng, map, popup, extraMarkerOptions) {
	var lastPosition = null;
	
	var markerOptions = {
        position: new google.maps.LatLng(latlng[0], latlng[1])
    };

    if (typeof extraMarkerOptions !== "undefined") {
        jQuery.extend(markerOptions, extraMarkerOptions);
    }

    var marker = new google.maps.Marker(markerOptions);
    var showPopup = function() { popup.show(marker); };

    google.maps.event.addListener(marker, "click", showPopup);

    return {
        showPopup: showPopup,
        
        getHeading: function() {
        	if(lastPosition === null)
        		return -1;

        	p1 = marker.getPosition();
        	p2 = lastPosition;

        	var lata = p2.lat() * (Math.PI / 180);
        	var latb = p1.lat() * (Math.PI / 180);
        	var lnga = p2.lng() * (Math.PI / 180);
        	var lngb = p1.lng() * (Math.PI / 180);
        	
        	// source: http://mathforum.org/library/drmath/view/55417.html
        	var b = (Math.atan2(Math.cos(lata) * Math.sin(latb) - Math.sin(lata) * Math.cos(latb)
                     * Math.cos(lngb - lnga), Math.sin(lngb - lnga) * Math.cos(latb))) % (180 * Math.PI);

        	// convert to degrees
        	b = Math.floor(b * (180 / Math.PI));

        	if(b < 0)
        		b += 360;
        	
        	return b;
        },

        getRawMarker: function() {
    		return marker;
    	},
    	
        hidePopup: function() {
            popup.hide();
        },
        
        addMarker: function() {
            marker.setMap(map);
        },
        
        removeMarker: function() {
            marker.setMap(null);
        },
        
        distance: function(p1, p2) {
        	if(p1 === null || p2 === null)
        		return null;
        	
        	return Math.sqrt(Math.pow(p1.lat() - p2.lat(), 2) + Math.pow(p1.lng() - p2.lng(), 2));
        },
        
        updatePosition: function(latlng) {
        	if(lastPosition === null || this.distance(latlng, lastPosition) > .0008)         	
        		lastPosition = marker.getPosition();

        	marker.setPosition(latlng);
        	
        	if(extraMarkerOptions.type === 'vehicle') {
        		var a = this.getHeading();

        		marker.setIcon(new google.maps.MarkerImage("img/vehicle/vehicle-" + a + ".png",
							new google.maps.Size(34, 34),
							new google.maps.Point(0,0),
							new google.maps.Point(17, 17)));
        	}
        },
        
        isDisplayed: function() {
            return marker.getMap() != null;
        },

        getId: function() {
            return entityId;
        }
    };
};

OBA.StopMarker = function(stopId, latlng, map, opts) {
    return OBA.Marker(stopId, latlng, map,
        OBA.StopPopup(stopId, map),
        jQuery.extend(opts, {icon: OBA.Config.stopIcon, zIndex: 100}));
};

OBA.VehicleMarker = function(vehicleId, latlng, map, opts) {
    return OBA.Marker(vehicleId, latlng, map,
        OBA.VehiclePopup(vehicleId, map),
        jQuery.extend(opts, {icon: OBA.Config.vehicleIcon, zIndex: 200, type: 'vehicle'}));
};
