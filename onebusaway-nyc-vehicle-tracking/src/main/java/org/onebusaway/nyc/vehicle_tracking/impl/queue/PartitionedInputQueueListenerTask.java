package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

public class PartitionedInputQueueListenerTask extends InputQueueListenerTask {

  private String _depotPartitionKey = null;
  @Autowired
  private VehicleLocationInferenceService _vehicleLocationService;
  @Autowired
  private VehicleAssignmentService _vehicleAssignmentService;

  @Override
  public void processMessage(String address, String contents) {
    CcLocationReport message = deserializeMessage(contents);
    if (acceptMessage(message)) {
      _vehicleLocationService.handleCcLocationReportRecord(message);
    }
  }

  private boolean acceptMessage(CcLocationReport message) {
    ArrayList<AgencyAndId> vehicleList = _vehicleAssignmentService.getAssignedVehicleIdsForDepot(_depotPartitionKey);

    CPTVehicleIden vehicleIdent = message.getVehicle();
    AgencyAndId vehicleId = new AgencyAndId(vehicleIdent.getAgencydesignator(),
        vehicleIdent.getVehicleId() + "");

    return vehicleList.contains(vehicleId);
  }

  public String getDepotPartitionKey() {
    return _depotPartitionKey;
  }

  public void setDepotPartitionKey(String depotPartitionKey) {
    _depotPartitionKey = depotPartitionKey;
  }

}
