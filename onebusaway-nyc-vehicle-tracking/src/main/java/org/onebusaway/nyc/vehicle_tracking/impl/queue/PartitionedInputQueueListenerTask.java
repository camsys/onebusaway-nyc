package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

public class PartitionedInputQueueListenerTask 
  extends InputQueueListenerTask 
  implements PartitionedInputQueueListener {

  private String[] _depotPartitionKeys = null;

  private VehicleLocationInferenceService _vehicleLocationService;
  
  private VehicleAssignmentService _vehicleAssignmentService;

  @Autowired
  public void setVehicleAssignmentService(VehicleAssignmentService vehicleAssignmentService) {
    _vehicleAssignmentService = vehicleAssignmentService;
  }

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

    ArrayList<AgencyAndId> vehicleList = new ArrayList<AgencyAndId>();
    for(String key : _depotPartitionKeys) {
      try {
        vehicleList.addAll(_vehicleAssignmentService.getAssignedVehicleIdsForDepot(key));
      } catch(Exception e) {
        _log.warn("Error fetching assigned vehicles for depot " + key + "; will retry.");
        continue;
      }
    }
    
    CPTVehicleIden vehicleIdent = message.getVehicle();
    AgencyAndId vehicleId = new AgencyAndId(vehicleIdent.getAgencydesignator(),
        vehicleIdent.getVehicleId() + "");

    return vehicleList.contains(vehicleId);
  }

  public String getDepotPartitionKey() {
    StringBuilder sb = new StringBuilder();
    for(String key : _depotPartitionKeys) {
      if(sb.length() > 0)
        sb.append(",");
      sb.append(key);
    }
    return sb.toString();
  }

  public void setDepotPartitionKey(String depotPartitionKey) {
    if(depotPartitionKey != null && !depotPartitionKey.isEmpty())
      _depotPartitionKeys = depotPartitionKey.split(",");
    else
      _depotPartitionKeys = null;
  }
  
  @PostConstruct
  public void setup() {
    super.setup();
  }
  
  @PreDestroy 
  public void destroy() {
    super.destroy();
  }
  
}
