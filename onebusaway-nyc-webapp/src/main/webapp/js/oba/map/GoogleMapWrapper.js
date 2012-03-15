var OBA = window.OBA || {};

OBA.GoogleMapWrapper = function(mapNode) {	
	
	var defaultMapOptions = {
			zoom: 11,
			mapTypeControl: false,
			streetViewControl: false,
			zoomControl: true,
			zoomControlOptions: {
				style: google.maps.ZoomControlStyle.LARGE
			},
			minZoom: 9, 
			maxZoom: 19,
			navigationControlOptions: { style: google.maps.NavigationControlStyle.DEFAULT },
			center: new google.maps.LatLng(40.639228,-74.081154)
	};

	var map = new google.maps.Map(mapNode, defaultMapOptions);

	// CUSTOM SUBWAY TILES
	var mtaSubwayMapType = new google.maps.ImageMapType({
		bounds: new google.maps.LatLngBounds(
					new google.maps.LatLng(40.48801936882241,-74.28397178649902),
					new google.maps.LatLng(40.92862373397717,-73.68182659149171)
				),
		getTileUrl: function(coord, zoom) {
			if(!(zoom >= this.minZoom && zoom <= this.maxZoom)) {
				return null;
			}

			// is the tile we're requesting out of the map's bounds?
			var zoomFactor = Math.pow(2, zoom);
			var center_p = new google.maps.Point((coord.x * 256 + 128) / zoomFactor, (((coord.y + 1) * 256) + 128) / zoomFactor);
		    var center_ll = map.getProjection().fromPointToLatLng(center_p);

		    if(!this.bounds.contains(center_ll)) {
		    	return null;
		    }

		    // if not, calculate the quadtree value and request the graphic
			var quad = "", i;
		    for(i = zoom; i > 0; i--) {
		        var mask = 1 << (i - 1); 
		        var cell = 0; 
		        if ((coord.x & mask) != 0) {
		            cell++; }
		        if ((coord.y & mask) != 0) {
		            cell += 2; }
		        quad += cell; 
		    } 
			return 'http://tripplanner.mta.info/maps/SystemRoutes_New/' + quad + '.png'; 
		},
		tileSize: new google.maps.Size(256, 256),
		opacity: 0.5,
		maxZoom: 15,
		minZoom: 14,
		name: 'MTA Subway Map',
		isPng: true,
		alt: ''
	});

	// SUBWAY TILES TOGGLE BUTTON
	var SubwayTilesControl = function() {
	  var subwayControlContainer = jQuery('<div id="subwayControlContainer"></div>');

	  var subwayControlWrapper = jQuery('<div id="subwayControl"></div>')
	  								.appendTo(subwayControlContainer);
	  
	  var subwayControl = jQuery('<a href="#" title="Click to toggle subway lines">Show Subway</a>')
	  								.appendTo(subwayControlWrapper);
	  
	  subwayControl.click(function(e) { 
		  e.preventDefault();
		  
		  if(map.overlayMapTypes.length === 1) { 
			  map.overlayMapTypes.removeAt(0, mtaSubwayMapType);
			  subwayControl.text("Show Subway");
		  } else {
			  map.overlayMapTypes.insertAt(0, mtaSubwayMapType);
			  subwayControl.text("Hide Subway");

			  // fix for IE
			  subwayControl.css('width', '86');
		  }
	  });

	  var zoomUpdate = function() {
		  if(map.getZoom() < 14 || map.getZoom() > 15) {
			  subwayControlContainer.hide();	
		  } else {
			  subwayControlContainer.show();
		  }
	  };
	  
	  google.maps.event.addListener(map, 'zoom_changed', function() { 
		  zoomUpdate();
	  });

	  google.maps.event.addListener(map, 'idle', function() { 
		  zoomUpdate();
	  });
	 
	  return subwayControlContainer.get(0);
	};
	
	var subwayTilesControl = new SubwayTilesControl();
	map.controls[google.maps.ControlPosition.TOP_RIGHT].push(subwayTilesControl);
	
	// CUSTOM STYLED BASEMAP
	var mutedTransitStylesArray = 
		[{
			featureType: "road.arterial",
			elementType: "geometry",
			stylers: [
			          { saturation: -80 },
			          { lightness: 60 },
			          { visibility: "on" },
			          { hue: "#0011FF" }
			          ]
		},{
			featureType: "road.arterial",
			elementType: "labels",
			stylers: [
			          { saturation: -80 },
			          { lightness: 40 },
			          { visibility: "on" },
			          { hue: "#0011FF" }
			          ]
		},{
			featureType: "road.highway",
			elementType: "geometry",
			stylers: [
			          { saturation: -80 },
			          { lightness: 60 },
			          { visibility: "on" },
			          { hue: "#0011FF" }
			          ]
		},{
			featureType: "road.highway",
			elementType: "labels",
			stylers: [
			          { lightness: 60 },
			          { saturation: -70 },
			          { hue: "#0011FF" },
			          { visibility: "on" }
			          ]
		},{
			featureType: "road.local",
			elementType: "all",
			stylers: [
			          { saturation: -100 },
			          { lightness: 32 }
			          ]
		},{ 
			featureType: "administrative.locality", 
			elementyType: "labels",
			stylers: [ { visibility: "on" }, 
			           { lightness: 50 },
			           { saturation: -80 }, 
			           { hue: "#ffff00" } ] 
		},{ 
			featureType: "administrative.neighborhood", 
			elementyType: "labels",
			stylers: [ { visibility: "on" }, 
			           { lightness: 50 },
			           { saturation: -80 }, 
			           { hue: "#ffffff" } ] 
		},{
			featureType: 'landscape',
			elementType: 'labels',
			stylers: [ {'visibility': 'on'},
			           { lightness: 50 },
			           { saturation: -80 },
			           { hue: "#0099ff" }
			           ]
		},{
			featureType: 'poi',
			elementType: 'labels',
			stylers: [ {'visibility': 'on'},
			           { lightness: 50 },
			           { saturation: -80 },
			           { hue: "#0099ff" }
			           ]
		},{
			featureType: 'water',
			elementType: 'labels',
			stylers: [ {'visibility': 'off'}
			]
		},{
			featureType: 'transit.station.bus',
			elementType: 'labels',
			stylers: [ {'visibility': 'off'}
			]
		}];
	
	var transitStyledMapType = new google.maps.StyledMapType(mutedTransitStylesArray, {name: "Transit"});
	map.mapTypes.set('Transit', transitStyledMapType);
	map.setMapTypeId('Transit');
	
	// RETURN OBJECT BACK TO CALLER
	return map;
};