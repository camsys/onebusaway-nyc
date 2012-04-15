package org.onebusaway.nyc.report_archive.queue;

import org.codehaus.jackson.map.DeserializationConfig;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.onebusaway.nyc.report_archive.services.NycVehicleManagementStatusDao;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data_federation.impl.queue.InferenceQueueListenerTask;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchivingInferenceQueueListenerTask extends
    InferenceQueueListenerTask {

  public static final int COUNT_INTERVAL = 10000;
  public static final int DELAY_THRESHOLD = 10 * 1000;
  private static Logger _log = LoggerFactory.getLogger(ArchivingInferenceQueueListenerTask.class);

  @Autowired
  private NycQueuedInferredLocationDao _locationDao;
  @Autowired
  private NycVehicleManagementStatusDao _statusDao;
  @Autowired
  private TransitDataService _transitDataService;

  private int _batchSize;

  public void setBatchSize(String batchSizeStr) {
    _batchSize = Integer.decode(batchSizeStr);
  }

  private int count = 0;
  private long countStart = System.currentTimeMillis();
  private int _batchCount = 0;
  private List<ArchivedInferredLocationRecord> records = Collections.synchronizedList(new ArrayList<ArchivedInferredLocationRecord>());

  @Refreshable(dependsOn = {
      "tds.inputQueueHost", "tds.inputQueuePort", "tds.inputQueueName"})
  @Override
  public void startListenerThread() {
    if (_initialized == true) {
      _log.warn("Configuration service tried to reconfigure inference output queue service; this service is not reconfigurable once started.");
      return;
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
  // this method can't throw exceptions or it will stop the queue
  // listening
  protected void processResult(NycQueuedInferredLocationBean inferredResult,
      String contents) {
    count++;

    try {
      if (_log.isDebugEnabled())
        _log.debug("vehicle=" + inferredResult.getVehicleId() + ":"
            + new Date(inferredResult.getRecordTimestamp()));
      ArchivedInferredLocationRecord locationRecord = new ArchivedInferredLocationRecord(
          inferredResult, contents);
      long processingStart = System.currentTimeMillis();
      postProcess(locationRecord);
      _batchCount++;
      records.add(locationRecord);
      if (_batchCount == _batchSize) {
        _locationDao.saveOrUpdateRecords(records.toArray(new ArchivedInferredLocationRecord[0]));
        records.clear();
        _batchCount = 0;
      }

      if (count > COUNT_INTERVAL) {
        _log.info("inference_queue processed " + count + " messages in "
            + (System.currentTimeMillis() - countStart));
        if (locationRecord != null) {
          long delta = System.currentTimeMillis()
              - locationRecord.getArchiveTimeReceived().getTime();
          if (delta > DELAY_THRESHOLD) {
            _log.error("inference queue is " + delta + " millis behind");
          }
          count = 0;
          countStart = System.currentTimeMillis();
        }
      }
    } catch (Throwable t) {
      _log.error("Exception processing contents= " + contents, t);
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

  @Override
  public Integer getQueuePort() {
    return _configurationService.getConfigurationValueAsInteger(
        "tds.inputQueuePort", 5567);
  }

  private void postProcess(ArchivedInferredLocationRecord locationRecord) {
    // Extract next stop id and distance
    String vehicleId = locationRecord.getAgencyId() + "_"
        + locationRecord.getVehicleId().toString();
    VehicleStatusBean vehicle = _transitDataService.getVehicleForAgency(
        vehicleId, locationRecord.getTimeReported().getTime());
    locationRecord.setVehicleStatusBean(vehicle);
    VehicleLocationRecordBean vehicleLocation = _transitDataService.getVehicleLocationRecordForVehicleId(
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
