package org.onebusaway.nyc.report.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.PostConstruct;

import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.services.CcAndInferredLocationDao;
import org.onebusaway.nyc.report.services.InferencePersistenceService;
import org.onebusaway.nyc.util.time.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

  private CcAndInferredLocationDao _locationDao;

  @Autowired
  public void setLocationDao(CcAndInferredLocationDao locationDao) {
    this._locationDao = locationDao;
  }

  private ArrayBlockingQueue<ArchivedInferredLocationRecord> messages = new ArrayBlockingQueue<ArchivedInferredLocationRecord>(
      100000);

  private int _batchSize;

  public void setBatchSize(String batchSizeStr) {
    _batchSize = Integer.decode(batchSizeStr);
  }

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  @PostConstruct
  public void setup() {
    final SaveThread saveThread = new SaveThread();
    _taskScheduler.scheduleWithFixedDelay(saveThread, 1000); // every second
  }

  @Override
  public void persist(ArchivedInferredLocationRecord record, String contents) {
      boolean accepted = messages.offer(record);
      if (!accepted) {
        _log.error("inf record " + record.getUUID() + " dropped, local buffer full! Clearing!");
        messages.clear();
      }
  }

  private class SaveThread implements Runnable {

    @Override
    public void run() {
      List<ArchivedInferredLocationRecord> reports = new ArrayList<ArchivedInferredLocationRecord>();
      // remove at most _batchSize (1000) records
      messages.drainTo(reports, _batchSize);
      logLatency(reports);
      try {
        _locationDao.saveOrUpdateRecords(reports.toArray(new ArchivedInferredLocationRecord[0]));

      } catch (Exception e) {
        _log.error("Error persisting=" + e);
      }
    }

    private void logLatency(List<ArchivedInferredLocationRecord> reports) {
      long now = SystemTime.currentTimeMillis();
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

}