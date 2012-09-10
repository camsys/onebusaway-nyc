package org.onebusaway.nyc.transit_data_manager.adapters.data;

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
}
