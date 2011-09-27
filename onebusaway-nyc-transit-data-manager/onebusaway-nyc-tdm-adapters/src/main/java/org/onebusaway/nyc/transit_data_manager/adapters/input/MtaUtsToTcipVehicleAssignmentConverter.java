package org.onebusaway.nyc.transit_data_manager.adapters.input;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

import tcip_final_3_0_5_1.CPTOperatorIden;
import tcip_final_3_0_5_1.CPTTransitFacilityIden;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.SCHBlockIden;
import tcip_final_3_0_5_1.SCHPullInOutInfo;

public class MtaUtsToTcipVehicleAssignmentConverter {

  UtsMappingTool mappingTool = null;

  public MtaUtsToTcipVehicleAssignmentConverter() {
    mappingTool = new UtsMappingTool();
  }

  /***
   * Need a class to generate pull out data from the schedule data of an input
   * row.
   * 
   * @param input a MtaUtsVehiclePullInPullOut, which contains both pull in and
   *          out scheduled times.
   * @return a SCHPullInOutInfo representing a pull out, using scheduled data.
   */
  public SCHPullInOutInfo convertToPullOut(MtaUtsVehiclePullInPullOut input) {
    return convertToGivenPullInOut(input, false);
  }

  /***
   * Need a class to generate pull in data from the schedule data of an input
   * row.
   * 
   * @param input a MtaUtsVehiclePullInPullOut, which contains both pull in and
   *          out scheduled times.
   * @return a SCHPullInOutInfo representing a pull in, using scheduled data.
   */
  public SCHPullInOutInfo convertToPullIn(MtaUtsVehiclePullInPullOut input) {
    return convertToGivenPullInOut(input, true);
  }

  /***
   * 
   * @param inputAssignment
   * @return
   */
  private SCHPullInOutInfo convertToGivenPullInOut(
      MtaUtsVehiclePullInPullOut inputAssignment, Boolean isPullIn) {

    SCHPullInOutInfo outputAssignment = new SCHPullInOutInfo();

    Long agencyId = mappingTool.getAgencyIdFromUtsAuthId(inputAssignment.getAuthId());
    DateTime pullInTime = getSchedDateAsCalByType(inputAssignment, true); // Get
                                                                          // the
                                                                          // pull
                                                                          // in
                                                                          // time
                                                                          // and
                                                                          // store
                                                                          // it
                                                                          // as
                                                                          // it's
                                                                          // used
                                                                          // multiple
                                                                          // times.
    DateTime pullOutTime = getSchedDateAsCalByType(inputAssignment, false); // ditto
                                                                            // for
                                                                            // the
                                                                            // pull
                                                                            // out
                                                                            // time.

    // Set vehicle to new CPTVehicleIden
    CPTVehicleIden bus = new CPTVehicleIden();
    bus.setAgencyId(agencyId);
    bus.setVehicleId(inputAssignment.getBusNumber());
    bus.setDesignator(UtsMappingTool.BUS_DESIGNATOR);
    outputAssignment.setVehicle(bus);

    // set time to be the scheduled pull in or pull out time, based on isPullIn
    DateTime adjMoveTime = isPullIn ? pullInTime : pullOutTime;
    DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
    outputAssignment.setTime(fmt.print(adjMoveTime));

    // Set the value of pullIn equal to isPullIn
    outputAssignment.setPullIn(isPullIn);

    // Set the garage to a new CPTTransitFacilityIden representing a depot.
    CPTTransitFacilityIden depot = new CPTTransitFacilityIden();
    depot.setFacilityName(inputAssignment.getDepot());
    depot.setFacilityId(new Long(0));
    outputAssignment.setGarage(depot);

    // Set the date to be the service date.
    outputAssignment.setDate(inputAssignment.getDate());

    // Set the operator to a new CPTOperatorIden, using the agency id, pass
    // number and operator designator.
    CPTOperatorIden op = new CPTOperatorIden();
    op.setOperatorId(inputAssignment.getPassNumber());
    op.setAgencyId(agencyId);
    op.setDesignator(inputAssignment.getOperatorDesignator());
    outputAssignment.setOperator(op);

    // Set the block using a designator of a few items concatenated.
    SCHBlockIden block = new SCHBlockIden();
    block.setBlockId(new Long(0));

    DateTimeFormatter sDateDTF = DateTimeFormat.forPattern("MMddyyyy");
    String serviceDateMMDDYYYY = sDateDTF.print(inputAssignment.getServiceDate());

    DateTimeFormatter poTimeDTF = DateTimeFormat.forPattern("HHmm");
    String pullOutTimeHHMM = poTimeDTF.print(pullOutTime);

    String concatPODateTime = serviceDateMMDDYYYY + "_" + pullOutTimeHHMM;
    String blockDesignator = inputAssignment.getDepot() + "_"
        + inputAssignment.getRoute() + "_"
        + inputAssignment.getRunNumberField() + "_" + concatPODateTime;
    block.setBlockDesignator(blockDesignator);
    outputAssignment.setBlock(block);

    // Set the local info, which holds the run-route.
    tcip_3_0_5_local.SCHPullInOutInfo localInfo = new tcip_3_0_5_local.SCHPullInOutInfo();
    localInfo.setRunRoute(inputAssignment.getRoute());
    outputAssignment.setLocalSCHPullInOutInfo(localInfo);

    return outputAssignment;
  }

  private DateTime getSchedDateAsCalByType(
      MtaUtsVehiclePullInPullOut inputAssign, Boolean isPullIn) {
    DateTime result = null;

    String timeStr = isPullIn ? inputAssign.getSchedPI()
        : inputAssign.getSchedPO();

    result = mappingTool.calculatePullInOutDateFromDateUtsSuffixedTime(
        inputAssign.getServiceDate(), timeStr);

    return result;
  }
}
