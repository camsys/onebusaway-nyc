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
 * An empty overlay which is used to retrieve the map projection panes.
 *
 * @constructor
 * @private
 * @param {google.maps.Map} the Google Maps v3
 */
function Fluster2ProjectionOverlay(map)
{
	google.maps.OverlayView.call(this);
	this.setMap(map);
	
	this.getP = function()
	{
		return this.getProjection();
	};
}

Fluster2ProjectionOverlay.prototype = new google.maps.OverlayView();

Fluster2ProjectionOverlay.prototype.draw = function()
{
};