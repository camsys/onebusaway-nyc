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

package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.Date;
import java.util.List;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CPTVehicleIden;

public interface VehicleDepotData {
  /**
   * Gets the entire set of DepotNames from the data.
   * 
   * This is built for MTA depot names initially, as they name depots with names
   * like 'YUK4'.
   * 
   * @return A list of strings like 'YUK4', one for each depot in the data.
   */
  List<String> getAllDepotNames();

  /**
   * Get a list of vehicles (in tcip format of course) associated with a certain
   * depot.
   * 
   * @param depotNameStr An MTA style depot name, such as 'YUK4' to return
   *          vehicles for.
   * @return A list of CPTVehicleIden, each corresponding to a vehicle
   *         associated with the depot param.
   */
  List<CPTVehicleIden> getVehiclesByDepotNameStr(String depotNameStr);
  
  /**
   * Get a list of all the fleetsubsetgroups matching a depotNameStr
   * @param depotNameStr the depot name string to match
   * @return a list of CPTFleetSubsetGroup
   */
  List<CPTFleetSubsetGroup> getGroupsWithDepotNameStr(String depotNameStr);
  
  /**
   * Get a list of all fleet subset groups
   * @return A list of all depot groups
   */
  List<CPTFleetSubsetGroup> getAllDepotGroups();
  
  /**
   * When the underlying data source was last updated
   */
  Date getLastUpdatedDate();

  /**
   * When the underlying data source was last updated
   */
  void setLastUpdatedDate(Date date);
}
