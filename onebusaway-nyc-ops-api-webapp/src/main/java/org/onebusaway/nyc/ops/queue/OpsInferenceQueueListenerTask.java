package org.onebusaway.nyc.ops.queue;

import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jackson.map.DeserializationConfig;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.ops.services.CcAndInferredLocationService;
import org.onebusaway.nyc.presentation.service.cache.NycCacheService;
import org.onebusaway.nyc.report.api.json.JsonTool;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report.services.CcAndInferredLocationDao;
import org.onebusaway.nyc.report.services.InferencePersistenceService;
import org.onebusaway.nyc.report.services.RecordValidationService;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data_federation.impl.queue.InferenceQueueListenerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class OpsInferenceQueueListenerTask extends
    InferenceQueueListenerTask {

  private static Logger _log = LoggerFactory.getLogger(OpsInferenceQueueListenerTask.class);

  private CcAndInferredLocationDao _locationDao;

  private RecordValidationService validationService;

  private InferencePersistenceService persister;
  
  private CcAndInferredLocationService _locationService;
  
  private static final int STATUS_INTERVAL_MINUTES = 1;
  
  private ScheduledFuture<OpsInferenceQueueListenerTask.StatusThread> _statusTask = null;
  
  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;
  
  @Autowired
  public void setLocationDao(CcAndInferredLocationDao locationDao) {
    this._locationDao = locationDao;
  }

  @Autowired
  public void setValidationService(RecordValidationService validationService) {
    this.validationService = validationService;
  }

  @Autowired
  public void setInferencePersistenceService(
      InferencePersistenceService pService) {
    this.persister = pService;
  }
  
  @Autowired
  public void setLocationService(CcAndInferredLocationService locationService) {
    this._locationService = locationService;
  }

  @Refreshable(dependsOn = {
      "tds.inputQueueHost", "tds.inputQueuePort", "tds.inputQueueName"})
  @Override
  public void startListenerThread() {
    if (_initialized == true) {
      _log.warn("Configuration service reconfiguring inference output queue service.");
    }

    String host = getQueueHost();
    String queueName = getQueueName();
    Integer port = getQueuePort();

    if (host == null || queueName == null || port == null) {
      _log.error("Inference input queue is not attached; input hostname was not available via configuration service.");
      return;
    }
    _log.info("inference archive listening on " + host + ":" + port
        + ", queue=" + queueName);
    try {
      initializeQueue(host, queueName, port);
      _log.warn("queue config:" + queueName + " COMPLETE");
    } catch (InterruptedException ie) {
      _log.error("queue " + queueName + " interrupted");
      return;
    } catch (Throwable t) {
      _log.error("queue " + queueName + " init failed:", t);
    }
  }

  @Override
  // this method must throw exceptions to force a transaction rollback
  protected void processResult(NycQueuedInferredLocationBean inferredResult,
      String contents) {
    long timeReceived = System.currentTimeMillis();
    ArchivedInferredLocationRecord locationRecord = null;

    if (_log.isDebugEnabled())
      _log.debug("vehicle=" + inferredResult.getVehicleId() + ":"
          + new Date(inferredResult.getRecordTimestamp()));

    boolean validInferredResult = validationService.validateInferenceRecord(inferredResult);

    if (validInferredResult) {
      locationRecord = new ArchivedInferredLocationRecord(inferredResult,
          contents, timeReceived);
        persister.persist(locationRecord, contents);
    } else {
      discardRecord(inferredResult.getVehicleId(), contents);
    }

  }

  private void discardRecord(String vehicleId, String contents) {
    _log.error(
        "Discarding inferred record for vehicle : {} as inferred latitude or inferred longitude "
            + "values are out of range, or tripID is too long", vehicleId);
    Exception e = new Exception("Inference record for vehile : " + vehicleId
        + " failed validation." + "Discarding");
    _locationDao.handleException(contents, e, new Date());
  }

  @Override
  public String getQueueHost() {
    return _configurationService.getConfigurationValueAsString(
        "tds.inputQueueHost", null);
  }

  @Override
  public String getQueueName() {
    return _configurationService.getConfigurationValueAsString(
        "tds.inputQueueName", null);
  }

  public String getQueueDisplayName() {
    return "archive_inference";
  }

  @Override
  public Integer getQueuePort() {
    return _configurationService.getConfigurationValueAsInteger(
        "tds.inputQueuePort", 5567);
  }
  
  public void logStatus() {
    _log.info("obanyc_inferredlocation records count : " + _locationDao.getArchiveInferredLocationCount());
    _log.info("obanyc_cclocationreport records count : " + _locationDao.getCcLocationReportRecordCount());
    _log.info("obanyc_last_known_vehicle records Count : " + _locationDao.getCcAndInferredLocationCount());
  }
  
  
  private class StatusThread extends TimerTask {
    @Override
    public void run() {
      logStatus();
    }
  }


  @SuppressWarnings("unchecked")
  @PostConstruct
  public void setup() {
    super.setup();

    // make parsing lenient
    _mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
        false);
    
   // get current state from backup ops api or archiver
    try {
    	_log.info("Attempting to set initial state");
		List<CcAndInferredLocationRecord> lastKnownRecords = _locationService.getAllLastKnownRecords();
		_locationDao.saveOrUpdateRecords(lastKnownRecords);
	} catch (Exception e) {
		_log.error("Failed to set initial state", e);
	}

    if (_statusTask == null) {
    	_statusTask = _taskScheduler.scheduleWithFixedDelay(new StatusThread(),
        STATUS_INTERVAL_MINUTES * 60 * 1000);
    }
    
  }

  @PreDestroy
  public void destroy() {
    super.destroy();
  }

}
