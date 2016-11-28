package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.admin.service.ParametersService;
import org.onebusaway.nyc.admin.util.ConfigurationKeyTranslator;
import org.onebusaway.util.services.configuration.ConfigurationService;

/**
 * Tests {@link ParametersService}
 * @author abelsare
 *
 */
public class ParametersServiceImplTest {

	@Mock
	private ConfigurationService configurationService;
	
	private ConfigurationKeyTranslator keyTranslator;
	
	private ParametersServiceImpl service;
	
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		keyTranslator = new ConfigurationKeyTranslator();
		
		service = new ParametersServiceImpl();
		service.setConfigurationService(configurationService);
		service.setKeyTranslator(keyTranslator);
	}
	
	@Test
	public void testGetParameters() {
		Map<String, String> configParameters = new HashMap<String, String>();
		configParameters.put("tdm.crewAssignmentRefreshInterval", "120");
		configParameters.put("admin.senderEmailAddress", "mtabuscis@mtabuscis.net");
		
		when(configurationService.getConfiguration()).thenReturn(configParameters);
		
		Map<String,String> displayParameters = service.getParameters();
		
		assertTrue("Expecting translated key to be present in the map", 
				displayParameters.containsKey("tdmCrewAssignmentRefreshKey"));
		assertEquals("Expecting value to be associated with the translated key", "120",
				displayParameters.get("tdmCrewAssignmentRefreshKey"));
		
		assertTrue("Expecting translated key to be present in the map", 
				displayParameters.containsKey("adminSenderEmailAddressKey"));
		assertEquals("Expecting value to be associated with the translated key", "mtabuscis@mtabuscis.net",
				displayParameters.get("adminSenderEmailAddressKey"));
	}
	
	@Test
	public void testSaveParameters() throws Exception {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("tdmCrewAssignmentRefreshKey", "120");
		parameters.put("adminSenderEmailAddressKey", "mtabuscis@mtabuscis.net");
		
		boolean success = service.saveParameters(parameters);
		
		assertTrue("Expecting save operation to be successful", success);
		
		verify(configurationService).setConfigurationValue("tdm", "tdm.crewAssignmentRefreshInterval", "120");
		verify(configurationService).setConfigurationValue("admin", "admin.senderEmailAddress", 
				"mtabuscis@mtabuscis.net");
	}
	
	@Test
	public void testSaveParametersException() throws Exception {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("tdmCrewAssignmentRefreshKey", "120");
		parameters.put("adminSenderEmailAddressKey", "mtabuscis@mtabuscis.net");
		
		doThrow(new Exception()).when(configurationService).
						setConfigurationValue("tdm", "tdm.crewAssignmentRefreshInterval", "120");
		
		boolean success = service.saveParameters(parameters);
		
		assertFalse("Expecting save operation to be successful", success);
		
		verify(configurationService).setConfigurationValue("admin", "admin.senderEmailAddress", 
				"mtabuscis@mtabuscis.net");
	}

}
