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
