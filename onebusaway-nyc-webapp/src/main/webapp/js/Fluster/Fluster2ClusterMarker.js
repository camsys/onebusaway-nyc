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
 * A cluster marker which shows a background image and the marker count 
 * of the assigned cluster.
 *
 * @constructor
 * @private
 * @param {Fluster2} the Fluster2 itself
 * @param {Fluster2Cluster} the Fluster2Cluster assigned to this marker
 */
function Fluster2ClusterMarker(_fluster, _cluster)
{
	this.fluster = _fluster;
	this.cluster = _cluster;
	this.position = this.cluster.getPosition();
	this.markerCount = this.cluster.getMarkerCount();
	this.map = this.fluster.getMap();
	this.style = null;
	this.div = null;
	
	// Assign style
	var styles = this.fluster.getStyles();
	for(var i in styles)
	{
		if(this.markerCount > i)
		{
			this.style = styles[i];
		}
		else
		{
			break;
		}
	}
	
	// Basics
	google.maps.OverlayView.call(this);
	this.setMap(this.map);
	
	// Draw
	this.draw();
};

Fluster2ClusterMarker.prototype = new google.maps.OverlayView();

Fluster2ClusterMarker.prototype.draw = function()
{
	if(this.div == null)
	{
		var me = this;
		
		// Create div
		this.div = document.createElement('div');
		
		// Set styles
		this.div.style.position = 'absolute';
		this.div.style.width = this.style.width + 'px';
		this.div.style.height = this.style.height + 'px';
		this.div.style.lineHeight = this.style.height + 'px';
		this.div.style.background = 'transparent url("' + this.style.image + '") 50% 50% no-repeat';
		this.div.style.color = this.style.textColor;
		this.div.style.zIndex = 100;
		
		if(this.style.showCount === true) {
			// Marker count
			this.div.style.textAlign = 'center';
			this.div.style.fontFamily = 'Arial, Helvetica';
			this.div.style.fontSize = '11px';
			this.div.style.fontWeight = 'bold';
			this.div.innerHTML = this.markerCount;
		}
		
		// Cursor and onlick
		this.div.style.cursor = 'pointer';
		google.maps.event.addDomListener(this.div, 'click', function() {
			me.map.fitBounds(me.cluster.getMarkerBounds());
		});
		
		this.getPanes().overlayLayer.appendChild(this.div);
	}
	
	// Position
	var position = this.getProjection().fromLatLngToDivPixel(this.position);
	this.div.style.left = (position.x - parseInt(this.style.width / 2)) + 'px';
	this.div.style.top = (position.y - parseInt(this.style.height / 2)) + 'px';
};

Fluster2ClusterMarker.prototype.hide = function()
{
	// Hide div
	this.div.style.display = 'none';
};

Fluster2ClusterMarker.prototype.show = function()
{
	// Show div
	this.div.style.display = 'block';
};