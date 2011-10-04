package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

public class PartitionedInputQueueListenerTask extends InputQueueListenerTask {

  private String[] _depotPartitionKeys = null;

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
    ArrayList<AgencyAndId> vehicleList = new ArrayList<AgencyAndId>();
    for(String key : _depotPartitionKeys) {
      vehicleList.addAll(_vehicleAssignmentService.getAssignedVehicleIdsForDepot(key));
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
    if(depotPartitionKey != null)
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
