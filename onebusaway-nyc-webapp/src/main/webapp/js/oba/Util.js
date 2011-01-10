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
		displayDistance: function(feetAway, stopsAway, context, tripStatus) {
			var s = "";
			var milesAway = feetAway / 5280;
			if(feetAway <= OBA.Config.atStopThresholdInFeet) {
				s = "at stop";
			} else if(feetAway <= OBA.Config.arrivingThresholdInFeet && stopsAway <= OBA.Config.arrivingThresholdInStops) {
				s = "approaching";
			} else {
				if(stopsAway <= OBA.Config.showDistanceInStopsThresholdInStops) {
					if(stopsAway === 0) {
						s = "< 1 stop away";
					} else {
						s = (stopsAway == 1 ? "1 stop" : stopsAway + " stops") + " away";					
					}
				} else {
					s = (milesAway == 1 ? "1 mile" : milesAway.toPrecision(2) + " miles") + " away";
				}
			}
			
			// if we're formatting a stop bubble, add "at terminal" if vehicle is currently in layover at the end terminal
			// on the previous trip
			if(context === "stop" && tripStatus !== null && 
					(tripStatus.phase.toLowerCase() === 'layover_during' ||
					 tripStatus.phase.toLowerCase() === 'layover_before')) {
			
				var distanceAlongTrip = tripStatus.distanceAlongTrip;
				var totalDistanceAlongTrip = tripStatus.totalDistanceAlongTrip;
				
				if(distanceAlongTrip !== null && totalDistanceAlongTrip !== null) {
					var ratio = distanceAlongTrip / totalDistanceAlongTrip;
					if(ratio > 0.80) {
						s += " (at terminal)";						
					}					
				}
			}
			
			return s;
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
