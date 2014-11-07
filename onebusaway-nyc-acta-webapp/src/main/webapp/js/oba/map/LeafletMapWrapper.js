var OBA = window.OBA || {};

OBA.LeafletMapWrapper = function(mapNode) {

	var lat = OBA.Config.mapCenterLat;
	var lon = OBA.Config.mapCenterLon;
	var zoom = OBA.Config.mapZoom;
	var instance = OBA.Config.mapInstance;

	if (!lat || !lon || !zoom) {
		// These will get overridden by the bundle bounds after the map initially loads.
		lat = 40.639228;
		lon = -74.081154;
		zoom = 11;
	}
	if (instance == 'leaflet-bing'){
		var map = new L.Map('map', {center: new L.LatLng(lat, lon), zoom: zoom });
		var osmLayer = new L.TileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png');
		var bingLayer = new L.BingLayer('Road'); //'Road', 'Aerial', 'AerialWithLabels'
		var mapboxLayer = new L.TileLayer('https://{s}.tiles.mapbox.com/v3/examples.map-i87786ca/{z}/{x}/{y}.png');
		map.addLayer(bingLayer);
		map.addControl(new L.Control.Layers({'Bing':bingLayer, 'Mapbox':mapboxLayer, 'OSM':osmLayer}, {}));
	}
	else if (instance == 'leaflet-google'){
		var map = new L.Map('map', {center: new L.LatLng(lat, lon), zoom: zoom });
		var googleLayer = new L.Google('ROADMAP');
		map.addLayer(googleLayer);
	}
	return map;
};