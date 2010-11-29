//Copyright 2010, OpenPlans
//Licensed under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at

//http://www.apache.org/licenses/LICENSE-2.0

//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

var OBA = window.OBA || {};

OBA.Tracker = function() {
	// elements for the resize handler
	var theWindow = null;
	var headerDiv, footerDiv, contentDiv, homeAlertDiv = null;

	var mapNode = document.getElementById("map");
	var routeMap = OBA.RouteMap(mapNode);
	var map = routeMap.getMap();

	function addExampleSearchBehavior() {
		var noResults = jQuery("#no-results");
		var searchForm = jQuery("#search form");
		var searchInput = jQuery("#search input[type=text]");
		
		jQuery.each(noResults.find("a"), function(_, element) {
			var link = jQuery(element);

			link.click(function(e) {
				searchInput.val(link.text());
				searchInput.removeClass("inactive");				
				searchForm.submit();

				return false;
			});
		});
	}
	
	function addSearchBehavior() {
		var searchForm = jQuery("#search form");
		var searchInput = jQuery("#search input[type=text]");

		var loseFocus = function() {
			if(searchInput.val() === "" || searchInput.val() === "Enter an intersection, bus route, or bus stop number.") {
				searchInput.val("Enter an intersection, bus route, or bus stop number.");
				searchInput.addClass("inactive");
			}
		};
		
		loseFocus();
		searchInput.blur(loseFocus);
		
		searchInput.focus(function() {
			if(searchInput.val() === "Enter an intersection, bus route, or bus stop number.") {
				searchInput.val("");
			}
			
			searchInput.removeClass("inactive");
		});

		searchForm.submit(function(e) {
			doSearch(searchInput.val());

			return false;
		});
	}

	function addResizeBehavior() {
		theWindow = jQuery(window);
		headerDiv = jQuery("#header");
		footerDiv = jQuery("#footer");
		contentDiv = jQuery("#content");
		homeAlertDiv = jQuery("#home_alert");
		
		function resize() {
			var h = theWindow.height() - footerDiv.height() - headerDiv.height() - homeAlertDiv.height();

			contentDiv.height(h);
		}

		// call when the window is resized
		theWindow.resize(resize);

		// call upon initial load
		resize();

		// now that we're resizing, we can hide any body overflow/scrollbars
		jQuery("body").css("overflow", "hidden");
	}

	function doSearch(q) {	
		jQuery.getJSON(OBA.Config.searchUrl, {q: q}, function(json) { 
			var noResults = jQuery("#no-results");
			
			if(json.searchResults.length === 0) {
				noResults.fadeIn();
				
			} else {
				noResults.hide();
				
				var displayType = json.searchResults[0].type;
				
				if(displayType === 'route') {
					jQuery.each(json.searchResults, function(_, record) {
						var successFn = function(routeId, directionId) {
							var latlngBounds = routeMap.getBounds(routeId, directionId);

							if (latlngBounds) {
								map.fitBounds(latlngBounds);
							}						
						};
					
						routeMap.addRoute(record.routeId, record.directionId, 
								successFn(record.routeId, record.directionId));				
					});
				} else {
					if(json.searchResults.length > 1) {
						var mapBounds = null;
						jQuery.each(json.searchResults, function(_, record) {
							var latlng = new google.maps.LatLng(record.latlng[0], record.latlng[1]);
							
							if(mapBounds === null) {
								mapBounds = new google.maps.LatLngBounds(latlng, latlng);
							} else {
								mapBounds.extend(latlng);
							}
						});

						map.fitBounds(mapBounds);				
					} else {
						routeMap.showStop(json.searchResults[0].stopId);					
					}					
				}
			}
		});   				
	}

	return {
		initialize: function() {
			addSearchBehavior();
			addExampleSearchBehavior();
			addResizeBehavior();
			
			doSearch("B63");
		}
	};
};

jQuery(document).ready(function() { OBA.Tracker().initialize(); });
