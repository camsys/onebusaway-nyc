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

OBA.Util = (function() {
    return {
    	/* 
    	 * A *SIMPLE* width calculator for a given string. Calculated with Helvetica/Arial. (FIXME)
    	 */
    	truncateToWidth: function(text, width, size) {
    		var lwide = "abcdeghkmnopqsuvwxyz234567890";
    		var lmedium = "ftr-1";
			var lskinny = "ilj. ";

			var uwide = "abcftrdjleghkmnopqsuvwxyz234567890";
    		var umedium = "";
			var uskinny = "1i-. ";

			if(typeof text === 'string') {    			
    			var w = 0;
    			for(var i = 0; i < text.length; i++) {
    				var uppercase = ((text.charCodeAt(i) > 64 && text.charCodeAt(i) < 91) ? true : false);
    				var c = text.toLowerCase().charAt(i);

    				if(uppercase) {
    					if(uwide.indexOf(c) >= 0)
    						w += size - 5;
    					else if(umedium.indexOf(c) >= 0)
    						w += Math.floor(size / 2);
    					else if(uskinny.indexOf(c) >= 0)
    						w += Math.floor(size / 2);    					
    				} else {
    					if(lwide.indexOf(c) >= 0)
    						w += size - 4;
    					else if(lmedium.indexOf(c) >= 0)
    						w += Math.floor(size / 2) - 1;
    					else if(lskinny.indexOf(c) >= 0)
    						w += Math.floor(size / 3) - 2;
    				}
    				
    				if(w > width)
    					return text.substring(0, i - 2) + "...";
    			}
       		}
    		
            return text;
        },
    	truncate: function(text, length) {
            // FIXME: truncate on word boundaries?
            if(typeof text === 'string' && text.length > length) {
                text = text.substr(0, length - 3) + "...";
            }

            return text;
        },
        serializeArray: function(lst, keyname) {
        	var result = null;

            jQuery.each(lst, function(i, x) {
            	if (!result) {
            		result = keyname + "=" + x;
            	} else {
            		result += "&" + keyname + "=" + x;
            	}
            });

            return result;
        },

        /** debug message to console */
        log: function(s) {
            if (console && console.log) {
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

            if (idx === -1)
                return entityId;

            return entityId.substring(idx + 1);
        },
        metersToFeet: function(meters) {
            var feetInMeters = 3.28083989501312;
            return meters * feetInMeters;
        },
        displayDistance: function(feet) {
            if (feet > 5280) {
                var miles = feet / 5280;
                return miles == 1 ? "1 mile" : miles.toPrecision(3) + " miles";
            } else {
                return feet == 1 ? "1 foot" : Math.round(feet) + " feet";
            }
        },
        displayTime: function(dateObj) {
            var minutes = dateObj.getMinutes();
            minutes = (minutes < 10) ? "0" + minutes : "" + minutes;

            var ampm = "";
            var hours = dateObj.getHours();
            
            if(hours <= 12) {
            	if(hours == 0) {
            		hours = 12;
            	}

            	ampm = "am";
            } else {
            	hours = hours - 12;            	
            	
            	ampm = "pm";
            }
            
            return hours + ":" + minutes + " " + ampm;
        }
    };
})();
