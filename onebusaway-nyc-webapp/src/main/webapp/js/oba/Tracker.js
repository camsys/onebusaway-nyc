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
    var headerDiv = null;
    var footerDiv = null;
    var contentDiv = null;
	var searchDiv = null;
    var sidebarHeaders = null;
    var displayedRoutesDiv = null;
    var searchResultsDiv = null;

    var mapNode = document.getElementById("map");
    var routeMap = OBA.RouteMap(mapNode);
    var map = routeMap.getMap();

    function addExampleSearchBehavior() {
      var searchForm = jQuery("#search form");
      var searchInput = jQuery("#search input[type=text]");
      var exampleSearches = jQuery("#example-searches");
    
      exampleSearches.find("a").click(function(e) {
        e.preventDefault();
    
        var exampleText = jQuery(this).text();
        searchInput.val(exampleText);
        searchForm.submit();

        exampleSearches.remove();
      });
    }

    function addSearchBehavior() {
      var searchForm = jQuery("#search form");
      var searchAction = searchForm.attr("action");
      var searchInput = jQuery("#search input[type=text]");
      var exampleSearches = jQuery("#example-searches");

      searchForm.submit(function(e) {
        e.preventDefault();

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
      });
    }

    function populateSearchResults(json) {
      if (!json || !json.searchResults)
        return;

      var anyResults = false;
      var searchResultsList = jQuery("#search-results-list");
      searchResultsList.empty();

      jQuery.each(json.searchResults, function(i, record) {
        if (typeof record.stopId !== 'undefined') {
          var stopElement = makeStopElement(record);
          stopElement.data("id", record.stopId);
          searchResultsList.append(jQuery("<li></li>").append(stopElement));

          anyResults = true;
        } else if (typeof record.routeId !== 'undefined' && typeof record.directionId !== 'undefined') {
          var routeElement = makeRouteElement(record);
          routeElement.data("id", record.routeId);
          routeElement.data("directionId", record.directionId);
          searchResultsList.append(jQuery("<li></li>").append(routeElement));

          if (routeMap.containsRoute(record.routeId, record.directionId)) {
              // if we already have the route displayed
              // its control should be disabled
              routeElement.find(".controls a").addClass("disabled");
          }

          anyResults = true;
        }
      });
      
      if (!anyResults) {
        searchResultsList.append(jQuery("<li>There were no matches for your query.</li>"));
	  }
	
      searchResultsList.hide().fadeIn();
    }
          
    function makeStopElement(record) {
      var el = jQuery('<div class="stop result"></div>')
                      .append('<p class="name">' + OBA.Util.truncateToWidth(record.name, 280, 14) + '</p>');

      var controls = jQuery('<ul></ul>').addClass("controls")
                .append('<li><a class="showOnMap" href="#">Show on Map</a></li>');
      
      el.append(controls);

      // display routes available at this stop
      if(typeof record.routesAvailable !== 'undefined') {
          var description = '<ul class="description">';
          
          jQuery.each(record.routesAvailable, function(i, route) {
            description += '<li>' + OBA.Util.truncateToWidth(route.headsign, 275, 11) + '</li>';
          });

          description += '</ul>';
          
          el.append(jQuery(description));
      }

      return el;      
    }

    function makeRouteElement(record) {
      var el = jQuery('<div class="route result' + ((typeof record.serviceNotice !== 'undefined') ? ' hasNotice' : '') + '"></div>')
                .append('<p class="name">' +
                        OBA.Util.truncateToWidth(record.tripHeadsign, 190, 14) + 
                        '</p>')
                .append('<p class="description">' + OBA.Util.truncateToWidth(record.description, 275, 11) + '</p>');
             
      var controls = jQuery('<ul></ul>').addClass("controls")
                .append('<li><a class="addToMap" href="#">Add To Map</a></li>')
                .append('<li><a class="zoomToExtent" href="#">Zoom To Extent</a></li>');

      el.append(controls);
                
      return el;
    }
      
    function addSearchControlBehavior() {
      jQuery("#search-results-list .showOnMap").live("click", handleShowOnMap);
      jQuery("#search-results-list .addToMap").live("click", handleAddToMap);
      jQuery("#displayed-routes-list .zoomToExtent").live("click", handleZoomToExtent);
      jQuery("#displayed-routes-list .removeFromMap").live("click", handleRemoveFromMap);
    }
            
    function handleShowOnMap(e) {
      e.preventDefault();
      var div = jQuery(this).parent().parent().parent("div");
      var stopId = div.data("id");
      routeMap.showStop(stopId);
    }

    function handleZoomToExtent(e) {
        var displayRouteDiv = jQuery(this).parents("div.result");
        var routeId = displayRouteDiv.data("id");
        var directionId = displayRouteDiv.data("directionId");
        var latlngBounds = routeMap.getBounds(routeId, directionId);
        if (latlngBounds)
            map.fitBounds(latlngBounds);
        return false;
    }

    function handleAddToMap(e) {
      var controlLink = jQuery(this);
      var resultDiv = controlLink.parent().parent().parent("div");
      var routeId = resultDiv.data("id");
      var directionId = resultDiv.data("directionId");

      if (routeMap.containsRoute(routeId, directionId)) {
        return false;
      }

      jQuery("#no-routes-displayed-message").hide();

      // clone the search result element to place in the routes displayed list
      var clonedDiv = resultDiv.clone();
      clonedDiv.data("id", resultDiv.data("id"));
      clonedDiv.data("directionId", resultDiv.data("directionId"));

      // update the control link class to alter the event fired
      var clonedControlLink = clonedDiv.find(".addToMap");
      clonedControlLink.removeClass("addToMap");
      clonedControlLink.addClass("removeFromMap");
      clonedControlLink.html("Remove from map");

      jQuery("<li></li>").append(clonedDiv)
        .appendTo(jQuery("#displayed-routes-list"))
        .hide().fadeIn();

      // also update the control link on the search result element to prevent the
      // user from clicking on it twice
      controlLink.addClass("disabled");

      var url = OBA.Config.routeShapeUrl + "/" + routeId + ".json";
      jQuery.getJSON(url, {version: 2, key: OBA.Config.apiKey}, function(json) {
          var shape;
          var stopGroupings = json.data.entry.stopGroupings;
          var directionStopGrouping = null;
          jQuery.each(stopGroupings, function(_, stopGrouping) {
              if (stopGrouping.type === "direction")
                  directionStopGrouping = stopGrouping;
          });
          if (directionStopGrouping === null) {
              OBA.Util.log("Could not find direction stop grouping");
              OBA.Util.log(json);
              return;
          }
          var stopGroups = directionStopGrouping.stopGroups;
          var shape = null;
          for ( var i = 0; i < stopGroups.length; i++) {
              var stopGroup = stopGroups[i];
              if (stopGroup.id === directionId) {
                  shape = stopGroup.polylines;
                  break;
              }
          }
          if (shape === null) {
              OBA.Util.log("Could not find shape for route direction: " + directionId);
              OBA.Util.log(json);
              return;
          }
          
          routeMap.addRoute(routeId, directionId, shape);

          // update text info on screen
          jQuery("#n-displayed-routes").text(routeMap.getCount());
      });

      return false;
    }

    function handleRemoveFromMap(e) {
      var controlLink = jQuery(this);
      var displayRouteDiv = controlLink.parent().parent().parent("div");
      var routeId = displayRouteDiv.data("id");
      var directionId = displayRouteDiv.data("directionId");

      displayRouteDiv.fadeOut("fast", function() { displayRouteDiv.remove(); });

      routeMap.removeRoute(routeId, directionId);

      jQuery("#search-results-list .result").each(function() {
          var eltRouteId = jQuery(this).data("id");
          var eltDirectionId = jQuery(this).data("directionId");
          if (eltRouteId === routeId && eltDirectionId === directionId) {
              jQuery(this).find("a.disabled").removeClass("disabled");
          }
      });

      // update text info on screen
      var nDisplayedRoutes = routeMap.getCount();

      if (nDisplayedRoutes <= 0) {
          jQuery("#no-routes-displayed-message").show();
      }

      jQuery("#n-displayed-routes").text(nDisplayedRoutes);

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
		searchResultsDiv = jQuery("#search-results")
	
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
        }
    };
};

jQuery(document).ready(function() { OBA.Tracker().initialize(); });
