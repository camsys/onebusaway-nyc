package org.onebusaway.nyc.transit_data_manager.job;

import org.onebusaway.nyc.transit_data_manager.persistence.service.SpearDataPersistenceService;
import org.onebusaway.nyc.transit_data_manager.persistence.service.UTSDataPersistenceService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
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
		persistSpearData(executionContext);
	}
	
	private void persistUTSData(JobExecutionContext executionContext) {
		UTSDataPersistenceService utsDataPersistenceService = (UTSDataPersistenceService) executionContext.getJobDetail().
				getJobDataMap().get("utsDataPersistenceService");
		
		persistCrewAssignmentData(utsDataPersistenceService);
		persistVehiclePipoData(utsDataPersistenceService);
		
	}
	
	private void persistSpearData(JobExecutionContext executionContext) {
		SpearDataPersistenceService spearDataPersistenceService = (SpearDataPersistenceService) executionContext.getJobDetail().
				getJobDataMap().get("spearDataPersistenceService");
		
		//Persist depot data
		boolean retry = true;
		int retryCount = 0;
		while(retry) {
			try {
				_log.info("persisting depot data");
				spearDataPersistenceService.saveDepotData();
				retry = false;
			} catch(DataAccessResourceFailureException e) {
				//Retry once if there is an exception
				retryCount++;
				if(retryCount > 1) {
					retry = false;
					_log.error("Error persisting depot data after retrying once");
					e.printStackTrace();
				}
				if(retry) {
					_log.info("Retry persisting depot data");
				}
			}
		}
	}
	
	private void persistVehiclePipoData(UTSDataPersistenceService utsDataPersistenceService) {
		//Persist vehicle PIPO data
		boolean retry = true;
		int retryCount = 0;
		while(retry) {
			try {
				_log.info("persisting vehicle PIPO data");
				utsDataPersistenceService.saveVehiclePulloutData();
				retry = false;
			} catch(DataAccessResourceFailureException e) {
				//Retry once if there is an exception
				retryCount++;
				if(retryCount > 1) {
					retry = false;
					_log.error("Error persisting vehicle PIPO data after retrying once");
					e.printStackTrace();
				}
				if(retry) {
					_log.info("Retry persisting vehicle PIPO data");
				}
			}
		}
	}
	
	private void persistCrewAssignmentData(UTSDataPersistenceService utsDataPersistenceService) {
		//Persist crew assignment data
		boolean retry = true;
		int retryCount = 0;
		while(retry) {
			try {
				_log.info("persisting crew assignment data");
				utsDataPersistenceService.saveCrewAssignmentData();
				retry = false;
			} catch(DataAccessResourceFailureException e) {
				//Retry once if there is an exception
				retryCount++;
				if(retryCount > 1) {
					retry = false;
					_log.error("Error persisting crew assignment data after retrying once");
					e.printStackTrace();
				}
				if(retry) {
					_log.info("Retry persisting crew assignment data");
				}
			}
		}
		
	}
	
}
