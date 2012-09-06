package org.onebusaway.nyc.transit_data_manager.job;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data_manager.persistence.service.UTSDataPersistenceService;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


/**
 * Tests {@link TDMDataPersisterJob}
 * @author abelsare
 *
 */
public class TDMDataPersisterJobTest {

	@Mock
	private UTSDataPersistenceService utsDataPersisterService;
	
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
		
		job = new TDMDataPersisterJob();
	}
	
	@Test
	public void testPersistUTSData() throws JobExecutionException {
		job.executeInternal(executionContext);
		
		verify(utsDataPersisterService).saveVehiclePulloutData();
		verify(utsDataPersisterService).saveCrewAssignmentData();
	}
	
	@Test(expected=RuntimeException.class)
	public void testPersistVehicleDataException() throws JobExecutionException {
		doThrow(new RuntimeException()).when(utsDataPersisterService).saveVehiclePulloutData();
		
		job.executeInternal(executionContext);
		
		verify(utsDataPersisterService, times(2)).saveVehiclePulloutData();
		verify(utsDataPersisterService, times(1)).saveCrewAssignmentData();
	}
	
	@Test(expected=RuntimeException.class)
	public void testPersistCrewDataException() throws JobExecutionException {
		doThrow(new RuntimeException()).when(utsDataPersisterService).saveCrewAssignmentData();
		
		job.executeInternal(executionContext);
		
		verify(utsDataPersisterService, times(0)).saveVehiclePulloutData();
		verify(utsDataPersisterService, times(2)).saveCrewAssignmentData();
	}

}
