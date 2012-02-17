package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

public class PartitionedInputQueueListenerTask extends InputQueueListenerTask
  implements PartitionedInputQueueListener, ServletContextAware {

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

  public void setServletContext(ServletContext servletContext) {
    // check for depot partition keys in the servlet context
    if (servletContext != null) {
      String key = (String)servletContext.getInitParameter("depot.partition.key");
      _log.info("servlet context provied depot.partition.key=" + key);
      if (key != null) {
        setDepotPartitionKey(key);
      }
    }
  }

  @Override
  public boolean processMessage(String address, String contents) {
    RealtimeEnvelope message = deserializeMessage(contents);

    if (acceptMessage(message)) {
      _vehicleLocationService.handleRealtimeEnvelopeRecord(message);
      return true;
    }

    return false;
  }

  private boolean acceptMessage(RealtimeEnvelope envelope) {
    if (envelope == null || envelope.getCcLocationReport() == null)
      return false;

    CcLocationReport message = envelope.getCcLocationReport();
    ArrayList<AgencyAndId> vehicleList = new ArrayList<AgencyAndId>();
    for (String key : _depotPartitionKeys) {
      try {
        vehicleList.addAll(_vehicleAssignmentService.getAssignedVehicleIdsForDepot(key));
      } catch (Exception e) {
        _log.warn("Error fetching assigned vehicles for depot " + key
            + "; will retry.");
        continue;
      }
    }

    CPTVehicleIden vehicleIdent = message.getVehicle();
    AgencyAndId vehicleId = new AgencyAndId(vehicleIdent.getAgencydesignator(),
        vehicleIdent.getVehicleId() + "");

    return vehicleList.contains(vehicleId);
  }

  @Override
  public String getDepotPartitionKey() {
    StringBuilder sb = new StringBuilder();
    for (String key : _depotPartitionKeys) {
      if (sb.length() > 0)
        sb.append(",");
      sb.append(key);
    }
    return sb.toString();
  }

  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {
    _log.info("depotPartitionKey=" + depotPartitionKey);
    if (depotPartitionKey != null && !depotPartitionKey.isEmpty())
      _depotPartitionKeys = depotPartitionKey.split(",");
    else
      _depotPartitionKeys = null;
  }

  @Override
  @PostConstruct
  public void setup() {
    // test is depotPartitionKeys is overridden in context
    //XXXX
    super.setup();
  }

  @Override
  @PreDestroy
  public void destroy() {
    super.destroy();
  }

}
