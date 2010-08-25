function AppState(map) {
	this.map = map;
	this.markers = new Array();
}

AppState.prototype.refreshVehicleData = function() {

	for(var x=0;x<this.markers.length;x++) {
		var marker = this.markers[x];
		marker.setMap(null);
	}
	this.markers = new Array();
	
	var href = window.location.href;
	var re = /taskId=(\d+)/;
	var m = re.exec(href);
	if (m == null)
		return;
	var url = 'vehicle-location-simulation!task-data.do?taskId=' + m[1];
	var t = this;
	$.getJSON(url,function(data) {
		t.handleTaskData(data);
	});
};

AppState.prototype.step = function() {

	var href = window.location.href;
	var re = /taskId=(\d+)/;
	var m = re.exec(href);
	if (m == null)
		return;
	var url = 'vehicle-location-simulation!step.do?taskId=' + m[1];
	var t = this;
	$.getJSON(url);
};

AppState.prototype.handleTaskData = function(data) {
	var summary = data.summary;
	var mostRecentRecord = summary.mostRecentRecord;
	var lat = mostRecentRecord.lat;
	var lon = mostRecentRecord.lon;
	var myLatlng = new google.maps.LatLng(lat, lon);

	var marker = new google.maps.Marker( {
		position : myLatlng,
		map : this.map,
		title : "Observation"
	});
	
	this.markers.push(marker);
	
	var vehicleId = mostRecentRecord.vehicleId;
	var url = 'vehicle-location.do?vehicleId=' + vehicleId;
	var t = this;
	$.getJSON(url, function(data) {
		t.handleVehicleData(data);
	});
};

AppState.prototype.handleVehicleData = function(data, map) {
	var record = data.record;
	var lat = record.currentLocationLat;
	var lon = record.currentLocationLon;

	var myLatlng = new google.maps.LatLng(lat, lon);

	var marker = new google.maps.Marker( {
		position : myLatlng,
		map : this.map,
		title : "Prediction",
		icon : 'Bus.png'
	});
	
	this.markers.push(marker);
};

function ButtonControl(controlDiv, title, callback) {

	// Set CSS styles for the DIV containing the control
	// Setting padding to 5 px will offset the control
	// from the edge of the map
	controlDiv.style.padding = '5px';

	// Set CSS for the control border
	var controlUI = document.createElement('DIV');
	controlUI.style.backgroundColor = 'white';
	controlUI.style.borderStyle = 'solid';
	controlUI.style.borderWidth = '2px';
	controlUI.style.cursor = 'pointer';
	controlUI.style.textAlign = 'center';
	controlUI.title = 'Click to refresh the vehicle data';
	controlDiv.appendChild(controlUI);

	// Set CSS for the control interior
	var controlText = document.createElement('DIV');
	controlText.style.fontFamily = 'Arial,sans-serif';
	controlText.style.fontSize = '12px';
	controlText.style.paddingLeft = '4px';
	controlText.style.paddingRight = '4px';
	controlText.innerHTML = title;
	controlUI.appendChild(controlText);

	// Setup the click event listeners: simply set the map to Chicago
	google.maps.event.addDomListener(controlUI, 'click', callback);
}

function initialize() {

	var latlng = new google.maps.LatLng(40.67256636389564, -73.91841888427734);

	var myOptions = {
		zoom : 12,
		center : latlng,
		mapTypeId : google.maps.MapTypeId.ROADMAP
	};
	var map = new google.maps.Map(document.getElementById("map_canvas"),
			myOptions);
	
	var state = new AppState(map);

	var refreshControlDiv = document.createElement('DIV');
	var refreshControl = new ButtonControl(refreshControlDiv, 'Refresh', function() {
		state.refreshVehicleData();
	});

	refreshControlDiv.index = 1;
	map.controls[google.maps.ControlPosition.TOP_RIGHT].push(refreshControlDiv);
	
	var stepControlDiv = document.createElement('DIV');
	var stepControl = new ButtonControl(stepControlDiv, 'Step', function() {
		state.step();
		state.refreshVehicleData();		
	});

	stepControlDiv.index = 2;
	map.controls[google.maps.ControlPosition.TOP_RIGHT].push(stepControlDiv);
	
	state.refreshVehicleData();
}



