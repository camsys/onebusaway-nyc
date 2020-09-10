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
