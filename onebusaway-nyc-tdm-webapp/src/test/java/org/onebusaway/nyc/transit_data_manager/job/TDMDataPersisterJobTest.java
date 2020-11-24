/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.onebusaway.nyc.transit_data_manager.persistence.service.DepotDataPersistenceService;
import org.onebusaway.nyc.transit_data_manager.persistence.service.VehicleAndCrewDataPersistenceService;
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
	private VehicleAndCrewDataPersistenceService vehicleAndCrewDataPersisterService;
	
	@Mock
	private DepotDataPersistenceService depotDataPersistenceService;
	
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
		when(jobDataMap.get("vehicleAndCrewDataPersistenceService")).thenReturn(vehicleAndCrewDataPersisterService);
		when(jobDataMap.get("depotDataPersistenceService")).thenReturn(depotDataPersistenceService);
		
		job = new TDMDataPersisterJob();
	}
	
	@Test
	public void testPersistData() throws JobExecutionException, DataAccessResourceFailureException {
		
		job.executeInternal(executionContext);
		
		verify(vehicleAndCrewDataPersisterService).saveVehiclePulloutData();
		verify(vehicleAndCrewDataPersisterService).saveCrewAssignmentData();
		verify(depotDataPersistenceService).saveDepotData();
	}
	
	@Test
	public void testPersistVehicleDataException() throws JobExecutionException, SQLException {
		doThrow(new DataAccessResourceFailureException("Test exception")).when(vehicleAndCrewDataPersisterService).saveVehiclePulloutData();
		
		job.executeInternal(executionContext);
		
		verify(vehicleAndCrewDataPersisterService, times(2)).saveVehiclePulloutData();
		verify(vehicleAndCrewDataPersisterService, times(1)).saveCrewAssignmentData();
		verify(depotDataPersistenceService, times(1)).saveDepotData();
	}
	
	@Test
	public void testPersistCrewDataException() throws JobExecutionException, DataAccessResourceFailureException {
		doThrow(new DataAccessResourceFailureException("Test exception")).when(vehicleAndCrewDataPersisterService).saveCrewAssignmentData();
		
		job.executeInternal(executionContext);
		
		verify(vehicleAndCrewDataPersisterService, times(1)).saveVehiclePulloutData();
		verify(vehicleAndCrewDataPersisterService, times(2)).saveCrewAssignmentData();
		verify(depotDataPersistenceService, times(1)).saveDepotData();
	}
	
	@Test
	public void testPersistDepotDataException() throws JobExecutionException, DataAccessResourceFailureException {
		doThrow(new DataAccessResourceFailureException("Test Exception")).when(depotDataPersistenceService).saveDepotData();
		
		job.executeInternal(executionContext);
		
		verify(vehicleAndCrewDataPersisterService, times(1)).saveVehiclePulloutData();
		verify(vehicleAndCrewDataPersisterService, times(1)).saveCrewAssignmentData();
		verify(depotDataPersistenceService, times(2)).saveDepotData();
	}

}
