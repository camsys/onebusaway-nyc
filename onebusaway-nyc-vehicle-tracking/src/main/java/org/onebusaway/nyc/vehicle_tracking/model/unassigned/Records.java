package org.onebusaway.nyc.vehicle_tracking.model.unassigned;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

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
