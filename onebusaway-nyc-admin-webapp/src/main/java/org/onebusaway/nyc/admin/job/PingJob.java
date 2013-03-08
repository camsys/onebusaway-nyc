package org.onebusaway.nyc.admin.job;

import java.util.List;

import org.onebusaway.nyc.admin.service.UserManagementService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * A simple job to ping the database to keep the connection pool active.
 * The application is not used for long period of time leading to "database down"
 * error messages.
 *
 */
public class PingJob extends QuartzJobBean {

  private static Logger _log = LoggerFactory.getLogger(PingJob.class);

  @Override
  protected void executeInternal(JobExecutionContext arg0)
      throws JobExecutionException {
    // execute a simple query to keep db connection alive
    UserManagementService userManagementService = 
        (UserManagementService)arg0.getJobDetail().getJobDataMap().get("userManagementService");
    
    
    final String searchString = "a";
    if (userManagementService != null) {
      userManagementService.getUserNames(searchString);
    } else {
      _log.error("userManagementService not provisioned");
    }
    
  }

}
