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

package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

import java.util.List;

/**
 * A model object representing a single depot and all of the buses assigned to
 * it. For use with Gson to make Json.
 * 
 * @author sclark
 * 
 */
public class DepotVehicleAssignments {
  public DepotVehicleAssignments() {

  }

  private Depot depot;
  private List<Vehicle> vehicles;

  public void setDepot(Depot depot) {
    this.depot = depot;
  }

  public void setVehicles(List<Vehicle> vehicles) {
    this.vehicles = vehicles;
  }

}
