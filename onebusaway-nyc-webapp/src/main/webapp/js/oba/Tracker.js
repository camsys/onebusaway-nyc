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

OBA.Tracker = function() {
	// elements for the resize handler
	var theWindow = null;
	var headerDiv, footerDiv, contentDiv, homeAlertDiv = null;

	var mapNode = document.getElementById("map");
	var routeMap = OBA.RouteMap(mapNode);
	var map = routeMap.getMap();
	
	function addSearchBehavior() {
		var searchForm = jQuery("#search");
		var searchInput = jQuery("#search input[type=text]");
		var noResults = jQuery("#no-results");

		var loseFocus = function() {
			if(searchInput.val() === "" || searchInput.val() === "Intersection along a route or stop ID") {
				searchInput.val("Intersection along a route or stop ID");
				searchInput.addClass("inactive");
			}
		};
		
		loseFocus();
		searchInput.blur(loseFocus);
		
		searchInput.focus(function() {
			if(searchInput.val() === "Intersection along a route or stop ID") {
				searchInput.val("");
			}
			noResults.hide();
			searchInput.removeClass("inactive");
		});

		searchForm.submit(function(e) {
			noResults.hide();
			doSearch(searchInput.val());

			OBA.Config.analyticsFunction("Search", searchInput.val());

			return false;
		});
	
		// add search control to Google Maps DIV so the popups avoid popping up under it.	
		map.controls[google.maps.ControlPosition.TOP_RIGHT].push(searchForm.get(0));
	}
	
	function addShareLinkBehavior() {
		var shareLinkDiv = jQuery("#share_link");

		// close button inside link window
		var closeFn = function() {
			shareLinkDiv.hide();			
			return false;
		};
		
		jQuery("#share_link a.close").click(closeFn);
		google.maps.event.addListener(map, 'dragstart', closeFn); 

		// add item to header
		var linkItem = jQuery("<li></li>").addClass("right")
						.addClass("link")
						.append(jQuery("<a></a>"));
		jQuery("#header ul").append(linkItem);

		jQuery("#header ul li.link a").click(function() {
			shareLinkDiv.show();			
		
			OBA.Config.analyticsFunction("Grab Share Link", null);

			var shareLinkUrl= jQuery("#share_link .content input");
			var searchInput = jQuery("#search input[type=text]");

			var url = window.location.href.match(/([^#]*)/i)[0] + "#";
			if(OBA.popupMarker !== null && OBA.popupMarker.getType() === "stop") {				
				url += OBA.Util.parseEntityId(OBA.popupMarker.getId()); 
			} else if(searchInput.val() !== null && searchInput.val() !== ""
				&& searchInput.val() !== "Intersection along a route or stop ID") {
				url += searchInput.val();
			}

			shareLinkUrl.val(url);
			shareLinkUrl.select();
			
			return false;
		});
	}	

	function addAlertBehavior() {
		var welcomeDiv = jQuery("#welcome");
		var contentDiv = jQuery("#welcome .content");
		var closeButton = jQuery("#welcome a.close");

		closeButton.click(function() {
			welcomeDiv.hide();
			jQuery.cookie("didShowWelcome", "true", { expires: 9999 });
			return false;
		});

		// show the welcome message if the user hasn't seen it before
		var didShowWelcome = jQuery.cookie("didShowWelcome");
		if(contentDiv.text() !== "" && didShowWelcome !== "true") {
			welcomeDiv.show();
		}
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

	// if query is parsable as an integer, this is a stop ID. (FIXME?)
	function queryIsForStopId(q) {
		if(q === null || q.length !== 6) { return false; }
		try {
			for(var i = 0; i < q.length; i++) {
				var c = q.charAt(i);
				var t = parseInt(c);				
				if(typeof t === undefined || t === null || isNaN(t) === true) {
					return false;
				}					
			}
			return true;
		} catch(e) {
			return false;
		}
	}
	
	function doSearch(q) {
		jQuery.getJSON(OBA.Config.searchUrl, {q: q}, function(json) { 
			var noResults = jQuery("#no-results");
			
			if(json.searchResults.length === 0) {
				noResults.fadeIn();
			} else {
				noResults.hide();

				if(OBA.popupMarker !== null) {
					OBA.popupMarker.getPopup().hide();
				}
				
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
					if(queryIsForStopId(q) === true) {
						map.setZoom(16);
						routeMap.showStop(json.searchResults[0].stopId);					
					} else {
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
					}
				}
			}
		});   				
	}
	
	return {
		initialize: function() {
			addSearchBehavior();
			addShareLinkBehavior();
			addAlertBehavior();
			addResizeBehavior();

			// add default/pilot routes to the map
			routeMap.addRoute(OBA.Config.agencyId + "_B63", ["1", "0"], function(routeId, directionId) {
				var bounds = routeMap.getBounds(routeId, directionId);
				if(bounds !== null) {
					map.fitBounds(bounds);
				}
			});	
			
			// load any deeplink identified search results
			google.maps.event.addListener(map, 'projection_changed', function() {
	            jQuery.history.init(function(hash) {
	            	if(hash !== null && hash !== "") {
	            		var shareLinkDiv = jQuery("#share_link");
	            		shareLinkDiv.hide();
	            		
	            		var searchInput = jQuery("#search input[type=text]");
	            		searchInput.val(hash).removeClass("inactive");
	            		doSearch(hash);
	            		
	            		OBA.Config.analyticsFunction("Deep Link", hash);
	            	}
	            });				
			}); 
		}
	};
};

jQuery(document).ready(function() { OBA.Tracker().initialize(); });
