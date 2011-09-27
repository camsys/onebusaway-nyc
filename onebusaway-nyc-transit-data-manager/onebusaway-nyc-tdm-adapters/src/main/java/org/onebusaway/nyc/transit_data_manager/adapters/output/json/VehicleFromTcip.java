package org.onebusaway.nyc.transit_data_manager.adapters.output.json;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

import tcip_final_3_0_5_1.CPTVehicleIden;

/**
 * Converts a tcip vehicle object (CPTVehicleIden) to a json vehicle object
 * (Vehicle).
 * 
 * @author sclark
 * 
 */
public class VehicleFromTcip implements
    ModelCounterpartConverter<CPTVehicleIden, Vehicle> {

  UtsMappingTool mappingTool = null;

  public VehicleFromTcip() {
    mappingTool = new UtsMappingTool();
  }

  /** {@inheritDoc} */
  public Vehicle convert(CPTVehicleIden input) {
    Vehicle outputVehicle = new Vehicle();

    outputVehicle.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(input.getAgencyId()));
    outputVehicle.setVehicleId(String.valueOf(input.getVehicleId()));

    return outputVehicle;
  }

}
