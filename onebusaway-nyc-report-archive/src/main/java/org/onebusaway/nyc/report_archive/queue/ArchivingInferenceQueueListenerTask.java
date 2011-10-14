package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.nyc.report_archive.model.NycQueuedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.onebusaway.nyc.report_archive.model.NycVehicleManagementStatusRecord;
import org.onebusaway.nyc.report_archive.services.NycVehicleManagementStatusDao;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data_federation.impl.queue.InferenceQueueListenerTask;
import org.onebusaway.container.refresh.Refreshable;

import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchivingInferenceQueueListenerTask extends InferenceQueueListenerTask {

  private static Logger _log = LoggerFactory.getLogger(ArchivingInferenceQueueListenerTask.class);

  @Autowired
  private NycQueuedInferredLocationDao _locationDao;
  @Autowired
  private NycVehicleManagementStatusDao _statusDao;

  @Refreshable(dependsOn = {"inference-engine.outputQueueHost", "inference-engine.outputQueuePort", 
      "inference-engine.outputQueueName"})
  @Override
  public void startListenerThread() {
    if(_initialized == true) {
      _log.warn("Configuration service tried to reconfigure inference output queue service; this service is not reconfigurable once started.");
      return;
    }

    String host = _configurationService.getConfigurationValueAsString("inference-engine.outputQueueHost", null);
    String queueName = _configurationService.getConfigurationValueAsString("inference-engine.outputQueueName", null);
    Integer port = _configurationService.getConfigurationValueAsInteger("inference-engine.outputQueuePort", 5567);

    if(host == null || queueName == null || port == null) {
      _log.info("Inference input queue is not attached; input hostname was not available via configuration service.");
      return;
    }

    initializeZmq(host, queueName, port);
  }

  @Override
  // this method can't throw exceptions or it will stop the queue
  // listening
  protected void processResult(NycQueuedInferredLocationBean inferredResult, String contents) {
    try {
      NycQueuedInferredLocationRecord locationRecord = new NycQueuedInferredLocationRecord(inferredResult, contents);

      NycVehicleManagementStatusBean managementBean = inferredResult.getManagementRecord();

      if (managementBean != null) {
	NycVehicleManagementStatusRecord managementRecord =
	  new NycVehicleManagementStatusRecord(managementBean);
	_statusDao.saveOrUpdateRecord(managementRecord);
      }
    
      _locationDao.saveOrUpdateRecord(locationRecord);
    } catch (Throwable t) {
      _log.error("Exception processing contents= " + contents, t);
    }
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
