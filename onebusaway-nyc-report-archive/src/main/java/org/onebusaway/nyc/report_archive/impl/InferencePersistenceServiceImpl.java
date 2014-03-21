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
 * Manage the persistence of inference records. Handles the need to save in
 * batches for performance, but with a timeout to keep the db up-to-date. Also
 * does the post-processing of records via the TDS.
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
      ArchivedInferredLocationRecordAndContents message = new ArchivedInferredLocationRecordAndContents(
          record, contents);
      boolean accepted = preMessages.offer(message);
      if (!accepted) {
        _log.error("inf record " + record.getUUID() + " dropped, local buffer full!  Clearing!");
        preMessages.clear();
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

    locationRecord.setVehicleLocationRecordBean(vehicleLocation);

    postProcessSuccess = validationService.validateArchiveInferenceRecord(locationRecord);
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

        try {
        	boolean postProcessSuccess = postProcess(record.getRecord());
        	if (postProcessSuccess) {
        	  postMessages.add(record.getRecord());
        	} else {
        	  discardRecord(record.getRecord().getVehicleId(), record.getContents());
        	}
        } catch (Throwable t) {
        	_log.error("caught postProcess error:", t);
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
      logLatency(reports);
      try {
        _locationDao.saveOrUpdateRecords(reports.toArray(new ArchivedInferredLocationRecord[0]));

      } catch (Exception e) {
        _log.error("Error persisting=" + e);
      }
    }

    private void logLatency(List<ArchivedInferredLocationRecord> reports) {
      long now = System.currentTimeMillis();
      if (reports.size() > 0) {
        StringBuffer sb = new StringBuffer();
        sb.append("inf drained ");
        sb.append(reports.size());
        sb.append(" messages and has avg reported latency ");
        long reportedLatencySum = 0;
        for (int i = 0; i < reports.size(); i++) {
          reportedLatencySum += now - reports.get(i).getTimeReported().getTime();
        }
        sb.append((reportedLatencySum / reports.size()) / 1000);

        sb.append(" s and avg received latency ");
        long receivedLatencySum = 0;
        for (int i = 0; i < reports.size(); i++) {
          receivedLatencySum += now - reports.get(i).getLastUpdateTime();
        }
        sb.append((receivedLatencySum / reports.size()) / 1000);
        
        sb.append(" s and avg processing latency ");
        long processingLatencySum = 0;
        for (int i = 0; i < reports.size(); i++) {
          processingLatencySum += now - reports.get(i).getArchiveTimeReceived().getTime();
        }
        sb.append((processingLatencySum / reports.size()));
        sb.append(" ms");
        _log.info(sb.toString());
      } else {
        _log.info("inf nothing to do");
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