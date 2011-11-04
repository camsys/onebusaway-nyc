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
	var routeMap = null;	

	// elements for search results/did you mean...
	var searchResultsDiv = $('#results');
	var searchResultsUlDiv = $('#results ul');
	var searchInput = jQuery("#search input[type=text]");
	
	// elements for interactive legend
	var legendDiv = $('#legend');
	var legendUlDiv = $('#legend ul');
	
	var displayedRoutes = {};
	
	
	function addSearchBehavior() {
		var searchForm = jQuery("#search");
		
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
		
		// add overflow TODO until CSS styled
		searchBarDiv.css("overflow", "auto");
		
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
	
	/**
	 * Adds a location to a map using a special icon and icon shadow.
	 * 
	 * @param latitude
	 * @param longitude 
	 * @param name - of location
	 * @param item - left panel bar element location represents
	 * @param popupDescription - content for popup
	 */
	function addLocationToMap (latitude, longitude, name, item, popupDescription) {
		
		var normalIcon = 'img/location/beachflag.png';
		var iconShadow = 'img/location/beachflag_shadow.png';
		var activeIcon = 'img/location/beachflag_hover.png';
		
		var latlon = new google.maps.LatLng(latitude, longitude);

		var image = new google.maps.MarkerImage(normalIcon,
	      new google.maps.Size(20, 32),
	      new google.maps.Point(0,0),
	      new google.maps.Point(0, 32));
		
		var shadow = new google.maps.MarkerImage(iconShadow,
	      new google.maps.Size(37, 32),
	      new google.maps.Point(0,0),
	      new google.maps.Point(0, 32));
		
	    // shape = clickable region of the icon.
		var shape = { coord: [1, 1, 1, 20, 18, 20, 18 , 1], type: 'poly' };	
		
		if (popupDescription !== "") {
			popupDescription += "<br /><br />";
		}
		
		// Add popup content
		// split street name and city/zip for readability/reduced popup size
		var stIndex = name.indexOf(",");
		var streetname, citystzip, address = "";
		
		if (stIndex > -1) {
			streetname = name.substring(0, stIndex);
			citystzip = name.substring(stIndex+2);   
			address = streetname + '<br />' + citystzip;
		} else {
			address = name;
		}

		var popupString = '<div id="content"><div id="bodyContent"><h3><b>' + address + '</b></h3>' + 
							popupDescription + '</div></div>';
		
		var marker = routeMap.createMarker(latlon, {shadow: shadow, icon: image, name: name, popup: popupString});
	}
	
	/**
	 * Adds a search result location to a map using a special icon and icon shadow.
	 * 
	 * @param latitude
	 * @param longitude 
	 * @param name - of location
	 * @param item - search bar element location represents
	 * @param popupDescription - content for popup
	 */
	function addSearchResultToMap (latitude, longitude, name, item, popupDescription) {
		
		var normalIcon = 'img/location/beachflag.png';
		var iconShadow = 'img/location/beachflag_shadow.png';
		var activeIcon = 'img/location/beachflag_hover.png';
		
		var latlon = new google.maps.LatLng(latitude, longitude);

		var image = new google.maps.MarkerImage(normalIcon,
	      new google.maps.Size(20, 32),
	      new google.maps.Point(0,0),
	      new google.maps.Point(0, 32));
		
		var shadow = new google.maps.MarkerImage(iconShadow,
	      new google.maps.Size(37, 32),
	      new google.maps.Point(0,0),
	      new google.maps.Point(0, 32));
		
	    // shape = clickable region of the icon.
		var shape = { coord: [1, 1, 1, 20, 18, 20, 18 , 1], type: 'poly' };	
		
		if (popupDescription !== "") {
			popupDescription += "<br /><br />";
		}
		
		// Add popup content
		// split street name and city/zip for readability
		var stIndex = name.indexOf(",");
		var streetname = name.substring(0, stIndex);
		var citystzip = name.substring(stIndex+2);   
		var address = streetname + '<br />' + citystzip; 
		
		var popupString = '<div id="content"><div id="bodyContent"><h3><b>' + address + '</b></h3>' + 
							popupDescription + '</div></div>';
		
		var marker = routeMap.createMarker(latlon, {shadow: shadow, icon: image, name: name, popup: popupString});
		var bouncingMarker = null;
		
		function setItemAsActive() {
			item.css("color", "red");    // TODO change to active/inactive once CSS is defined
		};
		function setItemAsInactive() {
			item.css("color", "blue");
		};
		function setMarkerAsMildlyActive() {
			if (bouncingMarker !==null) { bouncingMarker.setAnimation(null); }		
			marker.setIcon(activeIcon);
			marker.setAnimation(google.maps.Animation.BOUNCE);
			window.setTimeout(function() { marker.setAnimation(null); }, 1200);
			bouncingMarker = marker;
		}
		function setMarkerAsActive() {
			marker.setIcon(activeIcon);
			//marker.setAnimation(google.maps.Animation.BOUNCE);  
			routeMap.showPopup(marker);
		}
		function setMarkerAsInactive() {
			marker.setIcon(normalIcon);
			marker.setAnimation(null);
			routeMap.closePopup(marker);
		}
		
		// Add hover and click events to text in search panel
		item.mouseenter( function() {
			setItemAsActive();
			setMarkerAsMildlyActive(); 
		});
		item.mouseout( function() {
			setItemAsInactive();
			setMarkerAsInactive();
		});
		item.click( function() {
			setMarkerAsInactive();			
			routeMap.showLocationFromPoint(latlon);
			searchInput.val(name);
			hideDidYouMean(marker);
		});
		
		// Add hover event to map marker
		google.maps.event.addListener(marker, 'mouseover', function() { setItemAsActive(); setMarkerAsActive(); }); 
		google.maps.event.addListener(marker, 'mouseout', function() { setItemAsInactive(); setMarkerAsInactive(); });
		google.maps.event.addListener(marker, 'click', function() { item.click(); });
		
	}
	

	// show user list of addresses
	function disambiguate(locationResults) {
		var counter = 0;	
		searchResultsDiv.css("line-height", "150%"); // TODO remove when Buck styles CSS
		
		jQuery.each(locationResults, function(_, locationResult) {

			if (locationResult.type == "locationResult" || locationResult.type.indexOf("location") > -1) {
				// Create <li> for result text
				var name = locationResult.name;
				
				var item = $('<li id="item_' + counter + '">' + name + '</li>');
				item.css("color", "blue");
				item.css("cursor", "pointer");
					
				searchResultsUlDiv.append(item);
				counter += 1;
				var popupDescription = ""; 
				
				// Add neighborhood if available
				if (locationResult.neighborhood !== null) {
					popupDescription = locationResult.neighborhood;
					item.append(" (" + popupDescription + ")");  
				}
				
				// Add marker to map with hover and click options
				if (locationResult.latitude !== null) {
					addSearchResultToMap(locationResult.latitude, locationResult.longitude, name, item, popupDescription);
				}
			} // TODO Figure out what to do with routes, zip codes, boroughs, etc.
		});
		searchResultsDiv.show();
	}

	
	// single route search result view (displays a route on map and in legend)
	function displayRoute(routeResult) {
		var name = routeResult.name;
		
		// add to current set of routes  
		if (displayedRoutes[name] != null) {
			return;
		}
		displayedRoutes[name] = routeResult;

		var color = routeResult.color;
		var description = routeResult.description;
		var destinations = routeResult.destinations;
		
		// Create a checked checkbox for the route
		var routeCheckbox = $('<input type="checkbox" />').attr('id', 'checkbox_' + name);	
		routeCheckbox.attr("checked", true);
		routeCheckbox.click(function() {
		    //routeMap.toggleRoute(name);
		});
		
		var item = $('<li class="legend_item"></li>');
		item.append(routeCheckbox);
		item.append(' ' + name);
		
		// Style the route name based on route color
		if (routeResult.color == null) {
			color = "black";
		} else {
			color = routeResult.color;
		}
		item.css("color", color);
		item.css("cursor", "pointer");
		legendUlDiv.append(item);
		
		// Add route description if available
		if (description !== null) {
			item.append("<h4>" + description + "</h4>");  
		}
;	  
		var headsign_list = $('<div class="accordion"></div>');
		headsign_list.css("height", "100px");
		
		// Add destinations as drop-down lists
		jQuery.each(destinations, function(_, destination) {			
			var headsign = destination.headsign;			
			if (headsign !== null) {
				var headsign_item = $('<h5></h5>').append($('<a href="#">' + headsign + '</a>'));					
					
				// add stops
				var stops = destination.stops;	
				if (stops !== null) {
					var innerStopList = $('<ul></ul>');
					var visibleStops = stopsWithinBounds(stops);
					
					jQuery.each(visibleStops, function(_, visibleStop) {	
						stopLink = $('<li>' + visibleStop.name + '</li>');
						stopLink.css("color", color);
						stopLink.css("cursor", "pointer");
						stopLink.click(function() {
						   // routeMap.highlightStop(stop.stopIdWithoutAgency);
						});
						innerStopList.append(stopLink);
					});
					headsign_list.append(headsign_item);
					headsign_list.append($('<div></div>').append(innerStopList));
				}
			}
		});
	
		item.append(headsign_list);	

		// add route to map
		routeMap.showRoute(routeResult);

	}
	
	// return an array of points within the passed in bounds
	function stopsWithinBounds(stops) {
		var stopsInBounds = [];
		var currentExtent = routeMap.getBounds();
		
		jQuery.each(stops, function(_, stop) {
			var coordinate = new google.maps.LatLng(stop.latitude, stop.longitude);
			if (currentExtent.contains(coordinate)) {
				stopsInBounds.push(stop);
			}
		});
		return stopsInBounds;
	}

	// display (few enough) routes on map and in legend
	function showRoutesOnMap(routeResults) {
		jQuery.each(routeResults, function(_, routeResult) {	
			displayRoute(routeResult);
		});
		
		// initialize accordion plugin with all panels closed
		$('.accordion').accordion({ header: 'h5', collapsible: true, active: false, autoHeight: false });
		
		legendDiv.show();
		//routeMap.removeRoutesNotInSet(routeResults);
	}
	
	// single route search result view (shows ENTIRE route span)
	function showSingleRoute(routeResult) {
		routeMap.showRoute(routeResult);
		legendDiv.show();
	}

	// show a region's worth of routes--user chooses one, and 
	// user is then shown single route search result view.
	function showRoutePickerList(routeResults) {
		var target = searchResultsUlDiv;
		//routeMap.removeAllRoutes();
		
		jQuery.each(routeResults, function(_, route) {
			target.add("<li>" + route.name + "</li>");
		});
	}
	
	// clear results list (e.g. if user enters a new query)
	function removeSearchResults() {		
		if (searchResultsUlDiv.children().length > 1) {	
			searchResultsDiv.children().remove();
			searchResultsDiv.append($('<ul></ul>'));
			routeMap.clearMarkers();
			searchResultsUlDiv = $('#results ul');
		}
	}
	
	// clear results list, hide Did You Mean..., re-add marker
	function hideDidYouMean(marker) {
		if (searchResultsUlDiv.children().length > 1) {	
			searchResultsDiv.children().remove();
			searchResultsDiv.append($('<ul></ul>'));
			routeMap.clearMarkers(marker);
			searchResultsUlDiv = $('#results ul');
		}
		searchResultsDiv.hide();
	}

	// process search results
	function doSearch(q) {
		// remove any existing search results
		removeSearchResults();
		
		jQuery.getJSON(OBA.Config.searchUrl, {q: q }, function(json) { 
			var resultCount = json.searchResults.length;
			if(resultCount === 0)
				return;

			var resultType = json.searchResults[0].type;			
			if(resultType === "locationResult" || resultType === "stopResult") {
				if(resultCount > 1) {
					disambiguate(json.searchResults);

				// 1 stop or location--move map 
				// (can never get > 1 stop from search method)
				} else {
					var result = json.searchResults[0];
					routeMap.showLocation(result.latitude, result.longitude);
					
					// add a marker to the map, optionally pass in a div in search panel
					addLocationToMap (result.latitude, result.longitude, result.name, null, result.neighborhood);
					searchInput.val(result.name);
				}
			} else if(resultType === "routeResult") {
				// can never get > 1 route from the search method!
				if(resultCount == 1) {
					var result = json.searchResults[0];
					showSingleRoute(result);
				}
			}
		});		
	}
	
	// constructor:
	var mapMoveCallbackFn = function() {
						
		jQuery.getJSON(OBA.Config.routesWithinBoundsUrl, { bounds: routeMap.getBounds().toUrlValue() }, 
		function(json) {
			var resultCount = json.searchResults.length;
			if(resultCount === 0)
				return;
			
			if (resultCount > 5) {
				showRoutePickerList(json.searchResults);
			} else {
				showRoutesOnMap(json.searchResults);
			}			
		});
	};

	routeMap = OBA.RouteMap(mapNode, mapMoveCallbackFn);
		
	return {
		initialize: function() {
			addSearchBehavior();
			addResizeBehavior();
		}
	};
};

jQuery(document).ready(function() { OBA.Sidebar().initialize(); });
