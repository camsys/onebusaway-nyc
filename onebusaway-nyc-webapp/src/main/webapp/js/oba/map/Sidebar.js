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

OBA.Sidebar = function() {
	// elements for the resize handler
	var theWindow = null;
	var headerDiv, footerDiv, contentDiv = null;

	var mapNode = document.getElementById("map");
	var routeMap = OBA.RouteMap(mapNode);
	
	function addSearchBehavior() {
		var searchForm = jQuery("#search");
		var searchInput = jQuery("#search input[type=text]");
		
		searchForm.submit(function(e) {
			doSearch(searchInput.val());

			OBA.Config.analyticsFunction("Search", searchInput.val());
			return false;
		});
	}

	function addResizeBehavior() {
		theWindow = jQuery(window);
		headerDiv = jQuery("#header");
		footerDiv = jQuery("#footer");
		contentDiv = jQuery("#content");
		searchBarDiv = jQuery("#searchbar");
		
		function resize() {
			var h = theWindow.height() - footerDiv.height() - headerDiv.height();

			contentDiv.height(h);
			searchBarDiv.height(h);
		}

		// call when the window is resized
		theWindow.resize(resize);

		// call upon initial load
		resize();

		// now that we're resizing, we can hide any body overflow/scrollbars
		jQuery("body").css("overflow", "hidden");
	}

	function disambiguate(locationResults) {

	}
	
	function showSingleRoute(routeResult) {
		
	}

	function showRoutesOnMap(routeResults) {
		
	}

	function showRoutePickerList(routeResults) {
		
	}

	function doSearch(q) {
		jQuery.getJSON(OBA.Config.searchUrl, {q: q, bounds: routeMap.getBounds().toUrlValue() }, function(json) { 
			var resultCount = json.searchResults.length;
			if(resultCount === 0)
				return;
			
			var resultType = json.searchResults[0].type;
			
			if(resultType === "location" || resultType === "stop") {
				if(resultCount > 1) {
					disambiguate(json.searchResults);
				} else {
					var result = json.searchResults[0];
					routeMap.showLocation(result.latitude, result.longitude);					
				}
			} else if(resultType === "route") {
				if(resultCount > 1) {
					if(resultCount > 5) {
						showRoutePickerList(json.searchResults);
					} else {
						showRoutesOnMap(json.searchResults);
					}
				} else {
					var result = json.searchResults[0];
					showSingleRoute(result);
				}
			}
		});		
	}
	
	return {
		initialize: function() {
			addSearchBehavior();
			addResizeBehavior();
		}
	};
};

jQuery(document).ready(function() { OBA.Sidebar().initialize(); });
