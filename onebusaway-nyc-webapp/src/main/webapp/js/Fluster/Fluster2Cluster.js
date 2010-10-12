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
 * Cluster which holds one or more markers of the map.
 *
 * @constructor
 * @private
 * @param {Fluster2} the Fluster2 itself
 * @param {google.maps.Marker} the first marker
 */
function Fluster2Cluster(_fluster, _marker)
{	
	// Get properties from marker
	var markerPosition = _marker.getPosition();
	
	// Properties
	this.fluster = _fluster;
	this.markers = [];
	this.bounds = null;
	this.marker = null;
	this.lngSum = 0;
	this.latSum = 0;
	this.center = markerPosition;
	this.map = this.fluster.getMap();
	
	var me = this;
	
	// Get properties from fluster
	var projection = _fluster.getProjection();
	var gridSize = _fluster.gridSize;
	
	// Calculate bounds
	var position = projection.fromLatLngToDivPixel(markerPosition);
	var positionSW = new google.maps.Point(
		position.x - gridSize,
		position.y + gridSize
	);
	var positionNE = new google.maps.Point(
		position.x + gridSize,
		position.y - gridSize
	);
	this.bounds = new google.maps.LatLngBounds(
		projection.fromDivPixelToLatLng(positionSW),
		projection.fromDivPixelToLatLng(positionNE)
	);
	
	/**
	 * Adds a marker to the cluster.
	 */
	this.addMarker = function(_marker)
	{
		this.markers.push(_marker);
	};

	this.expand = function() {
		for(var i = 0; i < this.markers.length; i++)
		{
			this.markers[i].setMap(me.map);
		}
	}
	
	/**
	 * Shows either the only marker or a cluster marker instead.
	 */
	this.show = function()
	{
		// Show marker if there is only 1
		if(this.markers.length == 1)
		{
			this.markers[0].setMap(me.map);
		}
		else if(this.markers.length > 1)
		{
			// Hide all markers
			for(var i = 0; i < this.markers.length; i++)
			{
				this.markers[i].setMap(null);
			}
			
			// Create marker
			if(this.marker == null)
			{
				this.marker = new Fluster2ClusterMarker(this.fluster, this);
				
				if(this.fluster.debugEnabled)
				{
					google.maps.event.addListener(this.marker, 'mouseover', me.debugShowMarkers);
					google.maps.event.addListener(this.marker, 'mouseout', me.debugHideMarkers);
				}
			}

			// Show marker
			this.marker.show();
		}
	};
	
	/**
	 * Hides the cluster
	 */
	this.hide = function()
	{
		if(this.marker != null)
		{
			this.marker.hide();
		}
	};
	
	/**
	 * Shows all markers included by this cluster (debugging only).
	 */
	this.debugShowMarkers = function()
	{
		for(var i = 0; i < me.markers.length; i++)
		{
			me.markers[i].setVisible(true);
		}
	};
	
	/**
	 * Hides all markers included by this cluster (debugging only).
	 */
	this.debugHideMarkers = function()
	{
		for(var i = 0; i < me.markers.length; i++)
		{
			me.markers[i].setVisible(false);
		}
	};
	
	/**
	 * Returns the number of markers in this cluster.
	 */
	this.getMarkerCount = function()
	{
		return this.markers.length;
	};
	
	/**
	 * Checks if the cluster bounds contains the given position.
	 */
	this.contains = function(_position)
	{
		return me.bounds.contains(_position);
	};
	
	/**
	 * Returns the central point of this cluster's bounds.
	 */
	this.getPosition = function()
	{
		return this.center;
	};

	/**
	 * Returns this cluster's bounds.
	 */
	this.getBounds = function()
	{
		return this.bounds;
	};

	/**
	 * Return the bounds calculated on the markers in this cluster.
	 */
	this.getMarkerBounds = function()
	{
		var bounds = new google.maps.LatLngBounds(
			me.markers[0].getPosition(),
			me.markers[0].getPosition()
		);
		for(var i = 1; i < me.markers.length; i++)
		{
			bounds.extend(me.markers[i].getPosition());
		}
		return bounds;
	};
	
	// Add the first marker
	this.addMarker(_marker);
}