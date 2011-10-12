package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;
import org.onebusaway.nyc.vehicle_tracking.impl.queue.InputQueueListenerTask;

import org.springframework.beans.factory.annotation.Autowired;

import tcip_final_3_0_5_1.CcLocationReport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ArchivingInputQueueListenerTask extends InputQueueListenerTask {

  @Autowired
  private CcLocationReportDao _dao;

  @Override
  public void processMessage(String address, String contents) {
    CcLocationReport message = deserializeMessage(contents);
    
    // message discarded, probably corrupted
    if (message == null) return;

    CcLocationReportRecord record = new CcLocationReportRecord(message, contents);
    _dao.saveOrUpdateReport(record);
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
