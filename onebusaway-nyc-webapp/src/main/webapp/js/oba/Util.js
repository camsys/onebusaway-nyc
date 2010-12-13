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

OBA.Util = (function() {
	return {
		log: function(s) {
			if(OBA.Config.debug === true && typeof console !== 'undefined' && typeof console.log !== 'undefined') {
				console.log(s);
			}
		},
		// This function is from Google's polyline utility.
		decodePolyline: function(encoded) {
			var len = encoded.length;
			var index = 0;
			var array = [];
			var lat = 0;
			var lng = 0;

			while (index < len) {
				var b;
				var shift = 0;
				var result = 0;
				do {
					b = encoded.charCodeAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				var dlat = ((result & 1) ? ~(result >> 1) : (result >> 1));
				lat += dlat;

				shift = 0;
				result = 0;
				do {
					b = encoded.charCodeAt(index++) - 63;
					result |= (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				var dlng = ((result & 1) ? ~(result >> 1) : (result >> 1));
				lng += dlng;

				array.push([lat * 1e-5, lng * 1e-5]);
			}

			return array;
		},
		parseEntityId: function(entityId) {
			var idx = entityId.indexOf("_");
			if (idx === -1) {
				return entityId;
			}
			return entityId.substring(idx + 1);
		},
		metersToFeet: function(meters) {
			var feetInMeters = 3.28083989501312;
			return meters * feetInMeters;
		},
		displayDistance: function(feet, stopsAway) {
			var miles = feet / 5280;
			if(feet <= OBA.Config.arrivingThresholdInFeet && stopsAway === OBA.Config.arrivingThresholdInStops) {
				return "approaching";
			} else {
				return miles == 1 ? "1 mile" : miles.toPrecision(2) + " miles";
			}
		},
		displayTime: function(dateObj) {
			var minutes = dateObj.getMinutes();
			minutes = (minutes < 10) ? "0" + minutes : "" + minutes;
			var hours = dateObj.getHours();            
			var amOrPm = "";
			if(hours >= 12) {
				if(hours > 12) {
					hours = hours - 12;        	            	
				}
				amOrPm = "pm";
			} else {
				if(hours === 0) {
					hours = 12;
				}
				amOrPm = "am";
			}
			return hours + ":" + minutes + " " + amOrPm;
		}
	};
})();
