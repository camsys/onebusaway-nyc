package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;

public interface VehicleAssignsInputConverter {
  List<MtaUtsVehiclePullInPullOut> getVehicleAssignments();
}
