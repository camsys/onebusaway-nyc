package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import java.util.List;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

public class VehicleStatusServiceImplTest {

	@Mock
	private ConfigurationService configurationService;
	@Mock
	private RemoteConnectionService remoteConnectionService;
	
	private VehicleStatusServiceImpl service;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		service = new VehicleStatusServiceImpl();
		service.setConfigurationService(configurationService);
		service.setRemoteConnectionService(remoteConnectionService);
		
		System.setProperty("tdm.host", "tdm.dev.obanyc.com");
	}
	
	@Test
	public void testGetVehicleStatus() {
		String pulloutData = "{\"pullouts\":[{\"vehicle-id\":\"5638\"," +
				"\"agency-id-tcip\":\"2008\",\"agency-id\":\"MTA NYCT\",\"depot\":\"OS\"," +
				"\"service-date\":\"2012-07-18\",\"pullout-time\":\"2012-07-18T05:51:00-04:00\"," +
				"\"run\":\"M15-012\",\"operator-id\":\"1663\",\"pullin-time\":\"2012-07-19T00:23:00-04:00\"" +
				"}],\"status\":\"OK\"}";
		String lastKnownData = "{\"records\":[{" +
				  "\"uuid\": \"67f843e0-d1a8-11e1-8614-123139243533\"," +
				  "\"vehicle-agency-id\": 2008," +
			      "\"time-reported\": \"2012-07-19T13:48:20.030Z\"," +
			      "\"time-received\": \"2012-07-19T13:48:22.686Z\"," +
			      "\"archive-time-received\": \"2012-07-19T13:48:24.657Z\"," +
			      "\"operator-id-designator\": \"913450\"," +
			      "\"route-id-designator\": \"61\"," +
			      "\"run-id-designator\": \"144\"," +
			      "\"dest-sign-code\": 4611," +
			      "\"latitude\": 40.683021," +
			      "\"longitude\": -74.003720," +
			      "\"speed\": 16.0," +
			      "\"direction-deg\": 207.72," +
			      "\"agency-id\": \"MTA NYCT\"," +
			      "\"vehicle-id\": 5638," +
			      "\"depot-id\": \"JG\"," +
			      "\"service-date\": \"2012-07-19\"," +
			      "\"inferred-run-id\": \"B61-14\"," +
			      "\"inferred-block-id\": \"MTA NYCT_20120701CC_JG_22200_B61-14-JG_1972\"," +
			      "\"inferred-trip-id\": \"MTA NYCT_20120701CC_057500_B61_0103_B61_14\"," +
			      "\"inferred-route-id\": \"MTA NYCT_B61\"," +
			      "\"inferred-direction-id\": \"1\"," +
			      "\"inferred-dest-sign-code\": 4611," +
			      "\"inferred-latitude\": 40.683001," +
			      "\"inferred-longitude\": -74.003678," +
			      "\"inferred-phase\": \"IN_PROGRESS\"," +
			      "\"inferred-status\": \"default\"," +
			      "\"inference-is-formal\": false," +
			      "\"distance-along-block\": 30650.503480107127," +
			      "\"distance-along-trip\": 2374.6309121986087," +
			      "\"next-scheduled-stop-id\": \"MTA NYCT_305226\"," +
			      "\"next-scheduled-stop-distance\": 26.791625996334915" +
			    "}],\"status\":\"OK\"}";
		
		when(remoteConnectionService.getContent("http://tdm.dev.obanyc.com/api/pullouts/list")).thenReturn(pulloutData);
		when(configurationService.getConfigurationValueAsString("operational-api.host", "archive.dev.obanyc.com")).thenReturn("archive.dev.obanyc.com");
		when(remoteConnectionService.getContent("http://archive.dev.obanyc.com/api/record/last-known/list")).thenReturn(lastKnownData);
		
		List<VehicleStatus> vehicleStatusRecords = service.getVehicleStatus();
		
		VehicleStatus vehicleStatus = vehicleStatusRecords.get(0);
		
		assertEquals("Mismatched vehicle id", vehicleStatus.getVehicleId(), "5638");
		assertEquals("Mismatched direction id", vehicleStatus.getDirection(), "1");
		assertEquals("Mismatched inferred DSC", vehicleStatus.getInferredDSC(), "4611");
		assertEquals("Mismatched inferred state", vehicleStatus.getInferredState(), "IN_PROGRESS");
		assertEquals("Mismatched observed DSC", vehicleStatus.getObservedDSC(), "4611");
		assertEquals("Mismatched pull in time", vehicleStatus.getPullinTime(), "2012-07-19T00:23:00-04:00");
		assertEquals("Mismatched pull out time", vehicleStatus.getPulloutTime(), "2012-07-18T05:51:00-04:00");
		assertEquals("Mismatched route", vehicleStatus.getRoute(), "B61");
	}
	
	@Test
	public void testVehicleStatusNoData() {
		when(remoteConnectionService.getContent("http://tdm.dev.obanyc.com/api/pullouts/list")).thenReturn("{\"pullouts\":[],\"status\":\"OK\"}");
		when(configurationService.getConfigurationValueAsString("operational-api.host", "archive.dev.obanyc.com")).thenReturn("archive.dev.obanyc.com");
		when(remoteConnectionService.getContent("http://archive.dev.obanyc.com/api/record/last-known/list")).thenReturn("{\"records\":[],\"status\":\"OK\"}");
		
		List<VehicleStatus> vehicleStatusRecords = service.getVehicleStatus();
		
		assertEquals("Not expecting any vehicle status records", 0, vehicleStatusRecords.size());
	}
	
	@Test 
	public void testVehicleStatusMismatchVehicle() {
		String pulloutData = "{\"pullouts\":[{\"vehicle-id\":\"5638\"," +
				"\"agency-id-tcip\":\"2008\",\"agency-id\":\"MTA NYCT\",\"depot\":\"OS\"," +
				"\"service-date\":\"2012-07-18\",\"pullout-time\":\"2012-07-18T05:51:00-04:00\"," +
				"\"run\":\"M15-012\",\"operator-id\":\"1663\",\"pullin-time\":\"2012-07-19T00:23:00-04:00\"" +
				"}],\"status\":\"OK\"}";
		String lastKnownData = "{\"records\":[{" +
				  "\"uuid\": \"67f843e0-d1a8-11e1-8614-123139243533\"," +
				  "\"vehicle-agency-id\": 2008," +
			      "\"time-reported\": \"2012-07-19T13:48:20.030Z\"," +
			      "\"time-received\": \"2012-07-19T13:48:22.686Z\"," +
			      "\"archive-time-received\": \"2012-07-19T13:48:24.657Z\"," +
			      "\"operator-id-designator\": \"913450\"," +
			      "\"route-id-designator\": \"61\"," +
			      "\"run-id-designator\": \"144\"," +
			      "\"dest-sign-code\": 4611," +
			      "\"latitude\": 40.683021," +
			      "\"longitude\": -74.003720," +
			      "\"speed\": 16.0," +
			      "\"direction-deg\": 207.72," +
			      "\"agency-id\": \"MTA NYCT\"," +
			      "\"vehicle-id\": 344," +
			      "\"depot-id\": \"JG\"," +
			      "\"service-date\": \"2012-07-19\"," +
			      "\"inferred-run-id\": \"B61-14\"," +
			      "\"inferred-block-id\": \"MTA NYCT_20120701CC_JG_22200_B61-14-JG_1972\"," +
			      "\"inferred-trip-id\": \"MTA NYCT_20120701CC_057500_B61_0103_B61_14\"," +
			      "\"inferred-route-id\": \"MTA NYCT_B61\"," +
			      "\"inferred-direction-id\": \"1\"," +
			      "\"inferred-dest-sign-code\": 4611," +
			      "\"inferred-latitude\": 40.683001," +
			      "\"inferred-longitude\": -74.003678," +
			      "\"inferred-phase\": \"IN_PROGRESS\"," +
			      "\"inferred-status\": \"default\"," +
			      "\"inference-is-formal\": false," +
			      "\"distance-along-block\": 30650.503480107127," +
			      "\"distance-along-trip\": 2374.6309121986087," +
			      "\"next-scheduled-stop-id\": \"MTA NYCT_305226\"," +
			      "\"next-scheduled-stop-distance\": 26.791625996334915" +
			    "}],\"status\":\"OK\"}";
		
		when(remoteConnectionService.getContent("http://tdm.dev.obanyc.com/api/pullouts/list")).thenReturn(pulloutData);
		when(configurationService.getConfigurationValueAsString("operational-api.host", "archive.dev.obanyc.com")).thenReturn("archive.dev.obanyc.com");
		when(remoteConnectionService.getContent("http://archive.dev.obanyc.com/api/record/last-known/list")).thenReturn(lastKnownData);
		
		List<VehicleStatus> vehicleStatusRecords = service.getVehicleStatus();
		
		VehicleStatus vehicleStatus = vehicleStatusRecords.get(0);
		
		assertEquals("Mismatched vehicle id", vehicleStatus.getVehicleId(), "5638");
		assertNull("No direction id", vehicleStatus.getDirection());
		assertNull("No inferred DSC", vehicleStatus.getInferredDSC());
		assertNull("No inferred state", vehicleStatus.getInferredState());
		assertNull("No observed DSC", vehicleStatus.getObservedDSC());
		assertEquals("Mismatched pull in time", vehicleStatus.getPullinTime(), "2012-07-19T00:23:00-04:00");
		assertEquals("Mismatched pull out time", vehicleStatus.getPulloutTime(), "2012-07-18T05:51:00-04:00");
		assertNull("No route", vehicleStatus.getRoute());
	}

}
