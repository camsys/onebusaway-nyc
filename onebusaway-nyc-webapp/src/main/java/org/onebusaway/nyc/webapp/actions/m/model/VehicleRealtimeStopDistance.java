package org.onebusaway.nyc.webapp.actions.m.model;

import java.util.List;

public class VehicleRealtimeStopDistance {
    private String vehicleId;
    private String distanceAway;
    private Boolean isStroller;
    private Boolean hasRealtime;


    public void setHasRealtime(Boolean hasRealtime) {
        this.hasRealtime = hasRealtime;
    }

    public void setDistanceAway(String distanceAway) {
        this.distanceAway = distanceAway;
    }

    public void setIsStroller(Boolean isStroller) {
        this.isStroller = isStroller;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getDistanceAway() {
        return distanceAway;
    }

    public Boolean getIsStroller() {
        return isStroller;
    }

    public Boolean getHasRealtime() {
        return hasRealtime;
    }
}
