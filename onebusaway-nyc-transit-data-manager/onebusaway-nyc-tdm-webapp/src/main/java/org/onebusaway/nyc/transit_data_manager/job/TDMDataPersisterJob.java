package org.onebusaway.nyc.transit_data_manager.job;

import org.onebusaway.nyc.transit_data_manager.persistence.service.UTSDataPersistenceService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Persists TDM web service data every day at 4 a.m.
 * 
 */
public class TDMDataPersisterJob extends QuartzJobBean {

	private static Logger _log = LoggerFactory.getLogger(TDMDataPersisterJob.class);

	private UTSDataPersistenceService utsDataPersistenceService;
	
	@Override
	protected void executeInternal(JobExecutionContext executionContext)
			throws JobExecutionException {
		_log.info("persisting vehicle PIPO data");
		utsDataPersistenceService = (UTSDataPersistenceService) executionContext.getJobDetail().getJobDataMap().get("utsDataPersistenceService");
		utsDataPersistenceService.saveVehiclePulloutData();
	}


}
