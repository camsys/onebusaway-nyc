package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ChangeLogTask implements Runnable {

  private Logger _log = LoggerFactory.getLogger(ChangeLogTask.class);
  protected MultiCSVLogger logger;
  protected BundleRequestResponse requestResponse;
  
  @Autowired
  public void setBundleRequestResponse(BundleRequestResponse requestResponse) {
    this.requestResponse = requestResponse;
  }
  
  @Autowired
  public void setLogger(MultiCSVLogger logger) {
    this.logger = logger;
  }
  
  @Override
  public void run() {
    _log.info("creating change log and comment");
    logger.changelogHeader(requestResponse.getRequest().getBundleComment());
    _log.info("done");
  }
}
