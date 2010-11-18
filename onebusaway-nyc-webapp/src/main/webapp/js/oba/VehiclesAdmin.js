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
    var table = jQuery('#vehicle-table');
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
        });
    });
}

function createMaps() {
	jQuery(".map-location").each(function(_, elRaw) {
		var el = jQuery(elRaw);
		
		var location = el.html();
		var location_v = location.split(",");

		if(location_v.length != 2)
			return;
		
		var lat = location_v[0];
		var lng = location_v[1];
		var latlng = new google.maps.LatLng(lat, lng);

		var mapOptions = {
		      zoom: 15,
		      center: latlng,
		      mapTypeControl: false,
		      disableDefaultUI: true,
		      mapTypeId: google.maps.MapTypeId.ROADMAP
		};
			
		var map = new google.maps.Map(elRaw, mapOptions);
		var marker = new google.maps.Marker({
		      position: latlng, 
		      map: map
		});
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
