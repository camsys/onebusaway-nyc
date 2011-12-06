package org.onebusaway.nyc.report_archive.queue;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;
import org.onebusaway.nyc.vehicle_tracking.impl.queue.InputQueueListenerTask;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;

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

  private String _zoneOffset;

  @Override
  // this method can't throw exceptions or it will stop the queue
  // listening
  public boolean processMessage(String address, String contents) {
    try {    
      CcLocationReport message = deserializeMessage(contents);

      if (message == null) {
	  _log.error("Message discarded, probably corrupted, contents= " + contents);
	  Exception e = new Exception("deserializeMessage failed, possible corrupted message.");

	  _dao.handleException(contents, e);
	  return false;
      }

      CcLocationReportRecord record = new CcLocationReportRecord(message, contents, _zoneOffset);
      if (record != null) {
        _dao.saveOrUpdateReport(record);
      }
    } catch (Throwable t) {
      _log.error("Exception processing contents= " + contents, t);
      try {
        _dao.handleException(contents, t);
      } catch (Throwable tt) {
        // we tried
        _log.error("Exception handling exception= " + tt);
      }
    }

    return true;
  }
  
  @PostConstruct
  public void setup() {
    super.setup();
    _zoneOffset = 
	_configurationService.getConfigurationValueAsString("tds.zoneOffset", "-04:00");
  }
  
  @PreDestroy 
  public void destroy() {
    super.destroy();
  }



}
