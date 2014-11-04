var OBA = window.OBA || {};

OBA.LeafletMapWrapper = function(mapNode) {

	var lat = OBA.Config.mapCenterLat;
	var lon = OBA.Config.mapCenterLon;
	var zoom = OBA.Config.mapZoom;

	if (!lat || !lon || !zoom) {
		// These will get overridden by the bundle bounds after the map initially loads.
		lat = 40.639228;
		lon = -74.081154;
		zoom = 11;
	}

	var map = new L.Map('map', {center: new L.LatLng(lat, lon), zoom: zoom });
	var osm = new L.TileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png');
	var bing = new L.BingLayer('Road'); //'Road', 'Aerial', 'AerialWithLabels'
	map.addLayer(bing);
	map.addControl(new L.Control.Layers({'OSM':osm, "Bing":bing}, {}));
	return map;
};