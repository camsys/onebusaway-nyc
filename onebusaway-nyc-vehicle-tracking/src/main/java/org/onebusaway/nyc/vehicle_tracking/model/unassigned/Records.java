package org.onebusaway.nyc.vehicle_tracking.model.unassigned;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Records {
    @JsonProperty("records")
    UnassignedVehicleRecord[] unassignedVehicleRecords;

    public UnassignedVehicleRecord[] getUnassignedVehicleRecords() {
        return unassignedVehicleRecords;
    }

    public void setUnassignedVehicleRecords(UnassignedVehicleRecord[] unassignedVehicleRecords) {
        this.unassignedVehicleRecords = unassignedVehicleRecords;
    }
}
