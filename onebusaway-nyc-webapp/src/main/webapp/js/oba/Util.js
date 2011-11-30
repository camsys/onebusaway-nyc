/*
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var OBA = window.OBA || {};

OBA.Util = (function() {
	return {
		log: function(s) {
			if(OBA.Config.debug === true && typeof console !== 'undefined' && typeof console.log !== 'undefined') {
				console.log(s);
			}
		},
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
		displayTime: function(secondsAgo) {
			secondsAgo = Math.floor(secondsAgo);
			if(secondsAgo < 60) {
				return secondsAgo + " second" + ((secondsAgo === 1) ? "" : "s") + " ago";
			} else {
				minutesAgo = Math.floor(secondsAgo / 60);
				secondsAgo = secondsAgo - (minutesAgo * 60);
				
				var s = minutesAgo + " minute" + ((minutesAgo === 1) ? "" : "s");
				if(secondsAgo > 0) {
					s += ", " + secondsAgo + " second" + ((secondsAgo === 1) ? "" : "s");
				}
				s += " ago";
				return s;
			}
		},
		// quick fix to correct GMT timestamps to be 2011-11-29T13:37:07-05:00
		// Assumes receipt of 2011-11-29T13:37:07.342-0500 
		cleanUpGMT: function(prevTimeString) {
			var timeString = prevTimeString.replace(/\.[0-9]+/i, "");		
			var semicolonIndex = timeString.lastIndexOf('-');
			if (semicolonIndex < 9) {
				semicolonIndex = timeString.lastIndexOf('+');
			}
			if (semicolonIndex > 9) {
				timeString = timeString.substring(0, semicolonIndex+3) + ':' 
					+ timeString.substr(semicolonIndex+3);
			}
			return timeString;
		}
	};
})();
