// API key for http://openlayers.org. Please get your own at
// http://bingmapsportal.com/ and use that instead.
var apiKey = "AqTGBsziZHIJYYxgivLBf0hVdrAk9mWO5cQcb8Yux8sW5M8c8opEC2lZqKR1ZZXf";
    
var map;
var filter;
var filterStrategy;
var feature_layers = new Array();

//
//
//
function selectShape(shapeId) {
	filter.value = shapeId;
	filterStrategy.setFilter(filter);	
}
//
//Loads a GeoJSON layer using an AJAX callback
//
function addMapLayerFromGeoJSON(url, layer, fromProjection, toProjection) {

	OpenLayers.Request.GET({
		url: url,
		headers: {'Accept':'application/json'},
		success: function(req) {
			var g = new OpenLayers.Format.GeoJSON({
				'externalProjection' : fromProjection,
				'internalProjection' : toProjection					
			});
			var feature_collection = g.read(req.responseText);
			layer.destroyFeatures();
			layer.addFeatures(feature_collection);
		}
	});
}
function getLastXY() {
	var control = map.getControlsByClass("OpenLayers.Control.MousePosition")[0];
	var xy = control.lastXy;
	return xy;
}
function getPopupText(feature) {
	var html = [];
	html.push("<div class='popup'>");
	for (var property in feature.attributes) {
		html.push("<div class='popup_attribute'>");
		html.push(property);
		html.push(" = ");
		html.push(feature.attributes[property]);
		html.push("</div");
		html.push("<br/>");
	}
	html.push("</div");
	return html.join("");
}
//
// Activate popups for feature selection
//
function activatePopups(layer) {
	
	layer.events.on({
        "featureselected": function(e) {
        	var feature = e.feature;
        	var xy = getLastXY();
        	//alert('Selected feature ' + feature + ' at ' + map.getLonLatFromPixel(xy));
        	var popup = new OpenLayers.Popup.FramedCloud(
        			"popup",
        			map.getLonLatFromPixel(xy),
        			new OpenLayers.Size(300,200),
        			getPopupText(feature),
        			null,
        			true,
        			null
        			);
        	feature.popup = popup;
        	map.addPopup(popup);
        },
        "featureunselected": function(e) {
        	feature = e.feature;
        	if (feature.popup != null) {
        		map.removePopup(feature.popup);
        		feature.popup.destroy();
        		feature.popup = null;
        	}
        }
    });
	
}
//
//
//
function createMap(divname, mapcenter_x, mapcenter_y) {

	filter = new OpenLayers.Filter.Comparison({
        type: OpenLayers.Filter.Comparison.LIKE,
        property: "SHAPE_ID",
        value: "*"
    });

    filterStrategy = new OpenLayers.Strategy.Filter({filter: filter});
	
	map = new OpenLayers.Map(divname, {
	    controls: [
	        new OpenLayers.Control.Attribution(),
	        new OpenLayers.Control.Navigation(),
	        new OpenLayers.Control.PanZoomBar(),
	        new OpenLayers.Control.LayerSwitcher(),
	        new OpenLayers.Control.MousePosition()	    
	        ]
	});
		
	// Use the Bing map service for roads
	var road = new OpenLayers.Layer.Bing({
		key: apiKey, 
		type: "Road"
		});
	map.addLayers([road]);

	// Define some basic styles
	//var yellow_line = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults({strokeColor: 'yellow', strokeWidth: 4}, OpenLayers.Feature.Vector.style['default']));
	//var green_line = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults({strokeColor: 'green', strokeWidth: 4}, OpenLayers.Feature.Vector.style['default']));
	var black_point = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults({strokeColor: 'black', fillColor: 'black', pointRadius: 6}, OpenLayers.Feature.Vector.style['default']));
	
	var green_line = new OpenLayers.StyleMap({
        "default": new OpenLayers.Style(OpenLayers.Util.applyDefaults({
        	strokeColor: 'green', 
        	strokeWidth: 4
        }, OpenLayers.Feature.Vector.style["default"])),
        "select": new OpenLayers.Style(OpenLayers.Util.applyDefaults({
        	strokeColor: 'yellow', 
        	strokeWidth: 4
        }, OpenLayers.Feature.Vector.style["select"]))
	});
	var blue_line = new OpenLayers.StyleMap({
        "default": new OpenLayers.Style(OpenLayers.Util.applyDefaults({
        	strokeColor: 'blue', 
        	strokeWidth: 4
        }, OpenLayers.Feature.Vector.style["default"])),
        "select": new OpenLayers.Style(OpenLayers.Util.applyDefaults({
        	strokeColor: 'yellow', 
        	strokeWidth: 4
        }, OpenLayers.Feature.Vector.style["select"]))
	});
	
	// Add the GeoJson shapes in
	var shapes_layer = new OpenLayers.Layer.Vector('Raw Shapes', {styleMap: green_line});	
	var nodes_layer = new OpenLayers.Layer.Vector('Raw Nodes', {styleMap: black_point});
	var final_shapes_layer = new OpenLayers.Layer.Vector('Final Shapes', {styleMap: blue_line});	
	var final_nodes_layer = new OpenLayers.Layer.Vector('Final Nodes', {styleMap: black_point});

	filterStrategy.setLayer(final_shapes_layer);
	
	map.addLayer(shapes_layer);
	map.addLayer(nodes_layer);
	map.addLayer(final_shapes_layer);
	map.addLayer(final_nodes_layer);
	
	feature_layers.push(shapes_layer);
	feature_layers.push(final_shapes_layer);

	var geoProjection = new OpenLayers.Projection("EPSG:4326");
	var mapProjection = map.getProjectionObject();

	var selectControl = new OpenLayers.Control.SelectFeature(
            [shapes_layer, final_shapes_layer, final_nodes_layer],
            {
                //clickout: true, toggle: false,
                //multiple: false, hover: false,
                //toggleKey: "ctrlKey", // ctrl key removes from selection
                //multipleKey: "shiftKey" // shift key adds to selection
            }
        );
        
    map.addControl(selectControl);
    selectControl.activate();
    
	// Center the map on Manhattan
	var map_center = new OpenLayers.LonLat(mapcenter_y, mapcenter_x);
	map_center.transform(geoProjection, mapProjection);
	map.setCenter(map_center, 14);
	
	// Load the raw shapes using call backs
	addMapLayerFromGeoJSON('raw-shapes.do', shapes_layer, geoProjection, mapProjection);
	addMapLayerFromGeoJSON('raw-nodes.do', nodes_layer, geoProjection, mapProjection);
	addMapLayerFromGeoJSON('final-shapes.do', final_shapes_layer, geoProjection, mapProjection);
	addMapLayerFromGeoJSON('final-nodes.do', final_nodes_layer, geoProjection, mapProjection);

	// Activate popups for each layer
	activatePopups(shapes_layer);
	activatePopups(final_shapes_layer);
	activatePopups(final_nodes_layer);
	
}

