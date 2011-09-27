package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.nyc.report_archive.impl.CcLocationReportDaoImpl;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.vehicle_tracking.impl.queue.InputQueueListenerTask;

import tcip_final_3_0_5_1.CcLocationReport;

public class ArchivingInputQueueListenerTask extends InputQueueListenerTask {

  private CcLocationReportDaoImpl _dao;

  @Override
  public void processMessage(String address, String contents) {
    CcLocationReport message = deserializeMessage(contents);
    CcLocationReportRecord record = new CcLocationReportRecord(message, contents);
    _dao.saveOrUpdateReport(record);
  }

}
