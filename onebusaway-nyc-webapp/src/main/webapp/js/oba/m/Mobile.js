/*
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var OBA = window.OBA || {};

OBA.Mobile = (function() {
	
	var locationField = null;
	var typeField = null;
	
	function addAutocompleteBehavior() {
		
		var searchForm = jQuery("#searchPanel form");
		var searchInput = jQuery("#searchPanel form input[type=text]");

		searchInput.autocomplete({
			source : function(request, response) {
				var query = $.trim(request.term);
				if (query.length >= 3 && !query.match("^[^a-zA-Z0-9]*$")) {
					jQuery.get("../" + OBA.Config.autocompleteUrl, {
						term : query
					}, function(data) {
						response(data);
					});
				} else {
					response();
				}
			},
			delay: 250,
			minLength: 3,
			select: function(event, ui) {
				if(ui.item) {
					if (ui.item.value != ui.item.label) {
						searchInput.val(ui.item.label);
						// update location field
						if (locationField !== null)
							locationField.val(ui.item.value);
						if (typeField !== null)
							typeField.val("geocode");
					} else {
						searchInput.val(ui.item.value);
					}
					searchForm.submit();
					return false;
				}
			},
			focus: function (event, ui) {
				if (ui.item) {
					searchInput.val(ui.item.label);
				}
				return false;
			}
		});
		
		// Close the autocomplete list when the form is submitted
		searchForm.submit(function() {
			searchInput.autocomplete("close");
			return true;
		});
	}
	
	function addRefreshBehavior() {
		// refresh button logic
		var refreshBar = jQuery("#refresh")
					.css("position", "absolute")
					.css("right", "20")
					.css("left", "12");

		var refreshTimestamp = refreshBar
								.find("strong");
		
		// ajax refresh for browsers that support it
		refreshBar.find("a").click(function(e) {
			e.preventDefault();

			refreshTimestamp.text("Loading...");
			refreshBar.addClass("loading");

			jQuery("#content")
				.load(location.href + " #content>*", null, function() {
					refreshTimestamp.text("Updated " + new Date().format("mediumTime"));
					refreshBar.removeClass("loading");
				});
		});
				
		// scrolling/fixed refresh bar logic
		var contentDiv = jQuery("#content")
							.css("padding-top", refreshBar.height() * 1.5);				

		var topLimit = contentDiv.offset().top + (refreshBar.height() * 0.25) - 20;
		
		jQuery("body")
					.css("position", "relative");

		var theWindow = jQuery(window);
		var repositionRefreshBar = function() {
			var top = theWindow.scrollTop();

			if(top < topLimit) {
				top = topLimit;
			}
			
			refreshBar.css("top", top + 3);
		};
		repositionRefreshBar();
		
		theWindow.scroll(repositionRefreshBar)
					.resize(repositionRefreshBar);
	}
	
	function initLocationUI() {
		jQuery("#submitButton").removeClass("loading");
		
		var searchPanelForm = jQuery("#searchPanel form");
		
		var splitButton = jQuery("<div></div>").attr("id", "nearby-button-bar");
		
		var nearbyStopsBtn = jQuery("<div></div>").attr("id", "nearby-stops-button").attr("aria-label", "Find nearby stops using GPS").addClass("nearby-button").appendTo(splitButton);
		var nearbyRoutesBtn = jQuery("<div></div>").attr("id", "nearby-routes-button").attr("aria-label", "Find nearby routes using GPS").addClass("nearby-button").appendTo(splitButton);
		
		nearbyStopsBtn.append(jQuery("<div></div>").attr("id", "nearby-stops-button-icon").append(jQuery("<span></span>").addClass("nearby-text").text("Nearby Stops")));
		nearbyRoutesBtn.append(jQuery("<div></div>").attr("id", "nearby-routes-button-icon").append(jQuery("<span></span>").addClass("nearby-text").text("Nearby Routes")));
		
		searchPanelForm.before(splitButton);
				
		$( ".nearby-button" ).mousedown(function() {
			// change other button to mouse up
			if (jQuery(this).attr("id") === "nearby-stops-button") {
				nearbyRoutesBtn.removeClass("down");
			} else {
				nearbyStopsBtn.removeClass("down");
			}
			jQuery(this).addClass("down");
		});
		
		$( ".nearby-button" ).mouseup(function() {
			if (jQuery(this).attr("id") === "nearby-stops-button") {
				typeField.val("stops");
			} else {
				typeField.val("routes");
			}
			queryByLocation();
		});
	};
	
	// event when user turns on location
	function queryByLocation() {
		// show "finding location" message button to user while 
		// location is being found
		jQuery("#submitButton").addClass("loading");
		jQuery(".q").attr("placeholder", "Finding your location...");

		navigator.geolocation.getCurrentPosition(function(location) {
			
			jQuery(".q").attr("placeholder", "Searching...");

			// update search field
			if(locationField !== null) {
				locationField.val(location.coords.latitude + "," + location.coords.longitude);
			}
			
			var searchPanelForm = jQuery("#searchPanel form");
			
			searchPanelForm.find(".q").val("");
			searchPanelForm.submit();
			
		}, function() {
			alert("Unable to determine your location.");
			jQuery(".nearby-button").removeClass("down");
			jQuery("#submitButton").removeClass("loading");
			jQuery(".q").removeAttr("placeholder");
			
		});
	};
		
	return {
		initialize: function() {
			locationField = jQuery("#l");
			typeField = jQuery("#t");
			
			if(navigator.geolocation) {
				initLocationUI();
			}			
			
			addRefreshBehavior();
			addAutocompleteBehavior();
		}
	};
})();

jQuery(document).ready(function() { OBA.Mobile.initialize(); });