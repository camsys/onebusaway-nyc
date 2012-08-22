package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.admin.service.ParametersService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

/**
 * Tests {@link ParametersService}
 * @author abelsare
 *
 */
public class ParametersServiceImplTest {

	@Mock
	private ConfigurationService configurationService;
	
	private ParametersServiceImpl service;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		service = new ParametersServiceImpl();
		service.setConfigurationService(configurationService);
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

}
