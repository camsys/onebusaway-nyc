package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
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
		RealtimeEnvelope envelope = deserializeMessage(contents); 

    if(acceptMessage(envelope)) {
      _vehicleLocationService.handleRealtimeEnvelopeRecord(envelope);
      return true;
    }

    return false;
  }

  private boolean acceptMessage(RealtimeEnvelope envelope) {
    if(envelope == null || envelope.getCcLocationReport() == null)
      return false;

		CcLocationReport message = envelope.getCcLocationReport();
    CPTVehicleIden vehicleIdent = message.getVehicle();
    AgencyAndId vehicleId = new AgencyAndId(vehicleIdent.getAgencydesignator(),
        vehicleIdent.getVehicleId() + "");

    return _vehicleId.equals(vehicleId.toString());
  }

  @Override
  @PostConstruct
  public void setup() {
    super.setup();
  }
  
  @Override
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
