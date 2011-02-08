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
	this.map = this.fluster.getMap();

	this.setMap(this.map);
	this.setPosition(this.position);
	this.setIcon(new google.maps.MarkerImage(OBA.Config.stopIconFilePrefix + "." + OBA.Config.stopIconFileType,
			new google.maps.Size(OBA.Config.stopIconSize, OBA.Config.stopIconSize),
			new google.maps.Point(0,0),
			new google.maps.Point(OBA.Config.stopIconCenter, OBA.Config.stopIconCenter)));

	google.maps.event.addDomListener(this, 'click', function() {
		this.map.setCenter(this.position);
		this.map.setZoom(16);
	});
}

Fluster2ClusterMarker.prototype = new google.maps.Marker({ zIndex:100 });

Fluster2ClusterMarker.prototype.hide = function()
{
	this.setVisible(false);
};

Fluster2ClusterMarker.prototype.show = function()
{
	this.setVisible(true);
};