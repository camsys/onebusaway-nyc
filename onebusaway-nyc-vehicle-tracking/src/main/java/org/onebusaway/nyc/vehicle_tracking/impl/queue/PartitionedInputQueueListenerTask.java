package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

import org.springframework.beans.factory.annotation.Autowired;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class PartitionedInputQueueListenerTask extends InputQueueListenerTask
    implements PartitionedInputQueueListener {

  private String[] _depotPartitionKeys = null;

  private VehicleLocationInferenceService _vehicleLocationService;

  private VehicleAssignmentService _vehicleAssignmentService;

  @Autowired
  public void setVehicleAssignmentService(
      VehicleAssignmentService vehicleAssignmentService) {
    _vehicleAssignmentService = vehicleAssignmentService;
  }

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Override
  public boolean processMessage(String address, String contents) {
    final RealtimeEnvelope message = deserializeMessage(contents);

    if (acceptMessage(message)) {
      _vehicleLocationService.handleRealtimeEnvelopeRecord(message);
      return true;
    }

    return false;
  }

  private boolean acceptMessage(RealtimeEnvelope envelope) {
    if (envelope == null || envelope.getCcLocationReport() == null)
      return false;

    final CcLocationReport message = envelope.getCcLocationReport();
    final ArrayList<AgencyAndId> vehicleList = new ArrayList<AgencyAndId>();
    for (final String key : _depotPartitionKeys) {
      try {
        vehicleList.addAll(_vehicleAssignmentService.getAssignedVehicleIdsForDepot(key));
      } catch (final Exception e) {
        _log.warn("Error fetching assigned vehicles for depot " + key
            + "; will retry.");
        continue;
      }
    }

    final CPTVehicleIden vehicleIdent = message.getVehicle();
    final AgencyAndId vehicleId = new AgencyAndId(
        vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId() + "");

    return vehicleList.contains(vehicleId);
  }

  @Override
  public String getDepotPartitionKey() {
    final StringBuilder sb = new StringBuilder();
    for (final String key : _depotPartitionKeys) {
      if (sb.length() > 0)
        sb.append(",");
      sb.append(key);
    }
    return sb.toString();
  }

  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {
    if (depotPartitionKey != null && !depotPartitionKey.isEmpty())
      _depotPartitionKeys = depotPartitionKey.split(",");
    else
      _depotPartitionKeys = null;
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

}
