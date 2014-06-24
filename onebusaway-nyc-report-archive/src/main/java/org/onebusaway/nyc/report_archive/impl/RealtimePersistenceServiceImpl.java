package org.onebusaway.nyc.report_archive.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;
import org.onebusaway.nyc.report_archive.services.RealtimePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Manage the persistence of realtime records.  Handles the need to save in batches for performance, but
 * with a timeout to keep the db up-to-date.
 *
 */
public class RealtimePersistenceServiceImpl implements
    RealtimePersistenceService {

  protected static Logger _log = LoggerFactory.getLogger(RealtimePersistenceService.class);

  private ArrayBlockingQueue<CcLocationReportRecord> messages = new ArrayBlockingQueue<CcLocationReportRecord>(
      100000);

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  @Autowired
  private CcLocationReportDao _dao;

  /**
   * number of inserts to batch together
   */
  private int _batchSize;

  public void setBatchSize(String batchSizeStr) {
    _batchSize = Integer.decode(batchSizeStr);
  }

  @PostConstruct
  public void setup() {
    final SaveThread saveThread = new SaveThread();
    _taskScheduler.scheduleWithFixedDelay(saveThread, 1000); // every second
  }

  public void persist(CcLocationReportRecord record) {
    boolean accepted = messages.offer(record);
    if (!accepted) {
      _log.error("archive record " + record.getUUID() + " dropped, local buffer full!  Clearing");
      messages.clear();
    }
  }

  @PreDestroy
  public void destroy() {
  }

  private class SaveThread implements Runnable {

    @Override
    public void run() {
      List<CcLocationReportRecord> reports = new ArrayList<CcLocationReportRecord>();
      // remove at most _batchSize (1000) records
      messages.drainTo(reports, _batchSize);
      _log.info("bhs drained " + reports.size() + " messages");
      try {
        _dao.saveOrUpdateReports(reports.toArray(new CcLocationReportRecord[0]));
      } catch (Exception e) {
        _log.error("Error persisting=" + e);
      }
    }
  }
}