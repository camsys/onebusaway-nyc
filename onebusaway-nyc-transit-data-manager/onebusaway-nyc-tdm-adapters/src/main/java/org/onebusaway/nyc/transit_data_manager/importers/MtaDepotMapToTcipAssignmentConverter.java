package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.Iterator;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.importers.tools.UtsMappingTool;
import org.onebusaway.nyc.transit_data_manager.model.MtaBusDepotAssignment;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CPTFleetSubsetGroup.GroupMembers;
import tcip_final_3_0_5_1.CPTTransitFacilityIden;

public class MtaDepotMapToTcipAssignmentConverter {

  UtsMappingTool mappingTool = null;

  public MtaDepotMapToTcipAssignmentConverter() {
  }

  /***
   * 
   * @param list
   * @return
   */
  public CPTFleetSubsetGroup ConvertToOutput(String depotStr,
      List<MtaBusDepotAssignment> list) {

    CPTFleetSubsetGroup outputGroup = new CPTFleetSubsetGroup();

    outputGroup.setGroupId(new Long(0));

    outputGroup.setGroupName(depotStr);

    CPTTransitFacilityIden depotFacility = new CPTTransitFacilityIden();
    depotFacility.setFacilityId(new Long(0));
    depotFacility.setFacilityName(depotStr);
    outputGroup.setGroupGarage(depotFacility);

    outputGroup.setGroupMembers(generateGroupMembers(list));

    return outputGroup;
  }

  private GroupMembers generateGroupMembers(
      List<MtaBusDepotAssignment> bdAssigns) {
    GroupMembers gMembers = new GroupMembers();

    Iterator<MtaBusDepotAssignment> bdIt = bdAssigns.iterator();

    CPTVehicleIden bus = null;
    MtaBusDepotAssignment bdAssignment = null;

    while (bdIt.hasNext()) {
      bdAssignment = bdIt.next();
      bus = new CPTVehicleIden();

      bus.setAgencyId(bdAssignment.getAgencyId());
      bus.setVehicleId(bdAssignment.getBusNumber());
      bus.setName("Mercedes Benz");

      gMembers.getGroupMember().add(bus);
    }

    return gMembers;
  }
}
