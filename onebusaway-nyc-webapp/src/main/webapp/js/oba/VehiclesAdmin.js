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
	function addTableSortBehaviors() {
	    var table = jQuery('table');
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
	                if (x < y) { return -1; }
	                else if (x > y) { return 1; }
	                else { return 0; }
	            });
	            
	            if (lastSortIndex === i) {
	                rows.reverse();
	                lastSortIndex = -1;
	            } else {
	                lastSortIndex = i;
	            }
	            
	            table.append(rows);
	        });
	    });
	}
	
	function createMaps() {
		jQuery(".map").each(function(_, elRaw) {
			var el = jQuery(elRaw);

			var contents = el.html();
			if(contents === null) {
				return;
			}
			
			var location_r = contents.match(/Location\: ([0-9|.|\-| |,]*)/i);
			var orientation_r = contents.match(/Orientation\: ([0-9]*)/i);
			if(location_r === null || orientation_r === null 
				|| location_r.length !== 2 || location_r[1] === ""
				|| orientation_r.length !== 2) {
				return;
			}
			
			var location = location_r[1].split(",");			

			if(location === null || location.length !== 2) {
				return;
			}
			
			var lat = location[0];
			var lng = location[1];

			if(lat === null || lng === null) {
				return;
			}
			
			var orientation = Math.ceil(orientation_r[1] / 30) * 30;
	
			if(orientation === null || orientation === "" || orientation === 0) {
				orientation = "unknown";
			}

			var mapDiv = jQuery("<div></div>").addClass("map-location");
			var mapWrapper = jQuery("<div></div>").addClass("map-location-wrapper").append(mapDiv);

			el.html("");
			el.append(mapWrapper);
	
			var latlng = new google.maps.LatLng(lat, lng);
			
			var mapOptions = {
			      zoom: 15,
			      center: latlng,
			      mapTypeControl: false,
			      streetViewControl: false,
				  navigationControlOptions: { style: google.maps.NavigationControlStyle.SMALL },
				  mapTypeId: google.maps.MapTypeId.ROADMAP
			};
				
			var map = new google.maps.Map(mapDiv.get(0), mapOptions);
		
			var icon = new google.maps.MarkerImage(OBA.Config.vehicleIconFilePrefix + '-' + orientation + '.' + OBA.Config.vehicleIconFileType,
					OBA.Config.vehicleIconSize,
					new google.maps.Point(0,0),
					OBA.Config.vehicleIconCenter);
	    	
			var marker = new google.maps.Marker({
			      position: latlng, 
				  icon: icon,
			      map: map,
			      clickable: false
			});
	
			// (FIXME?) Google maps seems to always take into account marker padding with the assumption of a 
			// marker that is shaped like its default (i.e. taller than wide). Because of this, we have to adjust 
			// the map center to make up for this false assumption in our case.
			map.panBy(0, 18);
		});
	}
	
	jQuery(document).ready(function() {
	    addTableSortBehaviors();
	    createMaps();
	    
	    // refresh every 1m
	    setTimeout(function() {
	    	window.location.href = window.location.href;
	    }, 60 * 1000);
	});
})();
