package org.onebusaway.nyc.transit_data_manager.job;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data_manager.persistence.service.SpearDataPersistenceService;
import org.onebusaway.nyc.transit_data_manager.persistence.service.UTSDataPersistenceService;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.dao.DataAccessResourceFailureException;


/**
 * Tests {@link TDMDataPersisterJob}
 * @author abelsare
 *
 */
public class TDMDataPersisterJobTest {

	@Mock
	private UTSDataPersistenceService utsDataPersisterService;
	
	@Mock
	private SpearDataPersistenceService spearDataPersistenceService;
	
	@Mock 
	private JobExecutionContext executionContext;
	
	@Mock
	private JobDetail jobDetail;
	
	@Mock 
	private JobDataMap jobDataMap;
	
	private TDMDataPersisterJob job;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(executionContext.getJobDetail()).thenReturn(jobDetail);
		when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
		when(jobDataMap.get("utsDataPersistenceService")).thenReturn(utsDataPersisterService);
		when(jobDataMap.get("spearDataPersistenceService")).thenReturn(spearDataPersistenceService);
		
		job = new TDMDataPersisterJob();
	}
	
	@Test
	public void testPersistData() throws JobExecutionException, DataAccessResourceFailureException {
		
		job.executeInternal(executionContext);
		
		verify(utsDataPersisterService).saveVehiclePulloutData();
		verify(utsDataPersisterService).saveCrewAssignmentData();
		verify(spearDataPersistenceService).saveDepotData();
	}
	
	@Test
	public void testPersistVehicleDataException() throws JobExecutionException, SQLException {
		doThrow(new DataAccessResourceFailureException("Test exception")).when(utsDataPersisterService).saveVehiclePulloutData();
		
		job.executeInternal(executionContext);
		
		verify(utsDataPersisterService, times(2)).saveVehiclePulloutData();
		verify(utsDataPersisterService, times(1)).saveCrewAssignmentData();
		verify(spearDataPersistenceService, times(1)).saveDepotData();
	}
	
	@Test
	public void testPersistCrewDataException() throws JobExecutionException, DataAccessResourceFailureException {
		doThrow(new DataAccessResourceFailureException("Test exception")).when(utsDataPersisterService).saveCrewAssignmentData();
		
		job.executeInternal(executionContext);
		
		verify(utsDataPersisterService, times(1)).saveVehiclePulloutData();
		verify(utsDataPersisterService, times(2)).saveCrewAssignmentData();
		verify(spearDataPersistenceService, times(1)).saveDepotData();
	}
	
	@Test
	public void testPersistDepotDataException() throws JobExecutionException, DataAccessResourceFailureException {
		doThrow(new DataAccessResourceFailureException("Test Exception")).when(spearDataPersistenceService).saveDepotData();
		
		job.executeInternal(executionContext);
		
		verify(utsDataPersisterService, times(1)).saveVehiclePulloutData();
		verify(utsDataPersisterService, times(1)).saveCrewAssignmentData();
		verify(spearDataPersistenceService, times(2)).saveDepotData();
	}

}
