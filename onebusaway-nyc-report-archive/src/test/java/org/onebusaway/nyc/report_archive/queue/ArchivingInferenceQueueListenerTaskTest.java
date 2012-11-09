package org.onebusaway.nyc.report_archive.queue;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.onebusaway.nyc.report_archive.services.RecordValidationService;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;

public class ArchivingInferenceQueueListenerTaskTest {

	private ArchivingInferenceQueueListenerTask inferenceQueueListenertask;
	
	@Mock
	private NycQueuedInferredLocationBean inferredResult;
	
	@Mock
	private NycQueuedInferredLocationDao locationDao;
	
	@Mock
	private NycTransitDataService nycTransitDataService;
	
	@Mock
	private RecordValidationService validationService;
	
	@Mock
	private NycVehicleManagementStatusBean managementBean;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(inferredResult.getVehicleId()).thenReturn("MTA NYCT_4123");
		when(inferredResult.getServiceDate()).thenReturn(1L);
		when(inferredResult.getRecordTimestamp()).thenReturn(1L);
		when(inferredResult.getManagementRecord()).thenReturn(managementBean);
		when(managementBean.getUUID()).thenReturn("123");
		when(inferredResult.getInferredLatitude()).thenReturn(1D);
		when(inferredResult.getInferredLongitude()).thenReturn(-1D);
		
		inferenceQueueListenertask = new ArchivingInferenceQueueListenerTask();
		inferenceQueueListenertask.setBatchSize("1000");
		inferenceQueueListenertask.setLocationDao(locationDao);
		inferenceQueueListenertask.setNycTransitDataService(nycTransitDataService);
		inferenceQueueListenertask.setValidationService(validationService);
	}

	@Test
	public void testInvalidInferredResult() {
		when(validationService.validateInferenceRecord(inferredResult)).thenReturn(false);	
		inferenceQueueListenertask.processResult(inferredResult, "");
		
		verify(nycTransitDataService, times(0)).getVehicleForAgency(isA(String.class), isA(Long.class));
		verify(nycTransitDataService, times(0)).getVehicleLocationRecordForVehicleId(isA(String.class), 
				isA(Long.class));
	}
	
	
	@Test
	public void testPostProcessingInferredLatitude() {
		VehicleLocationRecordBean vehicleLocationRecord = mock(VehicleLocationRecordBean.class);
		CoordinatePoint currentLocation = new CoordinatePoint(-1000.0000, 74.0000);
		
		when(validationService.validateInferenceRecord(inferredResult)).thenReturn(true);
		
		when(nycTransitDataService.getVehicleForAgency(isA(String.class), isA(Long.class))).thenReturn(null);
		when(nycTransitDataService.getVehicleLocationRecordForVehicleId(isA(String.class), isA(Long.class)))
			.thenReturn(vehicleLocationRecord);
		
		when(vehicleLocationRecord.getCurrentLocation()).thenReturn(currentLocation);
		when(validationService.isValueWithinRange(-1000.0000, -999.999999, 999.999999)).thenReturn(false);
		when(validationService.isValueWithinRange(74.0000, -999.999999, 999.999999)).thenReturn(true);
		
		inferenceQueueListenertask.processResult(inferredResult, "");
		
		verify(nycTransitDataService).getVehicleForAgency(isA(String.class), isA(Long.class));
		verify(nycTransitDataService).getVehicleLocationRecordForVehicleId(isA(String.class), isA(Long.class));
	}
	
	@Test
	public void testPostProcessingInferredLongitude() {
		VehicleLocationRecordBean vehicleLocationRecord = mock(VehicleLocationRecordBean.class);
		CoordinatePoint currentLocation = new CoordinatePoint(-43.0000, 1000.0000);
		
		when(validationService.validateInferenceRecord(inferredResult)).thenReturn(true);
		
		when(nycTransitDataService.getVehicleForAgency(isA(String.class), isA(Long.class))).thenReturn(null);
		when(nycTransitDataService.getVehicleLocationRecordForVehicleId(isA(String.class), isA(Long.class)))
			.thenReturn(vehicleLocationRecord);
		
		when(vehicleLocationRecord.getCurrentLocation()).thenReturn(currentLocation);
		
		when(validationService.isValueWithinRange(-43.0000, -999.999999, 999.999999)).thenReturn(true);
		when(validationService.isValueWithinRange(1000.0000, -999.999999, 999.999999)).thenReturn(false);
		
		inferenceQueueListenertask.processResult(inferredResult, "");
		
		verify(nycTransitDataService).getVehicleForAgency(isA(String.class), isA(Long.class));
		verify(nycTransitDataService).getVehicleLocationRecordForVehicleId(isA(String.class), isA(Long.class));
	}
	
	@Test 
	public void testValidInferenceRecord() {
		VehicleLocationRecordBean vehicleLocationRecord = mock(VehicleLocationRecordBean.class);
		VehicleStatusBean vehicleStatusBean = new VehicleStatusBean();
		CoordinatePoint currentLocation = new CoordinatePoint(-43.0000, 70.0000);
		
		when(validationService.validateInferenceRecord(inferredResult)).thenReturn(true);
		
		when(nycTransitDataService.getVehicleForAgency(isA(String.class), isA(Long.class))).thenReturn(vehicleStatusBean);
		when(nycTransitDataService.getVehicleLocationRecordForVehicleId(isA(String.class), isA(Long.class)))
			.thenReturn(vehicleLocationRecord);
		
		when(vehicleLocationRecord.getCurrentLocation()).thenReturn(currentLocation);
		
		when(validationService.isValueWithinRange(-43.0000, -999.999999, 999.999999)).thenReturn(true);
		when(validationService.isValueWithinRange(70.0000, -999.999999, 999.999999)).thenReturn(true);
		
		inferenceQueueListenertask.processResult(inferredResult, "");
		
		verify(nycTransitDataService).getVehicleForAgency(isA(String.class), isA(Long.class));
		verify(nycTransitDataService).getVehicleLocationRecordForVehicleId(isA(String.class), isA(Long.class));
	}

}
