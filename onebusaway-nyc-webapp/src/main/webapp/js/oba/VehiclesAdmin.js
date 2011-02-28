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

OBA.VehiclesAdmin = (function() {
	var lastSortIndex = -1;
	var reverseSort = false;

	function sortTableRows(rows, sortIndex, reverse) {
		var keyFn = function(row) {
            var row = jQuery(row).children().slice(sortIndex, sortIndex+1);
            if(typeof row.attr("sortKey") !== 'undefined') {
            	return row.attr("sortKey");
            } else {
            	return row.text();
            }            
            return key;
        };
        rows = jQuery.makeArray(rows);
        rows.sort(function(a, b) {
            var x = keyFn(a);
            var y = keyFn(b);
            if (x < y) { return -1; }
            else if (x > y) { return 1; }
            else { return 0; }
        });
        if(reverse === true) {
            rows.reverse();
        }
        return rows;
	}

	function addTableSortBehaviors() {
	    var table = jQuery('table');
	    table.find('th a').each(function(i, a) {
	        jQuery(a).click(function(e) {
	            e.preventDefault();
	            if(lastSortIndex === i) {
	            	reverseSort = !reverseSort;
	            }
	            var oldRows = table.find('tr').not(":first");
	            var newRows = sortTableRows(oldRows, i, reverseSort);	      
	            lastSortIndex = i;
	            oldRows.remove();
	            table.append(newRows);
	            
	            addResetLinkBehavior();
	        });
	    });
	}

	function addResetLinkBehavior() {
	    jQuery('table').find('tr').not(":first")
	    .find("a.reset").click(function(e) {
	    	e.preventDefault();
	   		var element = jQuery(this);
	   		jQuery.ajax({
	   			url: element.attr("href")
	   		});
	   		var row = element
    					.parent().parent()
    					.parent().parent()
    					.fadeOut("fast");
    		return false;
	    });
	}

	function createMaps() {		
		jQuery(".position").each(function(_, el) {
			el = jQuery(el);
			
			var contents = el.html();
			if(contents === null) {
				return null;
			}
			
			var data_r = el.text();
			var data = data_r.split(",");			
			
			var lat, lng = null;
			try {
				lat = parseFloat(data[0]);
				lng = parseFloat(data[1]);

				if(isNaN(lat) || isNaN(lng)) {
					return;
				}
			} catch(e) {
				return;
			}

			var lng = data[1];
			var orientation = null;
			try {
				orientation = Math.floor(data[2] / 5) * 5;
				
				if(isNaN(orientation)) {
					orientation = "unknown";					
				}
			} catch(e) {
				orientation = "unknown";
			}
			
			if(jQuery("#showMap").attr("checked") !== true) {
				jQuery.ajax({
		            url: "http://dev.oba.openplans.org/debug/tools/osm.php",
		            dataType: "jsonp",
		            data: {
		            	ll: lat + "," + lng,
		            	format: "jsonp"
		            },
		            success: function(data) { 
		            	var locationText = jQuery("<p></p>")
		            					.text(data.block);

						el.find("p").hide();
		            	el.append(locationText);
		            }
		        });
			} else {
				var mapDivWrapper = jQuery("<div></div>")
									.addClass("map-location-wrapper");
	
				var mapContainer = jQuery("<div></div>")
									.addClass("map-location")
									.appendTo(mapDivWrapper);
	
				var marker = jQuery("<img></img>")
									.addClass("marker")
									.appendTo(mapDivWrapper)
									.attr("src", "http://dev.oba.openplans.org/" + OBA.Config.vehicleIconFilePrefix + '-' + orientation + '.' + OBA.Config.vehicleIconFileType);
	
				el.find("p").hide();
				el.append(mapDivWrapper);	
	
				var options = {
						zoom: 15,
						mapTypeControl: false,
						streetViewControl: false,		
						center: new google.maps.LatLng(lat, lng),
						mapTypeId: google.maps.MapTypeId.ROADMAP,
						disableDefaultUI: true,
						draggable: false,
						zoomControl: false,
						scrollwheel: false
				};
				new google.maps.Map(mapContainer.get(0), options);
			}
		});
	}	
	
	function refreshTable() {
		if(jQuery("#refresh").attr("checked") !== true) {
			setTimeout(refreshTable, 30 * 1000);
			return;
		}
		
		jQuery.ajax({
            url: window.location.href,
            success: function(data) { 
                 var table = jQuery("table");

                 var oldTableRows = table.find('tr').not(":first");
               	 var newTableRows = sortTableRows(
               			 	jQuery(data).find("table").find('tr').not(":first"),
               			 	lastSortIndex,
               			 	reverseSort);
 	 
               	 oldTableRows.remove();
               	 table.append(newTableRows);
               	 
               	 jQuery("#timestamp")
               	 	.text("Information current as of " + new Date(OBA.Util.getTime()).format("mmm d, yyyy h:MM:ss TT"));
               	 
     		     createMaps();               	 
               	 addResetLinkBehavior();	
               	 setTimeout(refreshTable, 30 * 1000);
            }
        });
	}

	return {
		initialize: function() {
		    addTableSortBehaviors();

		    createMaps();
		    addResetLinkBehavior();
	        setTimeout(refreshTable, 30 * 1000);			
		}
	};
})();

jQuery(document).ready(function() { OBA.VehiclesAdmin.initialize(); });
