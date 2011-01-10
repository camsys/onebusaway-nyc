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
			    addMapLinkBehavior();
			    addIntersectionAnnotation();
	        });
	    });
	}
	
	function addMapLinkBehavior() {
	    jQuery('table').find('tr').not(":first")
	    .find(".position div a").click(function(e) {
	    	e.preventDefault();
	   		var element = jQuery(this);
	   		var parent = element.parent();
	   		createMap(parent);
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

	function createMap(elRaw) {
		var el = jQuery(elRaw);
		var contents = el.html();
		if(contents === null) {
			return null;
		}
		var location_r = el.find("a").text();
		var lat, lng = null;
		if(location_r !== null) {
			var location = location_r.split(",");			
			lat = location[0];
			lng = location[1];
			if(lat === null || lng === null) {
				return null;
			}
		}
		
		var orientation_r = el.parent().find("img").attr("src").match(/([0-9]+)\.png$/i);
		var orientation = "unknown";		
		if(orientation_r !== null && orientation_r[1] !== "" && orientation_r[1] !== 0) {
			orientation = Math.floor(orientation_r[1] / 5) * 5;
		}
		var mapDivWrapper = jQuery("<div></div>")
							.addClass("map-location-wrapper");
		var iconUrl = "http://dev.oba.openplans.org/" + OBA.Config.vehicleIconFilePrefix + '-' + orientation + '.' + OBA.Config.vehicleIconFileType;
		var image = jQuery("<img></img>")
						.addClass("map-location")
						.appendTo(mapDivWrapper)
						.attr("src", "http://maps.google.com/maps/api/staticmap?size=150x160&markers=shadow:false|icon:" + iconUrl + "|" + lat + "," + lng + "&zoom=15&sensor=false");
		el.append(mapDivWrapper);
	}	
	
	function refreshTable() {
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
               	 	.text("Information current as of " + new Date().format("mmm d, yyyy h:MM:ss TT"));
               	 
               	 addResetLinkBehavior();	
 			     addMapLinkBehavior();
 			     addIntersectionAnnotation();
               	 setTimeout(refreshTable, 30 * 1000);
            }
        });
	}

	function addIntersectionAnnotation() {
		jQuery('table').find('tr').not(":first")
	    .find(".position a").each(function(_, el) {
			var location = jQuery(el).text();
			jQuery.ajax({
	            url: "http://dev.oba.openplans.org/debug/tools/osm.php",
	            dataType: "jsonp",
	            data: {
	            	ll: location,
	            	format: "jsonp"
	            },
	            success: function(data) { 
	            	var textDesc = jQuery("<p></p>")
	            					.text(data.block);
	            	jQuery(el).parent().prepend(textDesc);
	            }
	        });
	    });
	}
	
	return {
		initialize: function() {
		    addTableSortBehaviors();
		    
		    addMapLinkBehavior();
		    addResetLinkBehavior();
		    addIntersectionAnnotation();
	        setTimeout(refreshTable, 30 * 1000);			
		}
	};
})();

jQuery(document).ready(function() { OBA.VehiclesAdmin.initialize(); });
