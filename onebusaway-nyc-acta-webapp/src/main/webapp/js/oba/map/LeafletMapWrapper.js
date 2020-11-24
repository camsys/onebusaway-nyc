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
	
	options = {
		minZoom: 9,
		maxZoom: 19,
		tileSize: 256,
		subdomains: 'abc',
		errorTileUrl: '',
		attribution: '',
		opacity: 1,
		continuousWorld: false,
		noWrap: false
	};
	
	if (instance == 'leaflet-bing'){
		var map = new L.Map('map', {center: new L.LatLng(lat, lon), zoom: zoom });
		var bingLayer = new L.BingLayer('Road', options);
		var mapboxLayer = new L.TileLayer(document.location.protocol + '//{s}.tiles.mapbox.com/v3/examples.map-i87786ca/{z}/{x}/{y}.png', options);
		var osmLayer = new L.TileLayer(document.location.protocol + '//{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', options);
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