package org.onebusaway.nyc.webapp.actions.m.model;

import java.util.List;

public class VehicleRealtimeStopDistance {
    private String vehicleId;
    private String distanceAway;
    private String isKneeling;
    private Boolean hasRealtime;


    public void setHasRealtime(Boolean hasRealtime) {
        this.hasRealtime = hasRealtime;
    }

    public void setDistanceAway(String distanceAway) {
        this.distanceAway = distanceAway;
    }

    public void setIsKneeling(String isKneeling) {
        this.isKneeling = isKneeling;
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

    public String getIsKneeling() {
        return isKneeling;
    }

    public Boolean getHasRealtime() {
        return hasRealtime;
    }
}
