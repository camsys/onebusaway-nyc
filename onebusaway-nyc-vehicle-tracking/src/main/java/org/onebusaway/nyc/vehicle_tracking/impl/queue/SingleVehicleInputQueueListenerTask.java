package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

import org.springframework.beans.factory.annotation.Autowired;

import tcip_final_4_0_0_0.CPTVehicleIden;
import tcip_final_4_0_0_0.CcLocationReport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Listens to the real time queue for one vehicle and injects it into the inference process.
 * 
 * @author jmaki
 */
public class SingleVehicleInputQueueListenerTask extends InputQueueListenerTask
    implements PartitionedInputQueueListener {

  private final String _vehicleId = "MTA NYCT_2827";

  private VehicleLocationInferenceService _vehicleLocationService;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Override
  public String getQueueDisplayName() {
    return "SingleVehicleInputQueueListenerTask";
  }

  @Override
  public boolean processMessage(String address, byte[] buff) {
    final String contents = new String(buff);
    final RealtimeEnvelope envelope = deserializeMessage(contents);

    if (acceptMessage(envelope)) {
      _vehicleLocationService.handleRealtimeEnvelopeRecord(envelope);
      return true;
    }

    return false;
  }

  private boolean acceptMessage(RealtimeEnvelope envelope) {
    if (envelope == null || envelope.getCcLocationReport() == null)
      return false;

    final CcLocationReport message = envelope.getCcLocationReport();
    final CPTVehicleIden vehicleIdent = message.getVehicle();
    final AgencyAndId vehicleId = new AgencyAndId(
        vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId() + "");

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
    // not partitioned by depot, nothing to do here.
  }

}
