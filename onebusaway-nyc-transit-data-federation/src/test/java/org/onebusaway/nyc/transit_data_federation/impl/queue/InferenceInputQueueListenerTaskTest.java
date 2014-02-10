package org.onebusaway.nyc.transit_data_federation.impl.queue;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data_federation.impl.predictions.QueuePredictionIntegrationServiceImpl;
import org.onebusaway.nyc.transit_data_federation.impl.queue.InferenceInputQueueListenerTask;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;

import org.onebusaway.nyc.util.impl.RestApiLibrary;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceClientTDMImpl;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceClient;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;

import eu.datex2.schema._1_0._1_0.Time;

import java.net.URL;

@RunWith(MockitoJUnitRunner.class)
public class InferenceInputQueueListenerTaskTest {

	@Mock
	private RefreshService refreshService;

	@Mock
	private ConfigurationServiceClient mockApiLibrary;

	@Mock
	private RestApiLibrary mockRestApiLibrary;

	@InjectMocks
	private ConfigurationServiceImpl service;

	private NycQueuedInferredLocationBean inferredResult;
	private InferenceInputQueueListenerTask task;
	private Long timestamp = new Long(System.currentTimeMillis());
	private VehicleLocationListener listener;
	private PredictionIntegrationService pService;

	public NycQueuedInferredLocationBean initializeBean(){
		NycQueuedInferredLocationBean bean = new NycQueuedInferredLocationBean();
		bean.setVehicleId("MTA NYCT_123456789");
		bean.setRecordTimestamp(timestamp);	
		bean.setBlockId("sample_blockID");
		bean.setTripId("sample_tripID");
		bean.setServiceDate(Long.parseLong("1374120000000"));
		bean.setDistanceAlongBlock(Double.parseDouble("9379.514420929047"));
		bean.setInferredLatitude(Double.parseDouble("40.82360253803387"));
		bean.setInferredLongitude(Double.parseDouble("-73.85864343194719"));
		bean.setPhase("IN_PROGRESS");
		bean.setStatus("sample_Status");
		return bean;
	}

	@Before
	public void setupApiLibrary() throws Exception {
		task = new InferenceInputQueueListenerTask();
		task.setConfigurationService(service);
		listener = mock(VehicleLocationListener.class);
		pService =  mock(QueuePredictionIntegrationServiceImpl.class);
		task.setPredictionIntegrationService(pService);
		task.setVehicleLocationListener(listener);
	}

	@Test
	public void setValue() throws Exception {
		RestApiLibrary ral = new RestApiLibrary("localhost", null, "api");
		String json = new String("{\"config\":[{\"value\":\"20\",\"key\":\"tdm.crewAssignmentRefreshInterval\",\"description\":null,\"value-type\":\"String\",\"units\":\"minutes\",\"component\":\"tdm\",\"updated\":null}],\"status\":\"OK\"}");
		when(mockApiLibrary.getItemsForRequest("config", "list")).thenReturn(ral.getJsonObjectsForString(json));
		ConfigurationServiceClient tdmal = new ConfigurationServiceClientTDMImpl("tdm.staging.obanyc.com", 80, "/api");
		URL setUrl = tdmal.buildUrl("config", "testComponent", "test123", "set");
		when(mockRestApiLibrary.setContents(setUrl, "testValue")).thenReturn(true);
		service.refreshConfiguration();
		service.setConfigurationValue("testComponent", "test123", "testValue");
		assertEquals(service.getConfigurationValueAsString("test123", null), "testValue");
	}

	@Test
	public void testCheckAge() throws Exception {
		inferredResult = initializeBean();
		service.setConfigurationValue("display", "display.checkAge", "true");
		task.refreshCache();

		long defaultLimit = task.getAgeLimit();

		//one second over default configuration for age limit
		inferredResult.setRecordTimestamp(timestamp.longValue()-((defaultLimit+1)*1000));
		task.processResult(inferredResult, "");
		verify(listener,times(0)).handleVehicleLocationRecord((VehicleLocationRecord)any());

		//one second under default configuration for age limit
		inferredResult.setRecordTimestamp(timestamp.longValue()-((defaultLimit-1)*1000));
		task.processResult(inferredResult, "");
		verify(listener,times(1)).handleVehicleLocationRecord((VehicleLocationRecord)any());

		//age limit lowered by a minute
		service.setConfigurationValue("display", "display.ageLimit", Long.toString(defaultLimit-60));
		task.refreshCache();
		task.processResult(inferredResult, "");
		verify(listener,times(1)).handleVehicleLocationRecord((VehicleLocationRecord)any());

		//age limit increased by a minute
		service.setConfigurationValue("display", "display.ageLimit", Long.toString(defaultLimit+60));
		task.refreshCache();
		task.processResult(inferredResult, "");
		verify(listener,times(2)).handleVehicleLocationRecord((VehicleLocationRecord)any());
	}

	@Test
	public void testComputeTimeDifference() throws Exception {
		assertEquals(task.computeTimeDifference(timestamp), 0);
		assertTrue(task.computeTimeDifference(timestamp-50000) < 300);
		assertFalse(task.computeTimeDifference(timestamp-350000) < 300);
	}

	@Test
	public void testDefaultConfig() throws Exception {
		inferredResult = initializeBean();
		task.processResult(inferredResult, "");
		verify(listener, times(1)).handleVehicleLocationRecord((VehicleLocationRecord)any());
		verify(pService, times(0)).updatePredictionsForVehicle((AgencyAndId)any());
	}

	@Test
	public void testModifiedConfig() throws Exception {
		assertFalse(Boolean.parseBoolean(service.getConfigurationValueAsString("display.useTimePredictions", "error")));
		assertFalse(Boolean.parseBoolean(service.getConfigurationValueAsString("display.checkAge", "error")));
		service.setConfigurationValue("display", "display.useTimePredictions", "true");
		service.setConfigurationValue("display", "display.checkAge", "true");
		assertTrue(Boolean.parseBoolean(service.getConfigurationValueAsString("display.useTimePredictions", "error")));
		assertTrue(Boolean.parseBoolean(service.getConfigurationValueAsString("display.checkAge", "error")));
	}

	@Test
	public void testUseTimePredictions() throws Exception {
		inferredResult = initializeBean();
		service.setConfigurationValue("display", "display.useTimePredictions", "true");
		task.refreshCache();
		task.processResult(inferredResult, "");
		verify(listener,times(1)).handleVehicleLocationRecord((VehicleLocationRecord)any());
		verify(pService, times(1)).updatePredictionsForVehicle((AgencyAndId)any());
	}
}

