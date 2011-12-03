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

OBA.Sidebar = function () {
	var theWindow, headerDiv, contentDiv = null, searchBarDiv, mainbox,
		routeMap = OBA.RouteMap(document.getElementById("map")),
		welcome = jQuery("#welcome"),
		legend = jQuery("#legend"),
		results = jQuery("#results"),
		noResults = jQuery("#no-results"),
		loading = jQuery("#loading");
	
	function addSearchBehavior() {
		var searchForm = jQuery("#searchbar form"),
			searchInput = jQuery("#searchbar form input[type=text]");
		
		searchForm.submit(function(e) {
			e.preventDefault();
			
			jQuery.history.load(searchInput.val());
		});
	}

	function addResizeBehavior() {
		theWindow = jQuery(window);
		headerDiv = jQuery("#header");
		contentDiv = jQuery("#content");
		searchBarDiv = jQuery("#searchbar");
		mainbox = jQuery("#mainbox");
		
		function resize() {		
			var pageHeightAndWidth = OBA.Util.getPageHeightAndWidth();
			var h = pageHeightAndWidth[0] - headerDiv.height(),
				w = pageHeightAndWidth[1];
			contentDiv.height(h);
			searchBarDiv.height(h);
			if (w <= 1060) {
				mainbox.css("width", "960px");
			} else {
				mainbox.css("width", "95%");
			}
		}
		resize();

		// call when the window is resized
		theWindow.resize(resize);
	}

	// show user list of addresses
	function disambiguate(locationResults) {		
		var resultsList = jQuery("#results ul");
		var bounds = null;
		
		jQuery.each(locationResults, function(_, location) {
			var latlng = new google.maps.LatLng(location.latitude, location.longitude);
			var address = location.formattedAddress;
			var neighborhood = location.neighborhood;
			
			var marker = routeMap.addDisambiguationMarkerWithContent(latlng, address, neighborhood);

		    // sidebar item
			var link = jQuery("<a href='#'></a>")
							.text(address);

			var listItem = jQuery("<li></li>")
							.addClass("locationItem")
							.append(link);

			resultsList.append(listItem);

			link.click(function(e) {
				e.preventDefault();

				jQuery.history.load(jQuery(this).text());
			});

			link.hover(function() {
				marker.setAnimation(google.maps.Animation.BOUNCE);
				routeMap.activateLocationIcon(marker);
			}, function() {
				marker.setAnimation(null);
				routeMap.deactivateLocationIcon(marker);
			});

			// calculate extent of all options
			if(bounds === null) {
				bounds = new google.maps.LatLngBounds(latlng, latlng);
			} else {
				bounds.extend(latlng);
			}
		});
		
		routeMap.showBounds(bounds);
		results.show();
	}

	// display (few enough) routes on map and in legend
	function showRoutesOnMap(routeResults) {
		var legendList = jQuery("#legend ul");
		
		jQuery.each(routeResults, function(_, routeResult) {	

			var titleBox = jQuery("<p></p>")
							.addClass("name")
							.text(routeResult.routeIdWithoutAgency + " " + routeResult.longName)
							.css("border-bottom", "5px solid #" + routeResult.color)
							.css("cursor", "pointer");

			var descriptionBox = jQuery("<p></p>")
							.addClass("description")
							.text(routeResult.description);

			var serviceAlertList = jQuery("<ul></ul>")
							.addClass("alerts");
			
			jQuery.each(routeResult.serviceAlerts, function(_, alert) {
				var alertItem = jQuery("<li></li>")
									.text(alert.value);
				
				serviceAlertList.append(alertItem);
			});
			
			var listItem = jQuery("<li></li>")
							.addClass("legendItem")
							.append(titleBox)
							.append(serviceAlertList)
							.append(descriptionBox);
	
			legendList.append(listItem);
			
			// on double click of title pan to route extent (unless zoomed in)
			titleBox.click(function(e) {
				e.preventDefault();
				routeMap.panToRoute(routeResult);
			});

			// directions
			jQuery.each(routeResult.destinations, function(_, destination) {
				var directionHeader = jQuery("<p></p>")
											.text("to " + destination.headsign);

				var stopsList = jQuery("<ul></ul>")
											.addClass("stops");

				var destinationContainer = jQuery("<p></p>")
											.addClass("destination")
											.append(directionHeader)
											.append(stopsList);

				// stops for this destination
				jQuery.each(destination.stops, function(__, stop) {
					var stopLink = jQuery("<a href='#'></a>")
									.text(stop.name);
					
					var r_color = (routeResult.color !== null) ? routeResult.color : "none";

					var stopItem = jQuery('<li class="r_' + r_color + '"></li>')
									.append(stopLink);
	
					stopsList.append(stopItem);

					stopLink.click(function(e) {
						e.preventDefault();

						routeMap.showPopupForStopId(stop.stopId);
						console.log(stop.stopId);
					});
					stopLink.mouseenter(function(e) {
						e.preventDefault();
						routeMap.showStopIcon(stop.stopId);
					});
					stopLink.mouseout(function(e) {
						e.preventDefault();
						routeMap.hideStopIcon(stop.stopId);
					});
				});

				// accordion-ize
				destinationContainer.accordion({ header: 'p', 
					collapsible: true, 
					active: false, 
					autoHeight: false });
				
				listItem.append(destinationContainer);
			});

			routeMap.showRoute(routeResult);
		});
		
		// pan to extent of first few routes in legend TODO
		if (routeResults.length > 0) {
			routeMap.panToRoute(routeResults[0]);
		}
		
		legend.show();
	}

	// show many (too many to show on map) routes to user
	function showRoutePickerList(routeResults) {
		var resultsList = jQuery("#results ul");

		jQuery.each(routeResults, function(_, route) {
			var link = jQuery("<a href='#'></a>")
							.text(route.name)
							.attr("title", route.description);

			var listItem = jQuery("<li></li>")
							.addClass("routeItem")
							.append(link);
			
			resultsList.append(listItem);

			// polyline hover
			var allPolylines = [];
			jQuery.each(route.destinations, function(__, destination) {
				jQuery.each(destination.polylines, function(___, polyline) {
					allPolylines.push(polyline);
				});
			});
			
			link.hover(function() {
				routeMap.showHoverPolyline(allPolylines, route.color);
			}, function() {
				routeMap.removeHoverPolyline();
			});
			
			// search link handler
			link.click(function(e) {
				e.preventDefault();
				
				jQuery.history.load(jQuery(this).text());
			});
		});
		
		results.show();
	}

	// process search results
	function doSearch(q) {
		welcome.hide();
		legend.hide();
		results.hide();
		loading.show();

		var resultsList = jQuery("#results ul");
		var legendList = jQuery("#legend ul");

		legendList.empty();
		resultsList.empty();
		routeMap.removeAllRoutes();
		routeMap.removeDisambiguationMarkers();
		
		jQuery.getJSON(OBA.Config.searchUrl + "?callback=?", {q: q }, function(json) { 
			var resultCount = json.searchResults.length;
			if(resultCount === 0) {
				legend.hide();
				results.hide();
				noResults.show();
				loading.hide();
				return;
			} else {
				noResults.hide();
			}
			loading.hide();

			OBA.Config.analyticsFunction("Search", q + " [" + resultCount + "]");
			
			var resultType = json.searchResults[0].resultType;			
			if(resultCount === 1 && (resultType === "LocationResult" || resultType === "StopResult")) {
					var result = json.searchResults[0];

					// region (zip code or borough)
					if(resultType === "LocationResult" && result.region === true) {
						var bounds = result.bounds;
						var latLngBounds = new google.maps.LatLngBounds(
								new google.maps.LatLng(bounds.minLat, bounds.minLon), 
								new google.maps.LatLng(bounds.maxLat, bounds.maxLon));
						
						showRoutePickerList(result.nearbyRoutes);
						routeMap.showBounds(latLngBounds);

					// intersection or stop ID
					} else {
						showRoutesOnMap(result.nearbyRoutes);
						routeMap.showLocation(result.latitude, result.longitude);

						if(resultType === "StopResult") {
							routeMap.showPopupForStopId(result.stopId);
						}
					}
			} else {
				// location disambiguation
				if(resultType === "LocationResult") {
					disambiguate(json.searchResults);
					
				// routes (e.g. S74 itself or S74 + S74 LTD)
				} else if(resultType === "RouteResult") {
					showRoutesOnMap(json.searchResults);
				}
			}
		});
		}
	
	return {
		initialize: function() {
			addSearchBehavior();
			addResizeBehavior();
			
			// deep link handler
			jQuery.history.init(function(hash) {
				if(hash !== null && hash !== "") {
					var searchInput = jQuery("#searchbar form input[type=text]");
					
					searchInput.val(hash);
					doSearch(hash);
					}
				});
			}
	};
};

jQuery(document).ready(function() { OBA.Sidebar().initialize(); });
