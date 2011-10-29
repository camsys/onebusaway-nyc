jQuery(document).ready(function() {

	var turnOffButton = null;
	var turnOnButton = null;

	var locationField = null;
	var lastLatitude = null;
	var lastLongitude = null; 

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
	
	var removeToggleUI = function() {
		turnOffGeolocation();
		
		jQuery("#toggleUI")
			.remove();
	};
	
	var updateLocationField = function() {
		if(locationField === null) {
			locationField = jQuery("<input></input>")
				.attr("type", "hidden")
				.attr("name", "l");

			jQuery("#searchPanel form")
				.append(locationField);
		}
		
		if(lastLatitude !== null && lastLongitude !== null) {
			locationField.val(lastLatitude + "," + lastLongitude);
		}
	};

	var turnOffGeolocation = function() {
		if(locationField !== null) { 
			locationField.remove(); 
			locationField = null;
		}

		turnOffButton.text("Is Off");
		turnOnButton.text("Turn On");
	};
	
	var turnOnGeolocation = function() {
		updateLocationField();
		
		turnOffButton.text("Turn Off");
		turnOnButton.text("Is On");
	};
		
	if(navigator.geolocation) {
		addToggleUI();

		navigator.geolocation.getCurrentPosition(function(location) {
			lastLatitude = location.coords.latitude;
			lastLongitude = location.coords.longitude;

			updateLocationField();
		}, removeToggleUI);
		
		turnOnGeolocation();
	}
});

