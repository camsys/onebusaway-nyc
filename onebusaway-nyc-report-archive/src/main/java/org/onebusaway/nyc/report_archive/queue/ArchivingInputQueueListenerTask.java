package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;
import org.onebusaway.nyc.vehicle_tracking.impl.queue.InputQueueListenerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import tcip_final_3_0_5_1.CcLocationReport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ArchivingInputQueueListenerTask extends InputQueueListenerTask {

  protected static Logger _log = LoggerFactory.getLogger(ArchivingInputQueueListenerTask.class);

  @Autowired
  private CcLocationReportDao _dao;

  @Override
  // this method can't throw exceptions or it will stop the queue
  // listening
  public void processMessage(String address, String contents) {
    try {    
	CcLocationReport message = deserializeMessage(contents);
	CcLocationReportRecord record = new CcLocationReportRecord(message, contents);
	if (record != null) {
	    _dao.saveOrUpdateReport(record);
	}
    } catch (Throwable t) {
	_log.error("Exception process contents= " + contents, t);
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
