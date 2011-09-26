package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.nyc.vehicle_tracking.impl.queue.InputQueueListenerTask;

import tcip_final_3_0_5_1.CcLocationReport;

public class ArchivingInputQueueListenerTask extends InputQueueListenerTask {

  @Override
  public void processMessage(String address, String contents) {
    CcLocationReport message = deserializeMessage(contents);
//    CcLocationReportDao
  }

}
