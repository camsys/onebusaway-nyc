var OBA = window.OBA || {};

OBA.Mobile = (function() {
	var turnOffButton = null;
	var turnOnButton = null;

	var locationField = null;
	var lastLatitude = null;
	var lastLongitude = null; 

	// add location toggle UI to DOM
	var addToggleUI = function() {
		var searchPanelForm = jQuery("#searchPanel form");		

		var toggleUI = jQuery("<p>Share location:</p>")
			.attr("id", "toggleUI");

		turnOffButton = jQuery("<a href='#'>Turn Off</a>")
			.click(function() {
				turnOffGeolocation();
			});

		turnOnButton = jQuery("<a href='#'>Turn On</a>")
			.click(function() {
				turnOnGeolocation();
			});
		
		toggleUI.append(turnOffButton);
		toggleUI.append(turnOnButton);	
		
		searchPanelForm.before(toggleUI);
	};
	
	// remove location toggle UI from DOM
	var removeToggleUI = function() {
		turnOffGeolocation();
		
		jQuery("#toggleUI")
			.remove();
	};
	
	// remove all instances of location being sent back to server
	var removeLocationFields = function() {
		if(locationField !== null) {
			locationField.remove();
		}
		
		// update links on this page to NOT include location
		jQuery.each(jQuery.find("a"), function(_, _link) {
			var link = jQuery(_link);
			
			var existingHref = link.attr("href");
			if(typeof existingHref !== 'undefined' && existingHref.indexOf("&l=") > -1) {
				var newHref = existingHref.replace(/&l=[^&|#|?]*/i, "");
				link.attr("href", newHref);
			}
		});

	};
	
	// rewrite links to include location in query
	var updateLocationFields = function() {
		if(lastLatitude !== null && lastLongitude !== null) {
			// add location field to form if not there already
			if(locationField === null) {
				locationField = jQuery("<input></input>")
					.attr("type", "hidden")
					.attr("name", "l");

				jQuery("#searchPanel form")
					.append(locationField);
			}
			
			locationField.val(lastLatitude + "," + lastLongitude);

			// update links on this page to include location
			jQuery.each(jQuery.find("a"), function(_, _link) {
				var link = jQuery(_link);
				
				var existingHref = link.attr("href");
				if(typeof existingHref !== 'undefined' && existingHref.indexOf("&l=") > -1) {
					var newHref = existingHref.replace(/&l=[^&|#|?]*/i, "&l=" + lastLatitude + "%2C" + lastLongitude);
					link.attr("href", newHref);
				}
			});
		}
	};

	// event when user turns off location
	var turnOffGeolocation = function() {
		removeLocationFields();
		
		turnOffButton.text("Is Off");
		turnOnButton.text("Turn On");
	};
	
	// event when user turns on location
	var turnOnGeolocation = function() {
		updateLocationFields();
		
		turnOffButton.text("Turn Off");
		turnOnButton.text("Is On");
	};
		
	return {
		initialize: function() {
			if(navigator.geolocation) {
				addToggleUI();

				navigator.geolocation.getCurrentPosition(function(location) {
					lastLatitude = location.coords.latitude;
					lastLongitude = location.coords.longitude;

					updateLocationFields();
				}, removeToggleUI);
				
				turnOnGeolocation();
			}			
		}
	};
})();

jQuery(document).ready(function() { OBA.Mobile.initialize(); });

