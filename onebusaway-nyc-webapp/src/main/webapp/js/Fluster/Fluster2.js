/*
 * Fluster2 0.1.1
 * Copyright (C) 2009 Fusonic GmbH
 *
 * This file is part of Fluster2.
 *
 * Fluster2 is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * Fluster2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Creates a new Fluster to manage masses of markers in a Google Maps v3.
 *
 * @constructor
 * @param {google.maps.Map} the Google Map v3
 * @param {bool} run in debug mode or not
 */
function Fluster2(_map, _debug, stopMarkers)
{
	// Private variables
	var map = _map;
	var projection = new Fluster2ProjectionOverlay(map);
	var clusters = new Object();
	var me = this;
	
	// Properties
	this.gridSize = 40;
	this.maxZoom = 15;
	this.currentZoomLevel = _map.getZoom();			

	// Timeouts
	var zoomChangedTimeout = null;
	
	/**
	 * Create clusters for the current zoom level and assign markers.
	 */
	function createClusters(zoomChange)
	{
		var zoom = map.getZoom();
		
		if(clusters[me.currentZoomLevel] && zoomChange)
		{
			for(var i = 0; i < clusters[me.currentZoomLevel].length; i++)
			{
				clusters[me.currentZoomLevel][i].hide();
			}
		}
		
		if(! clusters[zoom]) {
			clusters[zoom] = new Array();
		
			for(id in stopMarkers) {
				var done = false;
				var marker = stopMarkers[id].getRawMarker();
				var mapBounds = map.getBounds();
	
				// Find a cluster which contains the marker
				for(var j = clusters[zoom].length - 1; j >= 0; j--)
				{
					var cluster = clusters[zoom][j];
	
					if(cluster.contains(marker.getPosition()))
					{
						cluster.addMarker(marker);
						done = true;
						break;
					}
				}
				
				// No cluster found, create a new one
				if(!done)
				{
					var cluster = new Fluster2Cluster(me, marker);
					cluster.show();
					
					clusters[zoom].push(cluster);
				}
			} // for stopMarkers
		}
		
		me.currentZoomLevel = _map.getZoom();	
		
		showClustersInBounds();
	};
	
	/**
	 * Displays all clusters inside the current map bounds.
	 */
	function showClustersInBounds()
	{
		if(! clusters[me.currentZoomLevel])
			return;
		
		var mapBounds = map.getBounds();
			
		for(var i = 0; i < clusters[me.currentZoomLevel].length; i++)
		{
			var cluster = clusters[me.currentZoomLevel][i];
	
			if(mapBounds.contains(cluster.getPosition()))
			{
				if(map.getZoom() > me.maxZoom) {
					cluster.expand();
					cluster.hide();
				} else {				
					cluster.show();
				}
			} else {
				cluster.hide();
			}
		}
	}
	
	/**
	 * Callback which is executed 500ms after the map's zoom level has changed.
	 */
	this.zoomChanged = function()
	{
		window.clearInterval(zoomChangedTimeout);
		
		zoomChangedTimeout = window.setTimeout(function() { createClusters(true); }, 500);
	};
	
	/**
	 * Returns the map assigned to this Fluster.
	 */
	this.getMap = function()
	{
		return map;
	};
	
	/**
	 * Returns the map projection.
	 */
	this.getProjection = function()
	{
		return projection.getP();
	};

	/**
	 * Adds a marker to the Fluster.
	 */
	this.addMarker = function(_marker)
	{
		if(clusters[me.currentZoomLevel]) {
			var done = false;
			var marker = _marker;
			var mapBounds = map.getBounds();

			// Find a cluster which contains the marker
			for(var j = clusters[me.currentZoomLevel].length - 1; j >= 0; j--)
			{
				var cluster = clusters[me.currentZoomLevel][j];

				if(cluster.contains(marker.getPosition()))
				{
					cluster.addMarker(marker);
					done = true;
					break;
				}
			}
			
			// No cluster found, create a new one
			if(!done)
			{
				var cluster = new Fluster2Cluster(me, marker);
				clusters[me.currentZoomLevel].push(cluster);
			}			
		}		
	};
	
	this.refresh = function() {
		showClustersInBounds();
	};
	
	/**
	 * Sets map event handlers and setup's the markers for the current
	 * map state.
	 */
	this.initialize = function()
	{		
		google.maps.event.addListener(map, 'zoom_changed', this.zoomChanged); // calls createClusters()

		// Setup markers for the current state
		window.setTimeout(function() { createClusters(false); }, 1000);	
	};
}