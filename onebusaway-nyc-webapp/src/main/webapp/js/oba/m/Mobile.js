var OBA = window.OBA || {};

OBA.Mobile = (function() {
	var locationField = null;

	var turnOffButton = null;
	var turnOnButton = null;
	var nearbyButton = null;

	var lastLatitude = null;
	var lastLongitude = null; 
	
	function addAutocompleteBehavior() {
		jQuery("#bustimesearch").autocomplete({
			source: "../" + OBA.Config.autocompleteUrl,
			select: function(event, ui) {
		        if(ui.item){
		        	jQuery('#bustimesearch').val(ui.item.value);
		        }
		        jQuery('#bustime_search_form').submit();
		    }
		});
	}
	
	function addRefreshBehavior() {
		// refresh button logic
		var refreshBar = jQuery("#refresh")
					.css("position", "absolute")
					.css("right", "20")
					.css("left", "20");

		var refreshTimestamp = refreshBar
								.find("strong");
		
		// ajax refresh for browsers that support it
		refreshBar.find("a").click(function(e) {
			e.preventDefault();
		
			refreshTimestamp.text("Loading...");
			
			jQuery("#content")
				.load(location.href + " #content>*", null, function() {
					refreshTimestamp.text("Updated " + new Date().format("mediumTime"));
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
		var searchPanelForm = jQuery("#searchPanel form");

		var toggleUI = jQuery("<p>Use my location:</p>")
			.attr("id", "toggleUI");

		turnOffButton = jQuery("<a href='#'>Turn Off</a>")
			.click(function(e) {
				e.preventDefault();				
				turnOffGeolocation();
				return false;
			});

		turnOnButton = jQuery("<a href='#'>Turn On</a>")
			.click(function(e) {
				e.preventDefault();
				turnOnGeolocation();
				return false;
			});
		
		toggleUI.append(turnOffButton);
		toggleUI.append(turnOnButton);	
		
		searchPanelForm.before(toggleUI);
		
		// find nearby button
		var welcomeDiv = jQuery(".welcome");

		if(welcomeDiv.length > 0) {
			var nearbyForm = jQuery("<form id='nearby'></form>");

			nearbyButton = jQuery("<input type='button' value='Finding your location...'/>")
				.attr("disabled", "true")
				.appendTo(nearbyForm);

			nearbyButton.click(function(e) {
				e.preventDefault();
				searchPanelForm.find("#q").val("");
				searchPanelForm.submit();
				return false;
			});
			
			welcomeDiv.append(nearbyForm);
		}
	};
	
	function updatePageState() {
		var locationValue = "off";
		if(lastLatitude !== null && lastLongitude !== null) {
			locationValue = lastLatitude + "," + lastLongitude;
		}

		// update search field
		if(locationField !== null) {
			locationField.val(locationValue);
		}
			
		// update links on this page to include location
		jQuery.each(jQuery("body").find("a"), function(_, _link) {
			var link = jQuery(_link);
			var existingHref = link.attr("href");
			if(typeof existingHref !== 'undefined' && existingHref.indexOf("&l=") > -1) {
				var newHref = existingHref.replace(/&l=[^&|#|?]*/i, "&l=" + locationValue);
				link.attr("href", newHref);
			}
		});
			
		// update find stops nearby button
		if(nearbyButton !== null) {
			if(locationValue !== "off") {
				nearbyButton.removeAttr("disabled");
				nearbyButton.val("Find Stops Near Me");
			} else {
				nearbyButton.hide();
			}
		}
	};

	// event when user turns off location
	function turnOffGeolocation() {
		lastLatitude = null;
		lastLongitude = null;

		updatePageState();
		
		turnOffButton.text("Is Off").css("font-weight", "bold");
		turnOnButton.text("Turn On").css("font-weight", "normal");
	};
	
	// event when user turns on location
	function turnOnGeolocation() {
		// show "finding location" message button to user while 
		// location is being found
		if(nearbyButton !== null) {
			nearbyButton.attr("disabled", "true");
			nearbyButton.val("Finding your location...");
			nearbyButton.show();
		}

		navigator.geolocation.getCurrentPosition(function(location) {
			lastLatitude = location.coords.latitude;
			lastLongitude = location.coords.longitude;

			updatePageState();
		}, turnOffGeolocation);

		turnOffButton.text("Turn Off").css("font-weight", "normal");
		turnOnButton.text("Is On").css("font-weight", "bold");
	};
		
	return {
		initialize: function() {
			locationField = jQuery("#l");
			
			if(navigator.geolocation) {
				initLocationUI();					

				if(locationField.val() !== "off") {
					turnOnGeolocation();
				} else {
					turnOffGeolocation();
				}
			}			
			
			addRefreshBehavior();
			addAutocompleteBehavior();
		}
	};
})();

jQuery(document).ready(function() { OBA.Mobile.initialize(); });