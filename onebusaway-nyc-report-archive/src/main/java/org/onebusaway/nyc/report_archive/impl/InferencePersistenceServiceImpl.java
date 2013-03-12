package org.onebusaway.nyc.report_archive.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.InferencePersistenceService;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.onebusaway.nyc.report_archive.services.RecordValidationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
* Manage the persistence of inference records.  Handles the need to save in batches for performance, but
* with a timeout to keep the db up-to-date.  Also does the post-processing of records via the TDS.
*
*/
public class InferencePersistenceServiceImpl implements
    InferencePersistenceService {
  private static Logger _log = LoggerFactory.getLogger(InferencePersistenceServiceImpl.class);

  private NycQueuedInferredLocationDao _locationDao;

  @Autowired
  public void setLocationDao(NycQueuedInferredLocationDao locationDao) {
    this._locationDao = locationDao;
  }

  private NycTransitDataService _nycTransitDataService;
  
  @Autowired
  public void setNycTransitDataService(
      NycTransitDataService nycTransitDataService) {
    this._nycTransitDataService = nycTransitDataService;
  }
  
  private RecordValidationService validationService;
  
  @Autowired
  public void setValidationService(RecordValidationService validationService) {
    this.validationService = validationService;
  }
  
  private ExecutorService executorService = null;
  
  
  private ArrayBlockingQueue<ArchivedInferredLocationRecordAndContents> preMessages = new ArrayBlockingQueue<ArchivedInferredLocationRecordAndContents>(
      100000);
  private ArrayBlockingQueue<ArchivedInferredLocationRecord> postMessages = new ArrayBlockingQueue<ArchivedInferredLocationRecord>(
      100000);
  private int _batchSize;

  public void setBatchSize(String batchSizeStr) {
    _batchSize = Integer.decode(batchSizeStr);
  }

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  @PostConstruct
  public void setup() {
    executorService = Executors.newFixedThreadPool(1);
    executorService.execute(new PostProcessThread());
    final SaveThread saveThread = new SaveThread();
    _taskScheduler.scheduleWithFixedDelay(saveThread, 1000); // every second
  }

  @Override
  public void persist(ArchivedInferredLocationRecord record, String contents) {
    try {
      ArchivedInferredLocationRecordAndContents message = new ArchivedInferredLocationRecordAndContents(
          record, contents);
      preMessages.put(message);
    } catch (InterruptedException e) {
      _log.error("interrupted put=", e);
    }
  }

  private void discardRecord(Integer vehicleId, String contents) {
    _log.error(
        "Discarding inferred record for vehicle : {} as inferred latitude or inferred longitude "
            + "values are out of range", vehicleId);
    Exception e = new Exception("Inference record for vehile : " + vehicleId
        + " failed validation." + "Discarding");
    _locationDao.handleException(contents, e, new Date());
  }

  private boolean postProcess(ArchivedInferredLocationRecord locationRecord) {
    boolean postProcessSuccess = true;

    // Extract next stop id and distance
    String vehicleId = locationRecord.getAgencyId() + "_"
        + locationRecord.getVehicleId().toString();

    VehicleStatusBean vehicle = _nycTransitDataService.getVehicleForAgency(
        vehicleId, locationRecord.getTimeReported().getTime());
    locationRecord.setVehicleStatusBean(vehicle);

    VehicleLocationRecordBean vehicleLocation = _nycTransitDataService.getVehicleLocationRecordForVehicleId(
        vehicleId, locationRecord.getTimeReported().getTime());

    if (vehicleLocation != null && vehicleLocation.getCurrentLocation() != null) {
      if (validationService.isValueWithinRange(
          vehicleLocation.getCurrentLocation().getLat(), -999.999999,
          999.999999)
          && validationService.isValueWithinRange(
              vehicleLocation.getCurrentLocation().getLon(), -999.999999,
              999.999999)) {
        locationRecord.setVehicleLocationRecordBean(vehicleLocation);
      } else {
        postProcessSuccess = false;
      }
    }

    return postProcessSuccess;
  }
  
  @PreDestroy
  public void destroy() {
    executorService.shutdownNow();
  }

  
  private class PostProcessThread implements Runnable {

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        ArchivedInferredLocationRecordAndContents record;
        try {
          record = preMessages.take();
        } catch (InterruptedException e) {
          _log.error("PostProcessThread interrupted with " + e);
          return;
        }

        boolean postProcessSuccess = postProcess(record.getRecord());
        if (postProcessSuccess) {
          postMessages.add(record.getRecord());
        } else {
          discardRecord(record.getRecord().getVehicleId(), record.getContents());
        }
      }
    }
  }

  private class SaveThread implements Runnable {

    @Override
    public void run() {
      List<ArchivedInferredLocationRecord> reports = new ArrayList<ArchivedInferredLocationRecord>();
      // remove at most _batchSize (1000) records
      postMessages.drainTo(reports, _batchSize);
      if (reports.size() > 0) {
        _log.info("inf drained "
            + reports.size()
            + " messages and is "
            + ((System.currentTimeMillis() - reports.get(0).getTimeReported().getTime()) / 1000)
            + " seconds behind");
      } else {
        _log.info("inf nothing to do");
      }
      try {
        _locationDao.saveOrUpdateRecords(reports.toArray(new ArchivedInferredLocationRecord[0]));

      } catch (Exception e) {
        _log.error("Error persisting=" + e);
      }
    }
  }

  private class ArchivedInferredLocationRecordAndContents {
    ArchivedInferredLocationRecord r;
    String contents;

    ArchivedInferredLocationRecordAndContents(ArchivedInferredLocationRecord r,
        String contents) {
      this.r = r;
      this.contents = contents;
    }

    public ArchivedInferredLocationRecord getRecord() {
      return r;
    }

    public String getContents() {
      return contents;
    }
  }
}