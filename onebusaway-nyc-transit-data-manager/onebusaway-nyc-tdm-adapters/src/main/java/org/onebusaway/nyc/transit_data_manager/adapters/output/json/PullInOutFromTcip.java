package org.onebusaway.nyc.transit_data_manager.adapters.output.json;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.PullInOut;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.SCHPullInOutInfo;

public class PullInOutFromTcip implements
    ModelCounterpartConverter<SCHPullInOutInfo, PullInOut> {

  UtsMappingTool mappingTool = null;
  ModelCounterpartConverter<CPTVehicleIden, Vehicle> vehConv = null;

  public PullInOutFromTcip() {
    mappingTool = new UtsMappingTool();
    vehConv = new VehicleFromTcip();
  }

  public PullInOut convert(SCHPullInOutInfo input) {
    PullInOut movement = new PullInOut();

    movement.setPullIn(input.isPullIn());
    movement.setDepotName(input.getGarage().getFacilityName());

    movement.setVehicle(vehConv.convert(input.getVehicle()));

    movement.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(input.getOperator().getAgencyId()));
    movement.setPassId(String.valueOf(input.getOperator().getOperatorId()));

    // Set the service Date.
    DateTimeFormatter xmlDateDTF = ISODateTimeFormat.date();
    DateTime serviceDate = xmlDateDTF.parseDateTime(input.getDate());

    DateTimeFormatter shortDateDTF = ISODateTimeFormat.date();
    movement.setServiceDate(shortDateDTF.print(serviceDate));

    // set the action date - the time the thing supposedly actually happened.
    DateTimeFormatter xmlDTF = ISODateTimeFormat.dateTimeNoMillis();
    DateTime actionTime = xmlDTF.parseDateTime(input.getTime());

    movement.setTime(xmlDTF.print(actionTime));

    movement.setBlockDesignator(input.getBlock().getBlockDesignator());

    movement.setRunNumber(mappingTool.parseRunNumFromBlockDesignator(input.getBlock().getBlockDesignator()));

    movement.setRunRoute(input.getLocalSCHPullInOutInfo().getRunRoute());

    return movement;
  }

}
