package org.onebusaway.nyc.transit_data_manager.job;

import org.onebusaway.nyc.transit_data_manager.persistence.service.DepotDataPersistenceService;
import org.onebusaway.nyc.transit_data_manager.persistence.service.VehicleAndCrewDataPersistenceService;
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
		persistvehicleAndCrewData(executionContext);
		persistDepotData(executionContext);
	}
	
	private void persistvehicleAndCrewData(JobExecutionContext executionContext) {
		VehicleAndCrewDataPersistenceService vehicleAndCrewDataPersistenceService = (VehicleAndCrewDataPersistenceService) executionContext.getJobDetail().
				getJobDataMap().get("vehicleAndCrewDataPersistenceService");
		
		persistCrewAssignmentData(vehicleAndCrewDataPersistenceService);
		persistVehiclePipoData(vehicleAndCrewDataPersistenceService);
		
	}
	
	private void persistDepotData(JobExecutionContext executionContext) {
		DepotDataPersistenceService depotDataPersistenceService = (DepotDataPersistenceService) executionContext.getJobDetail().
				getJobDataMap().get("depotDataPersistenceService");
		
		//Persist depot data
		boolean retry = true;
		int retryCount = 0;
		while(retry) {
			try {
				_log.info("persisting depot data");
				depotDataPersistenceService.saveDepotData();
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
	
	private void persistVehiclePipoData(VehicleAndCrewDataPersistenceService vehicleAndCrewDataPersistenceService) {
		//Persist vehicle PIPO data
		boolean retry = true;
		int retryCount = 0;
		while(retry) {
			try {
				_log.info("persisting vehicle PIPO data");
				vehicleAndCrewDataPersistenceService.saveVehiclePulloutData();
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
	
	private void persistCrewAssignmentData(VehicleAndCrewDataPersistenceService vehicleAndCrewDataPersistenceService) {
		//Persist crew assignment data
		boolean retry = true;
		int retryCount = 0;
		while(retry) {
			try {
				_log.info("persisting crew assignment data");
				vehicleAndCrewDataPersistenceService.saveCrewAssignmentData();
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
