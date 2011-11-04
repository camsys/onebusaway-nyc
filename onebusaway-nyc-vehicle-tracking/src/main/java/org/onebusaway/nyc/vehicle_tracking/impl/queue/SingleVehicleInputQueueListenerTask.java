package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

import org.springframework.beans.factory.annotation.Autowired;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class SingleVehicleInputQueueListenerTask
  extends InputQueueListenerTask 
  implements PartitionedInputQueueListener {

  private String _vehicleId = "MTA NYCT_2827";

  private VehicleLocationInferenceService _vehicleLocationService;
  
  @Autowired
  public void setVehicleLocationService(VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Override
  public boolean processMessage(String address, String contents) {
    CcLocationReport message = deserializeMessage(contents);

    if(acceptMessage(message)) {
      _vehicleLocationService.handleCcLocationReportRecord(message);
      return true;
    }

    return false;
  }

  private boolean acceptMessage(CcLocationReport message) {
    if(message == null)
      return false;

    CPTVehicleIden vehicleIdent = message.getVehicle();
    AgencyAndId vehicleId = new AgencyAndId(vehicleIdent.getAgencydesignator(),
        vehicleIdent.getVehicleId() + "");

    return _vehicleId.equals(vehicleId.toString());
  }

  @PostConstruct
  public void setup() {
    super.setup();
  }
  
  @PreDestroy 
  public void destroy() {
    super.destroy();
  }

  @Override
  public String getDepotPartitionKey() {
    return null;
  }

  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {
  }
  
}
