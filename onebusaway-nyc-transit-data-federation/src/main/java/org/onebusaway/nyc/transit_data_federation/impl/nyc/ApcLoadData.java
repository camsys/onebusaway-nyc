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
package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * Represents additional passenger count record from external system.
 */
public class ApcLoadData {
    private String agencyId;
    private String loadDescription;
    private long timestamp;
    private String vehicle;
    private String vehicleDescription;
    private String estCapacity;
    private String estLoad;

    public String getAgencyId() {
        return agencyId;
    }
    public String getLoadDescription() {
        return loadDescription;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public String getVehicle() {
        return vehicle;
    }
    public AgencyAndId getVehicleAsId() {
        if (agencyId != null && vehicle != null)
            return new AgencyAndId(agencyId, vehicle);
        return null;
    }
    public String getVehicleDescription() {
        return vehicleDescription;
    }
    public String getEstCapcity() {
        return estCapacity;
    }
    public Integer getEstCapacityAsInt() {
        if (estCapacity == null) return null;
        try {
            return Integer.parseInt(estCapacity);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
    public String getEstLoad() {
        return estLoad;
    }
    public Integer getEstLoadAsInt() {
        if (estLoad == null) return null;
        try {
            return Integer.parseInt(estLoad);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
