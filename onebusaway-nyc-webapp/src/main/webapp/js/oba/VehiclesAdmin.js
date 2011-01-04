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
	var lastSortIndex = -1;
	var reverseSort = false;
	
	var markers = {};
		
	function sortTableRows(rows, sortIndex, reverse) { 
        var keyFn = function(row) {
            var key = jQuery(row).children().slice(sortIndex, sortIndex+1).text();
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
	        });
	    });
	}
	
	jQuery(document).ready(function() {
	    addTableSortBehaviors();
	    
	    // refresh every 30s
	    var refreshFunction = function() {
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
	               	 	.text("Information current as of " + new Date().format("mmm d, yyyy h:mm:ss tt"));
	            }
            });
		    
		    setTimeout(refreshFunction, 30 * 1000);
	    };
	    
	    // kick off the updates...
	    setTimeout(refreshFunction, 30 * 1000);
	});
})();
