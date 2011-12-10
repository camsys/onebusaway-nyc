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
	var theWindow = jQuery(window),
		mapDiv = jQuery("#map"), 
		topBarDiv = jQuery("#topbar"), 
		searchBarDiv = jQuery("#searchbar"), 
		mainbox = jQuery("#mainbox"),
		menuBar = jQuery("#cssmenu1"),
		welcome = jQuery("#welcome"),
		legend = jQuery("#legend"),
		results = jQuery("#results"),
		noResults = jQuery("#no-results"),
		loading = jQuery("#loading");

	var routeMap = OBA.RouteMap(document.getElementById("map"));

	function addSearchBehavior() {
		var searchForm = jQuery("#searchbar form"),
			searchInput = jQuery("#searchbar form input[type=text]");
		
		searchForm.submit(function(e) {
			e.preventDefault();
			
			jQuery.history.load(searchInput.val());
		});
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
				mainbox.css("width", w - 150); // 75px margin on each side
			}

			// size set so we can have MTA menu items calculate their widths properly
			menuBar.width(mainbox.width());
		};
		resize();

		// call when the window is resized
		theWindow.resize(resize);
	}

	// show user list of addresses
	function disambiguate(locationResults) {		
		var resultsList = jQuery("<ul></ul>")
							.appendTo(results);

		var bounds = null;
		jQuery.each(locationResults, function(_, location) {
			var latlng = new google.maps.LatLng(location.latitude, location.longitude);
			var address = location.formattedAddress;
			var neighborhood = location.neighborhood;
			
			var marker = routeMap.addDisambiguationMarkerWithContent(latlng, address, neighborhood);

		    // sidebar item
			var link = jQuery("<a href='#" + address + "'></a>")
							.text(address);

			var listItem = jQuery("<li></li>")
							.addClass("locationItem")
							.append(link);

			resultsList.append(listItem);

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
	
	// separate nearby routes from routes for a stop 
	// (need to copys routes anyway to get service alerts field)
	function reorderRoutes(routes, routesFirst) {			
		var nearbyRoutes = {}, routesForStop = [];	

		jQuery.each(routes, function(_, route) {
			nearbyRoutes[route.routeIdWithoutAgency] = route;
		});
		
		jQuery.each(routesFirst, function(_, routeFirst) {
			routesForStop.push(nearbyRoutes[routeFirst.routeIdWithoutAgency]);
			delete nearbyRoutes[routeFirst.routeIdWithoutAgency];
		});
		
		return [nearbyRoutes, routesForStop];
	}

	// display routes on map and in legend, order by 1) for stop, then 2) nearby
	function showRoutesOnMap(routeResults, routesFirst) {
		var nearbyRoutes = routeResults;
		
		var nearby = jQuery("#legend > #nearby");
		var for_stop = jQuery("#legend > #for_stop");

		if (routesFirst !== undefined && routesFirst.length !== 0) {	
			// case 0: some routes for stop -> sort & list them 1st
			if (routeResults.length !== routesFirst.length) {
				var routelists = reorderRoutes(routeResults, routesFirst);
				nearbyRoutes = routelists[0];
				routesFirst = routelists[1];
			} else {
			// case 1: routes for stop equals all routes -> list all as 1st
				routesFirst = routeResults;
				nearbyRoutes = null;
			}
			// Add as "routes for this stop"
			for_stop.text("Routes for this stop:");
			
			var routesAtStopLegend = jQuery("<ul></ul>").appendTo(for_stop);
			addRoutesToLegend(routesFirst, routesAtStopLegend);

			for_stop.show();
			nearby.hide();
		}
		
		// case 2: no routes for stop -> Add all as "nearby"
		if (nearbyRoutes !== null && nearbyRoutes.length !== 0) {
			nearby.text("Nearby routes:");
			
			var nearbyRoutesLegend = jQuery("<ul></ul>").appendTo(nearby);	
			addRoutesToLegend(nearbyRoutes, nearbyRoutesLegend);
			
			nearby.show();
			for_stop.hide();
		}
		
		// pan to extent of first few routes in legend
		if (routeResults.length > 0) {
			if (routesFirst !== undefined) {
				routeMap.panToRoute(routesFirst[0]);
			} else {
				routeMap.panToRoute(nearbyRoutes[0]);
			}
		}	
		
		legend.show();
	}
		
	function addRoutesToLegend(routeResults, legendList) {
		jQuery.each(routeResults, function(_, routeResult) {				
			var titleBox = jQuery("<p></p>")
							.addClass("name")
							.text(routeResult.routeIdWithoutAgency + " " + routeResult.longName)
							.css("border-bottom", "5px solid #" + routeResult.color)
							.css("cursor", "pointer");

			var descriptionBox = jQuery("<p></p>")
							.addClass("description")
							.text(routeResult.description);

			// service alerts
			var serviceAlertList = jQuery("<ul></ul>")
							.addClass("alerts");
						
			jQuery.each(routeResult.serviceAlerts, function(_, alert) {
				var alertItem = jQuery("<li></li>")
									.text(alert.value);
				
				serviceAlertList.append(alertItem);
			});

			// legend item
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
				var directionHeader = jQuery("<p></p>");
				
				jQuery("<span></span>")
					.text("to " + destination.headsign)
					.appendTo(directionHeader);

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
					
					var routeColor = (routeResult.color !== null) ? routeResult.color : "none";
					var stopItem = jQuery('<li class="r_' + routeColor + '"></li>')
									.append(stopLink);
	
					stopsList.append(stopItem);

					stopLink.click(function(e) {
						e.preventDefault();

						routeMap.showPopupForStopId(stop.stopId);
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
	}

	// show many (too many to show on map) routes to user
	function showRoutePickerList(routeResults) {
		var resultsList = jQuery("<ul></ul>")
							.appendTo(results);

		jQuery.each(routeResults, function(_, route) {
			var link = jQuery("<a href='#" + route.name + "'></a>")
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
		});
		
		results.show();
	}
	
	function resetSearchPanelAndMap() {
		welcome.hide();
		legend.hide();
		results.hide();
		
		jQuery("#results ul").remove();
		jQuery("#legend #for_stop").children().empty();
		jQuery("#legend #nearby").children().empty();

		routeMap.removeAllRoutes();
		routeMap.removeDisambiguationMarkers();
	}

	// process search results
	function doSearch(q) {
		resetSearchPanelAndMap();
		loading.show();
		
		jQuery.getJSON(OBA.Config.searchUrl + "?callback=?", { q: q }, function(json) { 
			loading.hide();

			var resultCount = json.searchResults.length;
			if(resultCount === 0) {
				noResults.show();
				return;
			} else {
				noResults.hide();
			}

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
						
						// location exists, but no nearby routes exist
						var nearbyRoutes = result.nearbyRoutes;
						if(nearbyRoutes.length === 0) {
							noResults.show();
							return;							
						}
						
						showRoutePickerList(result.nearbyRoutes);
						routeMap.showBounds(latLngBounds);

					// intersection or stop ID
					} else {									
						var listFirst = result.routesAvailable;	
						showRoutesOnMap(result.nearbyRoutes, listFirst);
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

