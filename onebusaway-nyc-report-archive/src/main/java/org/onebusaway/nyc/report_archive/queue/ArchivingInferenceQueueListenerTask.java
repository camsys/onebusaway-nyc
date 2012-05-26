package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.onebusaway.nyc.report_archive.services.NycVehicleManagementStatusDao;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.queue.InferenceQueueListenerTask;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;

import org.codehaus.jackson.map.DeserializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ArchivingInferenceQueueListenerTask extends
    InferenceQueueListenerTask {

  public static final int DELAY_THRESHOLD = 10 * 1000;
  private static Logger _log = LoggerFactory.getLogger(ArchivingInferenceQueueListenerTask.class);

  @Autowired
  private NycQueuedInferredLocationDao _locationDao;
  @Autowired
  private NycVehicleManagementStatusDao _statusDao;
  @Autowired
  private NycTransitDataService _nycTransitDataService;

  private int _batchSize;

  public void setBatchSize(String batchSizeStr) {
    _batchSize = Integer.decode(batchSizeStr);
  }

  private long _lastCommitTime = System.currentTimeMillis();

  private long _commitTimeout = 1 * 1000; // 1 seconds by default

  /**
   * Time in milliseconds to give up waiting for data and commit current batch.
   * 
   * @param commitTimeout number of milliseconds to wait
   */
  public void setCommitTimeout(String commitTimeout) {
    _commitTimeout = Integer.decode(commitTimeout);
  }

  private int _batchCount = 0;

  private List<ArchivedInferredLocationRecord> records = Collections.synchronizedList(new ArrayList<ArchivedInferredLocationRecord>());

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
      _log.info("Inference input queue is not attached; input hostname was not available via configuration service.");
      return;
    }
    _log.info("inference archive listening on " + host + ":" + port
        + ", queue=" + queueName);
    try {
      initializeQueue(host, queueName, port);
    } catch (InterruptedException ie) {
      return;
    }
  }

  @Override
  // this method must throw exceptions to force a transaction rollback
  protected void processResult(NycQueuedInferredLocationBean inferredResult,
      String contents) {

    if (_log.isDebugEnabled())
      _log.debug("vehicle=" + inferredResult.getVehicleId() + ":"
          + new Date(inferredResult.getRecordTimestamp()));
    ArchivedInferredLocationRecord locationRecord = new ArchivedInferredLocationRecord(
        inferredResult, contents);
    postProcess(locationRecord);
    _batchCount++;
    records.add(locationRecord);
    long batchWindow = System.currentTimeMillis() - _lastCommitTime;
    if (_batchCount == _batchSize || batchWindow > _commitTimeout) {
      try {
        _locationDao.saveOrUpdateRecords(records.toArray(new ArchivedInferredLocationRecord[0]));
      } finally {
        records.clear();
        _batchCount = 0;
        _lastCommitTime = System.currentTimeMillis();
      }
    }

    if (_batchCount == 0) {
      if (locationRecord != null) {
        long delta = System.currentTimeMillis()
            - locationRecord.getTimeReported().getTime();
        if (delta > DELAY_THRESHOLD) {
          _log.error("inference queue is " + (delta / 1000) + " seconds behind");
        }
      }
    }
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

  private void postProcess(ArchivedInferredLocationRecord locationRecord) {
    // Extract next stop id and distance
    String vehicleId = locationRecord.getAgencyId() + "_"
        + locationRecord.getVehicleId().toString();
    VehicleStatusBean vehicle = _nycTransitDataService.getVehicleForAgency(
        vehicleId, locationRecord.getTimeReported().getTime());
    locationRecord.setVehicleStatusBean(vehicle);
    VehicleLocationRecordBean vehicleLocation = _nycTransitDataService.getVehicleLocationRecordForVehicleId(
        vehicleId, locationRecord.getTimeReported().getTime());
    if (vehicleLocation != null && vehicleLocation.getCurrentLocation() != null) {
      locationRecord.setVehicleLocationRecordBean(vehicleLocation);
    }

  }

  @PostConstruct
  public void setup() {
    super.setup();

    // make parsing lenient
    _mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
        false);
  }

  @PreDestroy
  public void destroy() {
    super.destroy();
  }

}
