package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

/**
 * Listen for vehicles assigned to one or more depots and inject them into the inference
 * process
 * 
 * @author jmaki
 *
 */
public class PartitionedInputQueueListenerTask extends InputQueueListenerTask
    implements PartitionedInputQueueListener, ServletContextAware {
  
  private String _depotPartitionKey;
  
  @Override
  public void setServletContext(ServletContext servletContext) {
    // check for depot partition keys in the servlet context
    if (servletContext != null) {
    	setDepotPartitionKey(servletContext.getInitParameter("depot.partition.key"));
      _log.info("servlet context provied depot.partition.key=" + _depotPartitionKey);
    }
  }
  
  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {
	  _depotPartitionKey = depotPartitionKey;
  }
  
  @Override
  public String getDepotPartitionKey() {
    return _depotPartitionKey;
  }

  @Override
  public boolean processMessage(String address, byte[] buff) throws Exception{
	  return _inputService.processMessage(address, buff);
  }
  
  @Override
  public String getQueueDisplayName() {
    return "PartitionedInputQueueListenerTask";
  }

  @Override
  @PostConstruct
  public void setup() {
	_inputService.setDepotPartitionKey(_depotPartitionKey);
    super.setup();
  }

  @Override
  @PreDestroy
  public void destroy() {
    super.destroy();
  }

  

}
