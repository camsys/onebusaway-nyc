/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
