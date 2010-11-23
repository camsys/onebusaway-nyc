//Copyright 2010, OpenPlans
//Licensed under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at

//http://www.apache.org/licenses/LICENSE-2.0

//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

var OBA = window.OBA || {};

OBA.Marker = function(entityId, latlng, map, popup, options) {
	var markerOptions = {
			position: new google.maps.LatLng(latlng[0], latlng[1])
	};

	var marker = new google.maps.Marker(jQuery.extend(markerOptions, options || {}));
	var showPopup = function() { popup.show(marker); };

	google.maps.event.addListener(marker, "click", showPopup);

	return {
		showPopup: showPopup,

		getMap: function() {
			return marker.getMap();
		},

		setMap: function(map) {
			// marker cannot be removed from map if it is attached to the current infoWindow.
			if(map === null) {
				if(OBA.theInfoWindowMarker !== null && OBA.theInfoWindowMarker.getPosition() === marker.getPosition()) {
					return;
				}
			}

			marker.setMap(map);
		},

		addMarker: function() {
			marker.setMap(map);
		},

		removeMarker: function() {
			marker.setMap(null);
		},

		updateOrientation: function(_orientation) {
			var orientation = 'undefined';

			if(typeof _orientation !== 'undefined' && _orientation !== 'NaN') {
				orientation = Math.floor(_orientation / 30) * 30;
			}

			var icon = new google.maps.MarkerImage(OBA.Config.vehicleIconFilePrefix + '-' + orientation + '.' + OBA.Config.vehicleIconFileType,
					OBA.Config.vehicleIconSize,
					new google.maps.Point(0,0),
					OBA.Config.vehicleIconCenter);

			marker.setIcon(icon);
		},

		updatePosition: function(latlng) {
			// refresh infoWindow if the infoWindow-bound marker is the same as us
			if(OBA.theInfoWindow !== null) {
				if(OBA.theInfoWindowMarker !== null && OBA.theInfoWindowMarker.getPosition() == marker.getPosition()) {
					popup.refresh();
				}
			}

			marker.setPosition(latlng);
		},

		getPosition: function() {
			return marker.getPosition();
		},

		isDisplayed: function() {
			return marker.getMap() !== null;
		},

		getId: function() {
			return entityId;
		}
	};
};

OBA.StopMarker = function(stopId, latlng, direction, map, opts) {
	opts = jQuery.extend(opts || {}, {zIndex: 100});

	if(typeof opts.icon === 'undefined' && direction !== null) {
		var marker = new google.maps.MarkerImage(OBA.Config.stopIconFilePrefix + '-' + direction + '.' + OBA.Config.stopIconFileType,
				OBA.Config.stopIconSize,
				new google.maps.Point(0,0),
				OBA.Config.stopIconCenter);

		opts.icon = marker;
	}

	return OBA.Marker(stopId, latlng, map,
			OBA.StopPopup(stopId, map),
			opts);
};

OBA.VehicleMarker = function(vehicleId, latlng, map, opts) {
	opts = jQuery.extend(opts || {}, {zIndex: 200});

	if(typeof opts.icon === 'undefined') {
		var marker = new google.maps.MarkerImage(OBA.Config.vehicleIconFilePrefix + '-unknown.' + OBA.Config.vehicleIconFileType,
				OBA.Config.vehicleIconSize,
				new google.maps.Point(0,0),
				OBA.Config.vehicleIconCenter);

		opts.icon = marker;
	}

	return OBA.Marker(vehicleId, latlng, map,
			OBA.VehiclePopup(vehicleId, map),
			opts);
};
