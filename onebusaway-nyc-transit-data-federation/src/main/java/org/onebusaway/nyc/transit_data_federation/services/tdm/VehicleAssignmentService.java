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

package org.onebusaway.nyc.transit_data_federation.services.tdm;

import java.util.ArrayList;

import org.onebusaway.gtfs.model.AgencyAndId;

/**
 * Service interface for getting which vehicles are assigned to a depot and vice-versa.
 * 
 * @author jmaki
 * @author dhaskin
 */
public interface VehicleAssignmentService {

  /**
   * Get a list of vehicles assigned to the named depot.
   * 
   * @param depotIdentifier The depot identifier to request vehicles for.
   */
  public ArrayList<AgencyAndId> getAssignedVehicleIdsForDepot(String depotIdentifier)
    throws Exception;

  /**
   * Get depot for the given vehicleId, assuming the depot has been previously requested
   * with getAssignedVehicleIdsForDepot().
   * 
   * @param vehicleId The vehicle identifier to request depot for.
   */
  public String getAssignedDepotForVehicleId(AgencyAndId vehicle);

}
