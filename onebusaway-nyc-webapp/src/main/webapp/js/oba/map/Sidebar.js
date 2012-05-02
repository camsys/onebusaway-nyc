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
	var theWindow = jQuery(window),
		topBarDiv = jQuery("#topbar"), 
		mainbox = jQuery("#mainbox"),
		menuBar = jQuery("#cssmenu1"),
		mapDiv = jQuery("#map");

	var searchBarDiv = jQuery("#searchbar"), 
		matches = jQuery("#matches"),
		suggestions = jQuery("#suggestions"),
		noResults = jQuery("#no-results"),
		welcome = jQuery("#welcome"),
		loading = jQuery("#loading");

	var routeMap = null;
	var wizard = null;

	var searchRequest = null;
	
	function addSearchBehavior() {
		var searchForm = jQuery("#searchbar form");
		
		searchForm.submit(function(e) {
			e.preventDefault();
			
			var searchInput = jQuery("#searchbar form input[type=text]");

			// if search hasn't changed, force the search again to make panning, etc. happen
			if(window.location.hash !== "#" + searchInput.val()) {
				jQuery.history.load(searchInput.val());	
			} else {
				doSearch(searchInput.val());
			}
			
			(wizard && wizard.enabled()) ? results.trigger('search_launched') : null;
		});
		
		// add autocomplete behavior
		jQuery("#bustimesearch").autocomplete({
			source: OBA.Config.autocompleteUrl,
			select: function(event, ui) {
		        if(ui.item){
		        	jQuery('#bustimesearch').val(ui.item.value);
		        }
		        jQuery('#bustime_search_form').submit();
		    }
		});
		jQuery('#bustime_search_form').submit(function() {
			jQuery("#bustimesearch").autocomplete("close");
			return true;
		});
		
		jQuery("#bustimesearch").autocomplete("close").submit
		jQuery("#bustimesearch").autocomplete("close")
	}

	function addResizeBehavior() {
		var resize = function() {		
			var h = theWindow.height() - topBarDiv.height(),
				w = theWindow.width();

			searchBarDiv.height(h);
			mapDiv.height(h);
						
			if (w <= 1060) {
				mainbox.css("width", "960px");
			} else {
				mainbox.css("width", w - 150); // 75px margin on each 
											   // side for dropdown menus
			}

			// size set so we can have MTA menu items calculate their widths properly
			menuBar.width(mainbox.width());
		};
		resize();

		// call when the window is resized
		theWindow.resize(resize);
	}

	// show user list of addresses
	function disambiguateLocations(locations) {	
		suggestions.find("h2")
			.text("Did you mean?");

		var resultsList = suggestions.find("ul");

		var bounds = null;
		jQuery.each(locations, function(i, location) {
			var latlng = new google.maps.LatLng(location.latitude, location.longitude);
			var address = location.formattedAddress;
			var neighborhood = location.neighborhood;
			
		    // sidebar item
			var link = jQuery("<a href='#" + address + "'></a>")
							.text(address);

			var listItem = jQuery("<li></li>")
							.addClass("locationItem")
							.css("background-image", "url('img/location/location_" + (i + 1) + ".png')")
							.append(link);

			resultsList.append(listItem);

			// marker
			var marker = routeMap.addDisambiguationMarker(latlng, address, neighborhood, (i + 1));			

			listItem.hover(function() {
				routeMap.highlightDisambiguationMarker(marker, (i + 1));
				listItem.css("background-image", "url('img/location/location_active_sidebar_" + (i + 1) + ".png')");
			}, function() {
				routeMap.unhighlightDisambiguationMarker(marker, (i + 1));
				listItem.css("background-image", "url('img/location/location_" + (i + 1) + ".png')");
			});

			// calculate extent of all options
			if(bounds === null) {
				bounds = new google.maps.LatLngBounds(latlng, latlng);
			} else {
				bounds.extend(latlng);
			}
		});
		
		routeMap.showBounds(bounds);
		
		suggestions.show();
	}
		
	function loadStopsForRouteAndDirection(routeResult, direction, destinationContainer) {
		var stopsList = destinationContainer.find("ul");
		
		// if stops are already loaded, don't request them again
		if(! stopsList.hasClass("not-loaded")) {
			return;
		}

		var loading = destinationContainer.find(".loading");
		loading.show();

		// multiple of these can be out at once without being inconsistent UI-wise.
		jQuery.getJSON(OBA.Config.stopsOnRouteForDirection + "?callback=?", { routeId: routeResult.id, directionId: direction.directionId }, 
		function(json) { 
			loading.hide();

			stopsList.removeClass("not-loaded");

			jQuery.each(json.stops, function(_, stop) {
				routeMap.addStop(stop, null);
				
				var stopLink = jQuery("<a href='#'></a>")
									.text(stop.name);
					
				var imagePiece = "middle";
				if(_ === 0) {
					imagePiece = "start";
				} else if(_ === json.stops.length - 1) {
					imagePiece = "end";
				}
				
				var stopItem = jQuery('<li></li>')
								.css("background-image", "url(img/stop-on-route/stop_on_route_" + imagePiece + "_" + routeResult.color + ".png)")
								.append(stopLink);

				stopsList.append(stopItem);
				
				stopLink.click(function(e) {
					e.preventDefault();
					
					routeMap.showPopupForStopId(stop.id);
				});
				
				stopLink.hover(function() {
					routeMap.highlightStop(stop);
				}, function() {
					routeMap.unhighlightStop();					
				});
			});
			
		});
	}
	
	function addRoutesToLegend(routeResults, title) {
		if(typeof title !== "undefined" && title !== null) {
			matches.find("h2").text(title);
		}

		var resultsList = matches.find("ul");

		jQuery.each(routeResults, function(_, routeResult) {				
			// service alerts
			var serviceAlertList = jQuery("<ul></ul>")
							.addClass("alerts");
			
			var serviceAlertHeader = jQuery("<p class='serviceAlert'>Service Change for Route</p>")
											.append(jQuery("<span class='click_info'> + Click for info</span>"));
			
			var serviceAlertContainer = jQuery("<div></div>")
											.attr("id", "alerts-" + routeResult.id.replace(" ", "_"))
											.addClass("serviceAlertContainer")
											.append(serviceAlertHeader)
											.append(serviceAlertList);
			
			serviceAlertContainer.accordion({ header: 'p.serviceAlert', 
				collapsible: true, 
				active: false, 
				autoHeight: false });

			// sidebar item
			var titleBox = jQuery("<p></p>")
							.addClass("name")
							.text(routeResult.shortName + " " + routeResult.longName)
							.css("border-bottom", "5px solid #" + routeResult.color);
			
			var descriptionBox = jQuery("<p></p>")
							.addClass("description")
							.text(routeResult.description);

			var listItem = jQuery("<li></li>")
							.addClass("legendItem")
							.append(titleBox)
							.append(descriptionBox)
							.append(serviceAlertContainer);
	
			resultsList.append(listItem);
			
			// on click of title, pan to route extent 
			titleBox.click(function(e) {
				e.preventDefault();
				
				routeMap.panToRoute(routeResult.id);
			});

			// hover polylines
			titleBox.hover(function(e) {
				titleBox.css("color", "#" + routeResult.color);
			}, function(e) {
				titleBox.css("color", "");
			});

			titleBox.hoverIntent({
				over: function(e) { routeMap.highlightRoute(routeResult.id); },
				out: function(e) { routeMap.unhighlightRoute(routeResult.id); },
				sensitivity: 10
			});

			// direction picker
			jQuery.each(routeResult.directions, function(_, direction) {
				var directionHeader = jQuery("<p></p>");
				
				jQuery("<span></span>")
					.text("to " + direction.destination)
					.appendTo(directionHeader);
				
				if(direction.hasUpcomingScheduledService === false) {
					var noServiceMessage = jQuery("<div></div>")
												.addClass("no-service")
												.text("No scheduled service for the " + 
														routeResult.shortName + 
														" to " + direction.destination + " at this time.");

					directionHeader.append(noServiceMessage);
				}

				var stopsList = jQuery("<ul></ul>")
											.addClass("stops")
											.addClass("not-loaded");

				var loading = jQuery("<div><span>Loading...</span></div>")
											.addClass("loading");

				var destinationContainer = jQuery("<p></p>")
											.addClass("destination")
											.append(directionHeader)
											.append(stopsList)
											.append(loading);
				
				// load stops when user expands stop list
				directionHeader.click(function(e) {
					loadStopsForRouteAndDirection(routeResult, direction, destinationContainer);
				});
				
				// accordion-ize
				destinationContainer.accordion({ header: 'p', 
					collapsible: true, 
					active: false, 
					autoHeight: false });
				
				listItem.append(destinationContainer);
			});
			
			// add to map
			routeMap.addRoute(routeResult);
		});

		matches.show();
	}

	// show multiple route choices to user
	function showRoutePickerList(routeResults) {	
		suggestions.find("h2").text("Did you mean?");

		var resultsList = suggestions.find("ul");

		jQuery.each(routeResults, function(_, route) {
			var link = jQuery('<a href="#' + route.shortName + '"></a>')
							.text(route.shortName)
							.attr("title", route.description);

			var listItem = jQuery("<li></li>")
							.addClass("routeItem")
							.append(link);
			
			resultsList.append(listItem);

			// polyline hovers
			var allPolylines = [];

			// "region" routes (searches for a zip code, etc.)
			if(typeof route.polylines !== "undefined") {
				jQuery.each(route.polylines, function(__, polyline) {
					allPolylines.push(polyline);
				});
			
			// "did you mean" route suggestions--ex. X17 suggests X17J,A,C
			} else if(route.directions !== "undefined") {
				jQuery.each(route.directions, function(__, direction) {
					jQuery.each(direction.polylines, function(___, polyline) {
						allPolylines.push(polyline);						
					});
				});
			}

			if(allPolylines.length > 0) {
				link.hover(function() {
					routeMap.showHoverPolyline(allPolylines, route.color);
				}, function() {
					routeMap.removeHoverPolyline();
				});
			}
		});
		
		suggestions.show();
	}
	
	function resetSearchPanelAndMap() {
		welcome.hide();
		noResults.hide();

		matches.hide();		
		matches.children().empty();

		suggestions.hide();		
		suggestions.children().empty();

		routeMap.reset();
	}
	
	function showNoResults(message) {
		if (typeof message !== "undefined") { 
			noResults.html("<h2>" + message + "</h2>"); 
			noResults.show();
		}

		welcome.show();

		(wizard && wizard.enabled()) ? results.trigger('no_result') : null;
	}

	// process search results
	function doSearch(q) {
		resetSearchPanelAndMap();		

		(wizard && wizard.enabled()) ? results.trigger('search_launched') : null;
		
		loading.show();	
		
		if(searchRequest !== null) {
			searchRequest.abort();
		}		
		searchRequest = jQuery.getJSON(OBA.Config.searchUrl + "?callback=?", { q: q }, function(json) { 
			loading.hide();

			var resultType = json.searchResults.resultType;
			var empty = json.searchResults.empty;
			
			var matches = json.searchResults.matches;
			var suggestions = json.searchResults.suggestions;

			OBA.Config.analyticsFunction("Search", q + " [M:" + matches.length + " S:" + suggestions.length + "]");
			
			if(empty === true) {
				showNoResults("No matches.");
				return;
			} else {
				noResults.hide();
			}

			// direct matches
			if(matches.length === 1) {
				switch(resultType) {
					case "GeocodeResult":
						// result is a region
						if(matches[0].isRegion === true) {
							if(matches[0].nearbyRoutes.length === 0) {
								showNoResults("No stops nearby.");
							} else {
								showRoutePickerList(matches[0].nearbyRoutes);								
							}

							var latLngBounds = new google.maps.LatLngBounds(
									new google.maps.LatLng(matches[0].bounds.minLat, matches[0].bounds.minLon), 
									new google.maps.LatLng(matches[0].bounds.maxLat, matches[0].bounds.maxLon));

							routeMap.showBounds(latLngBounds);
							
						// result is a point--intersection or address
						} else {
							if(matches[0].nearbyRoutes.length === 0) {
								showNoResults("No stops nearby.");
							} else {
								addRoutesToLegend(matches[0].nearbyRoutes, "Nearby routes:");
							}
							
							var latlng = new google.maps.LatLng(matches[0].latitude, matches[0].longitude);
							
							routeMap.showLocation(latlng);
							routeMap.addLocationMarker(latlng, matches[0].formattedAddress, matches[0].neighborhood);
						}

						(wizard && wizard.enabled()) ? results.trigger('location_result') : null;
						break;
				
					case "RouteResult":
						addRoutesToLegend(matches, "Routes:");

						routeMap.panToRoute(matches[0].id);
						
						(wizard && wizard.enabled()) ? results.trigger('route_result') : null;
						break;
					
					case "StopResult":
						addRoutesToLegend(matches[0].routesAvailable, "Routes available:");

						var latlng = new google.maps.LatLng(matches[0].latitude, matches[0].longitude);
						routeMap.addStop(matches[0], function(marker) {
							routeMap.showPopupForStopId(matches[0].id);							
						});
						
						routeMap.showLocation(latlng);
						
						(wizard && wizard.enabled()) ? results.trigger('stop_result') : null;
						break;
				}				
			} 
			
			// did you mean suggestions
			if(suggestions.length > 0){
				switch(resultType) {
					case "GeocodeResult":					
						disambiguateLocations(suggestions)
						
						(wizard && wizard.enabled()) ? results.trigger('disambiguation_result') : null;
						break;

					// a route query with no direct matches
					case "RouteResult":
						showRoutePickerList(suggestions);								

						(wizard && wizard.enabled()) ? results.trigger('route_result') : null;
						break;
				}
			}
		});
	}
	
	return {
		initialize: function() {
			addSearchBehavior();
			addResizeBehavior();
			
			// initialize map, and continue initialization of things that use the map
			// on load only when google maps says it's ready.
			routeMap = OBA.RouteMap(document.getElementById("map"), function() {
				// deep link handler
				jQuery.history.init(function(hash) {
					if(hash !== null && hash !== "") {
						var searchInput = jQuery("#searchbar form input[type=text]");
						searchInput.val(hash);
						doSearch(hash);
					} else {
						// Launch wizard
						//(wizard !== null) ? null : wizard = OBA.Wizard(routeMap);
					}
				});
			}, function(routeId, serviceAlerts) { // service alert notification handler
				var serviceAlertsContainer = jQuery("#alerts-" + routeId.replace(" ", "_"));
				if(serviceAlertsContainer.length === 0) {
					return;
				}

				if(serviceAlerts.length === 0) {
					serviceAlertsContainer.hide();
				} else {
					serviceAlertsContainer.show();
					
					var serviceAlertsList = serviceAlertsContainer.find("ul");
					serviceAlertsList.empty();

					jQuery.each(serviceAlerts, function(_, serviceAlert) {
	                    var description = serviceAlert.Description.replace(/\n/g, "<br/>");
						serviceAlertsList.append(jQuery("<li></li>").html(description));
					});
				}
			});
		}
	};
};

// for IE: only start using google maps when the VML/SVG namespace is ready
if(jQuery.browser.msie) {
	window.onload = function() { OBA.Sidebar().initialize(); };
} else {
	jQuery(document).ready(function() { OBA.Sidebar().initialize(); });
}