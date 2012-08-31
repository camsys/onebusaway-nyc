package org.onebusaway.nyc.transit_data_manager.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Example of a Quartz job -- configured by Spring triggers.  This job prints a log message every minute.
 * See data-sources.xml for its configuration.
 * 
 *
 */
public class PingJob extends QuartzJobBean {

  private static Logger _log = LoggerFactory.getLogger(PingJob.class);
  
  @Override
  protected void executeInternal(JobExecutionContext arg0)
      throws JobExecutionException {
    _log.info("ping");
  }

}
