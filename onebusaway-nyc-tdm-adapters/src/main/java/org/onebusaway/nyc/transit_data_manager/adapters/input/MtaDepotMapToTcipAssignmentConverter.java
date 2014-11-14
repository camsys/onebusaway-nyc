package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_4_0_0_0.CPTFleetSubsetGroup;
import tcip_final_4_0_0_0.CPTFleetSubsetGroup.GroupMembers;
import tcip_final_4_0_0_0.CPTTransitFacilityIden;
import tcip_final_4_0_0_0.CPTVehicleIden;

public class MtaDepotMapToTcipAssignmentConverter {

  private static String DATASOURCE_SYSTEM = "SPEAR";
  
  public MtaDepotMapToTcipAssignmentConverter() {
  }
  
  private DepotIdTranslator depotIdTranslator = null;

  /***
   * 
   * @param list
   * @return
   */
  public CPTFleetSubsetGroup ConvertToOutput(String mtaSourceDepotIdStr,
      List<MtaBusDepotAssignment> sourceMtaBusDepotAssigns) {

    CPTFleetSubsetGroup outputGroup = new CPTFleetSubsetGroup();

    outputGroup.setGroupId(new Long(0));
    outputGroup.setGroupName(getMappedId(mtaSourceDepotIdStr));

    // Add the group-garage block
    CPTTransitFacilityIden depotFacility = new CPTTransitFacilityIden();
    depotFacility.setFacilityId(new Long(0));
    depotFacility.setFacilityName(getMappedId(mtaSourceDepotIdStr));
    outputGroup.setGroupGarage(depotFacility);

    outputGroup.setGroupMembers(generateGroupMembers(sourceMtaBusDepotAssigns));

    return outputGroup;
  }

  private GroupMembers generateGroupMembers(
      List<MtaBusDepotAssignment> mtaBDAssigns) {
    GroupMembers gMembers = new GroupMembers();

    for (MtaBusDepotAssignment mtaSourceDepotAssignment : mtaBDAssigns) {
      gMembers.getGroupMember().add(makeTcipVehicleFromMtaAssignment(mtaSourceDepotAssignment));
    }

    return gMembers;
  }
  
  private CPTVehicleIden makeTcipVehicleFromMtaAssignment (MtaBusDepotAssignment mtaAssignment) {
    CPTVehicleIden vehicle = new CPTVehicleIden();
    
    vehicle.setAgencyId(mtaAssignment.getAgencyId());
    vehicle.setVehicleId(mtaAssignment.getBusNumber());
    
    return vehicle;
  }
  
  private String getMappedId(String fromId) {
    if (depotIdTranslator != null) {
      return depotIdTranslator.getMappedId(DATASOURCE_SYSTEM, fromId);
    } else {
      return fromId;
    }
  }
  
  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator; 
  }
}
