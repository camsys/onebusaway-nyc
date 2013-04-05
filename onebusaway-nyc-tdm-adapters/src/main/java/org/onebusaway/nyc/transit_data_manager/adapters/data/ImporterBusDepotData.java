package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CPTVehicleIden;

public class ImporterBusDepotData implements VehicleDepotData {

  private Date lastUpdated;

  private List<CPTFleetSubsetGroup> depotGroups = null;

  public ImporterBusDepotData(List<CPTFleetSubsetGroup> depotGroups) {
    this.depotGroups = depotGroups;
  }

  /** {@inheritDoc} */
  public List<String> getAllDepotNames() {
    List<String> depotNames = new ArrayList<String>();

    Iterator<CPTFleetSubsetGroup> depGroupsIt = depotGroups.iterator();
    CPTFleetSubsetGroup subGroup = null;

    while (depGroupsIt.hasNext()) {
      subGroup = depGroupsIt.next();
      depotNames.add(getDepotNameStrFromSubsetGroup(subGroup));
    }

    return depotNames;
  }

  /** {@inheritDoc} */
  public List<CPTVehicleIden> getVehiclesByDepotNameStr(String depotNameStr) {
    List<CPTVehicleIden> vehicles = new ArrayList<CPTVehicleIden>();

    // I'll do this in two rounds, first to grab all the CPTFleetSubsetGroups
    // that have our depotNameStr
    // (There should really only be one if my other part was right, but I guess
    // I don't want to count on that!)

    List<CPTFleetSubsetGroup> fleetSubGroupsMatchingDepotStr = getGroupsWithDepotNameStr(depotNameStr);

    // And second to put all the vehicles in each matching CPTFleetSubsetGroup
    // into the same
    // list for output.
    CPTFleetSubsetGroup subGroup = null;

    Iterator<CPTFleetSubsetGroup> matchDGroupsIt = fleetSubGroupsMatchingDepotStr.iterator();
    while (matchDGroupsIt.hasNext()) {
      subGroup = matchDGroupsIt.next();

      vehicles.addAll(subGroup.getGroupMembers().getGroupMember());
    }

    return vehicles;
  }

  public List<CPTFleetSubsetGroup> getGroupsWithDepotNameStr(String depotNameStr) {
    List<CPTFleetSubsetGroup> fleetSubGroupsMatchingDepotStr = new ArrayList<CPTFleetSubsetGroup>();

    Iterator<CPTFleetSubsetGroup> depGroupsIt = depotGroups.iterator();
    while (depGroupsIt.hasNext()) {
      CPTFleetSubsetGroup group = depGroupsIt.next();

      if (getDepotNameStrFromSubsetGroup(group).equals(depotNameStr)) {
        fleetSubGroupsMatchingDepotStr.add(group);
      }
    }

    return fleetSubGroupsMatchingDepotStr;
  }

  public List<CPTFleetSubsetGroup> getAllDepotGroups() {
    return depotGroups;
  }

  /**
   * Grab the depotNameStr from the "official" property of CPTFleetSubsetGroup.
   * 
   * @param subGroup
   * @return
   */
  private String getDepotNameStrFromSubsetGroup(CPTFleetSubsetGroup subGroup) {
    return subGroup.getGroupGarage().getFacilityName();
  }

  public Date getLastUpdatedDate() {
    return this.lastUpdated;
  }
  
  public void setLastUpdatedDate(Date updateDate) {
    this.lastUpdated = updateDate;
  }
}
