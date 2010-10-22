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

OBA.Tracker = function() {
    // elements for the resize handler
    var theWindow = null;
    var headerDiv, footerDiv, contentDiv = null;
	var searchDiv, searchResultsDiv = null;
    var sidebarHeaders = null;
    var displayedRoutesDiv = null;

    var mapNode = document.getElementById("map");
    var routeMap = OBA.RouteMap(mapNode);
    var map = routeMap.getMap();
    var me = this;

    function addExampleSearchBehavior() {
      var searchForm = jQuery("#search form");
      var searchInput = jQuery("#search input[type=text]");
      var exampleSearches = jQuery("#example-searches");
    
      exampleSearches.find("a").click(function(e) {
        var exampleText = jQuery(this).text();
        searchInput.val(exampleText);
        searchForm.submit();

        exampleSearches.remove();
        
        return false;
      });
    }

    function addSearchBehavior() {
      var searchForm = jQuery("#search form");
      var searchAction = searchForm.attr("action");
      var searchInput = jQuery("#search input[type=text]");
      var exampleSearches = jQuery("#example-searches");

      searchForm.submit(function(e) {
        var formData = jQuery(this).serialize();
        var searchResults = jQuery("#search-results");
        var searchResultsList = jQuery("#search-results-list");

        jQuery.ajax({
            beforeSend: function(xhr) {
                searchResultsList.empty();                
                searchResults.addClass("loading");                
            },
            complete: function(xhr, s) {
                searchResults.removeClass("loading");
            },
            success: function(data, s, xhr) {
                exampleSearches.remove();
                populateSearchResults(data);
            },
            dataType: "json",
            data: formData,
            url: searchAction
        });
        
        return false;
      });
    }

    // adds all routes matching the search result to the map
    function addAllRoutesForSearchQuery(q) {
	    jQuery.getJSON(OBA.Config.searchUrl, {q: q}, function(json) { 
	        var displayedRoutesList = jQuery("#displayed-routes-list");

	    	jQuery.each(json.searchResults, function(_, record) {
	    		if(record.type === 'route') {
	    			jQuery("#no-routes-displayed-message").hide();

	    			var routeElement = makeRouteElement(record);
	    			routeElement.data("id", record.routeId);
	    			routeElement.data("directionId", record.directionId);

	    			// convert to a "displayed routes" div, and add route to map. 
	    			convertSearchResultDivToDisplayedRoutesDiv(routeElement);
	    		}
	    	});
	    });   
    }
    
    function populateSearchResults(json) {
      if (!json || !json.searchResults)
        return;

      var anyResults = false;
      var searchResultsList = jQuery("#search-results-list");
      searchResultsList.empty();

      jQuery.each(json.searchResults, function(i, record) {
        if(typeof record.stopId !== 'undefined') {
          var stopElement = makeStopElement(record);
          stopElement.data("id", record.stopId);
          searchResultsList.append(jQuery("<li></li>").append(stopElement));

          anyResults = true;
        } else if (typeof record.routeId !== 'undefined' && typeof record.directionId !== 'undefined') {
          var routeElement = makeRouteElement(record);
          routeElement.data("id", record.routeId);
          routeElement.data("directionId", record.directionId);
          searchResultsList.append(jQuery("<li></li>").append(routeElement));

          anyResults = true;
        }
      });
      
      if (!anyResults) {
        searchResultsList.append(jQuery("<li>There were no matches for your query.</li>"));
	  }
	
      searchResultsList.hide().fadeIn();
    }
          
    function makeStopElement(record) {
      var element = jQuery('<div class="stop result"></div>')
                      .append('<p class="name">' + OBA.Util.truncateToWidth(record.name, 280, 14) + '</p>');

      var controls = jQuery('<ul class="controls"></ul>')
                		.append('<li><a class="showOnMap" href="#">Show on Map</a></li>');
      
      element.append(controls);

      // display routes available at this stop
      if(typeof record.routesAvailable !== 'undefined' && record.routesAvailable.length > 0) {
          var description = '<ul class="description">';
          
          jQuery.each(record.routesAvailable, function(i, route) {
            description += '<li>' + OBA.Util.truncateToWidth(route.headsign, 275, 11) + '</li>';
          });

          description += '</ul>';
          
          element.append(jQuery(description));
      } else {
    	  element.append(jQuery('<ul class="description">No service is available at this stop.</ul>'));
      }

      return element;      
    }

    function makeRouteElement(record, exists) {
      var element = jQuery('<div class="route result' + ((typeof record.serviceNotice !== 'undefined') ? ' hasNotice' : '') + '"></div>')
                .append('<p class="name">' + OBA.Util.truncateToWidth(record.tripHeadsign, 190, 14) + '</p>')
                .append('<p class="description">' + OBA.Util.truncateToWidth(record.description, 275, 11) + '</p>');
             
      var controls = jQuery('<ul class="controls"></ul>')
                .append('<li><a class="addToMap" href="#">Add To Map</a></li>')
                .append('<li><a class="zoomToExtent" href="#">Show Entire Route</a></li>');

      // if we already have the route displayed
      // its control should be disabled
      if (routeMap.containsRoute(record.routeId, record.directionId)) {
    	  controls.find("a").addClass("disabled");
      }
      
      element.append(controls);
                
      return element;
    }
      
    function addSearchControlBehavior() {
      jQuery("#search-results-list .showOnMap").live("click", handleShowOnMap);
      jQuery("#search-results-list .addToMap").live("click", handleAddToMap);
      jQuery("#displayed-routes-list .zoomToExtent").live("click", handleZoomToExtent);
      jQuery("#displayed-routes-list .removeFromMap").live("click", handleRemoveFromMap);
    }
            
    function handleShowOnMap(e) {
      var resultDiv = jQuery(this).parents("div.result");
      var stopId = resultDiv.data("id");
      
      routeMap.showStop(stopId);
      
      return false;
    }

    function handleZoomToExtent(e) {
        var resultDiv = jQuery(this).parents("div.result");
        var routeId = resultDiv.data("id");
        var directionId = resultDiv.data("directionId");
        
        var latlngBounds = routeMap.getBounds(routeId, directionId);
      
        if (latlngBounds) {
            map.fitBounds(latlngBounds);
        }
        
        return false;
    }

    // takes a (cloned) search result div and adds it to "displayed routes", adding to map. 
    function convertSearchResultDivToDisplayedRoutesDiv(clonedDiv) {
        var routeId = clonedDiv.data("id");
        var directionId = clonedDiv.data("directionId");
        
        // update the control link class to alter the event fired
        var clonedControlLink = clonedDiv.find(".addToMap");
        clonedControlLink.removeClass("addToMap");
        clonedControlLink.addClass("removeFromMap");
        clonedControlLink.html("Remove from map");
        clonedControlLink.removeClass("disabled");

        jQuery("<li></li>").append(clonedDiv)
          .appendTo(jQuery("#displayed-routes-list"))
          .hide().fadeIn();

        routeMap.addRoute(routeId, directionId, function(routeId, directionId) {
            jQuery("#n-displayed-routes").text(routeMap.getCount()); 
        });
    }
    
    // adds search result to "displayed routes" and adds to map. 
    function handleAddToMap(e) {
      var controlLink = jQuery(this);
      var resultDiv = controlLink.parents("div.result");
      var routeId = resultDiv.data("id");
      var directionId = resultDiv.data("directionId");

      if (routeMap.containsRoute(routeId, directionId)) {
        return false;
      }

      // also update the control link on the search result element to prevent the
      // user from clicking on it twice
      controlLink.addClass("disabled");
      
      jQuery("#no-routes-displayed-message").hide();

      // clone the search result element to place in the routes displayed list
      var clonedDiv = resultDiv.clone();
      clonedDiv.data("id", resultDiv.data("id"));
      clonedDiv.data("directionId", resultDiv.data("directionId"));

      // take our cloned div, fix up the links, and add to the "displayed routes" div.
      convertSearchResultDivToDisplayedRoutesDiv(clonedDiv);

      return false;
    }

    // removes search result from "displayed routes" and removes route from map.
    function handleRemoveFromMap(e) {        
      var resultDiv = jQuery(this).parents("div.result");
      var routeId = resultDiv.data("id");
      var directionId = resultDiv.data("directionId");

      // remove from displayed routes
      resultDiv.fadeOut("fast", function() { resultDiv.remove(); });
      routeMap.removeRoute(routeId, directionId);

      // update any search results that are for this route
      jQuery("#search-results-list .result").each(function() {
          var eltRouteId = jQuery(this).data("id");
          var eltDirectionId = jQuery(this).data("directionId");

          if (eltRouteId === routeId && eltDirectionId === directionId) {
              jQuery(this).find("a.disabled").removeClass("disabled");
          }
      });

      // update text info on screen
      var numberOfDisplayedRoutes = routeMap.getCount();
      jQuery("#n-displayed-routes").text(numberOfDisplayedRoutes);

      if (numberOfDisplayedRoutes <= 0) {
          jQuery("#no-routes-displayed-message").show();
      }

      return false;
    }
    
    function addResizeBehavior() {
		theWindow = jQuery(window);
		headerDiv = jQuery("#header");
		footerDiv = jQuery("#footer");
		contentDiv = jQuery("#content");

		searchDiv = jQuery("#search");
		sidebarHeaders = jQuery("#sidebar p.header");
		displayedRoutesDiv = jQuery("#displayed-routes");
		searchResultsDiv = jQuery("#search-results");
	
		function resize() {
			var h = theWindow.height() - footerDiv.height() - headerDiv.height();

			contentDiv.height(h);

			searchResultsDiv.height(Math.ceil(h * .75 - sidebarHeaders.outerHeight() - searchDiv.outerHeight()));
			displayedRoutesDiv.height(Math.floor(h * .25));
		}
	
		// call when the window is resized
		theWindow.resize(resize);

		// call upon initial load
		resize();

		// now that we're resizing, we can hide any body overflow/scrollbars
		jQuery("body").css("overflow", "hidden");
	}

    return {
        initialize: function() {
            addSearchBehavior();
            addSearchControlBehavior();
            addExampleSearchBehavior();
	    	addResizeBehavior();
	    	
	    	addAllRoutesForSearchQuery("B63");
        }
    };
};

jQuery(document).ready(function() { OBA.Tracker().initialize(); });
