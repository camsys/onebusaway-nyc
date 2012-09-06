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

	
	@Override
	protected void executeInternal(JobExecutionContext executionContext)
			throws JobExecutionException {
		persistUTSData(executionContext);
	}
	
	private void persistUTSData(JobExecutionContext executionContext) {
		UTSDataPersistenceService utsDataPersistenceService = (UTSDataPersistenceService) executionContext.getJobDetail().
				getJobDataMap().get("utsDataPersistenceService");
		
		//Persist crew assignment data
		try {
			persistCrewAssignmentData(utsDataPersistenceService);
		} catch(Exception e) {
			//Retry once
			_log.info("Retry persisting crew assignment data");
			persistCrewAssignmentData(utsDataPersistenceService);
		}

		//Persist vehicle pipo data
		try {
			persistVehiclePipoData(utsDataPersistenceService);
		} catch(Exception e) {
			//Retry once
			_log.info("Retry persisting vehicle pipo data");
			persistVehiclePipoData(utsDataPersistenceService);
		}
		
		
	}
	
	private void persistVehiclePipoData(UTSDataPersistenceService utsDataPersistenceService) {
		_log.info("persisting vehicle PIPO data");
		utsDataPersistenceService.saveVehiclePulloutData();
	}
	
	private void persistCrewAssignmentData(UTSDataPersistenceService utsDataPersistenceService) {
		_log.info("persisting crew assignment data");
		utsDataPersistenceService.saveCrewAssignmentData();
	}


}
