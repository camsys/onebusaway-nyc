package org.onebusaway.nyc.presentation.model;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.geospatial.model.EncodedPolylineBean;

public class StopRouteDirectionGroup {
	private String direction;
	
	private List<StopRouteDirection> stopRouteDirections = new ArrayList<StopRouteDirection>();
	
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public List<StopRouteDirection> getStopRouteDirections() {
		return stopRouteDirections;
	}
	public void setStopRouteDirections(List<StopRouteDirection> stopRouteDirections) {
		this.stopRouteDirections = stopRouteDirections;
	}

}
