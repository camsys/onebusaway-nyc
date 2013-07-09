package org.onebusaway.nyc.report_archive.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.isA;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.report_archive.event.SNSApplicationEventData;
import org.onebusaway.nyc.report_archive.event.handlers.SNSApplicationEventPublisher;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

/**
 * Tests {@link EmergencyStatusNotificationServiceImpl}
 * @author abelsare
 *
 */
public class EmergencyStatusNotificationServiceImplTest {

	@Mock
	private SNSApplicationEventPublisher snsEventPublisher;
	
	@Mock
	private ServletContext servletContext;
	
	private EmergencyStatusNotificationServiceImpl service;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(servletContext.getInitParameter("sns.user")).thenReturn("testUser");
		when(servletContext.getInitParameter("sns.password")).thenReturn("testPassword");
		
		service = new EmergencyStatusNotificationServiceImpl();
		service.setSnsEventPublisher(snsEventPublisher);
		service.setInitialized(true);
	}
	
	@Test
	public void testNewRecordEmergency() {
		Map<Integer, Boolean> emergencyState = new HashMap<Integer, Boolean>();
		emergencyState.put(1, false);
		emergencyState.put(2, false);
		emergencyState.put(3, false);
		// this has changed, we need to see at least two records from a bus
		// before we decide it has changed status
		emergencyState.put(4, false);
		
		service.setEmergencyState(emergencyState);
		
		when(servletContext.getInitParameter("sns.sendNotification")).thenReturn("true");
		service.setServletContext(servletContext);
		
		CcLocationReportRecord record = mock(CcLocationReportRecord.class);
		when(record.getVehicleId()).thenReturn(4);
		when(record.getEmergencyCode()).thenReturn("1");
		
		service.process(record);
		
		assertTrue("Expecting new record to be present in the map", emergencyState.containsKey(4));
		assertEquals("Expecting emergency code to be set for the new record", true, emergencyState.get(4));
		
		verify(snsEventPublisher).setData(isA(SNSApplicationEventData.class));
		verify(snsEventPublisher).run();
	}
	
	@Test
	public void testNewRecordNoEmergency() {
		Map<Integer, Boolean> emergencyState = new HashMap<Integer, Boolean>();
		emergencyState.put(1, false);
		emergencyState.put(2, false);
		emergencyState.put(3, false);
		
		service.setEmergencyState(emergencyState);
		
		when(servletContext.getInitParameter("sns.sendNotification")).thenReturn("true");
		service.setServletContext(servletContext);
		
		CcLocationReportRecord record = mock(CcLocationReportRecord.class);
		when(record.getVehicleId()).thenReturn(4);
		when(record.getEmergencyCode()).thenReturn(null);
		
		service.process(record);
		
		assertTrue("Expecting new record to not be present in the map", emergencyState.containsKey(4));
		assertEquals("Expecting new record not to be in emergency", false , emergencyState.get(4));
		
		verify(snsEventPublisher, times(0)).setData(isA(SNSApplicationEventData.class));
		verify(snsEventPublisher, times(0)).run();
	}
	
	@Test
	public void testEmergencyStatusChange() {
		Map<Integer, Boolean> emergencyState = new HashMap<Integer, Boolean>();
		emergencyState.put(1, false);
		emergencyState.put(2, false);
		emergencyState.put(3, false);
		emergencyState.put(4, false);
		emergencyState.put(5, true);
		emergencyState.put(6, true);
		
		service.setEmergencyState(emergencyState);
		
		when(servletContext.getInitParameter("sns.sendNotification")).thenReturn("true");
		service.setServletContext(servletContext);
		
		CcLocationReportRecord record = mock(CcLocationReportRecord.class);
		when(record.getVehicleId()).thenReturn(2);
		when(record.getEmergencyCode()).thenReturn("1");
		
		
		service.process(record);
		
		assertEquals("Expecting emergency code to be updated for the record", true, emergencyState.get(2));
		
		verify(snsEventPublisher).setData(isA(SNSApplicationEventData.class));
		verify(snsEventPublisher).run();
		
		
	}
	
	@Test
	public void testEmergencyStatusNoChange() {
		Map<Integer, Boolean> emergencyState = new HashMap<Integer, Boolean>();
		emergencyState.put(1, false);
		emergencyState.put(2, false);
		emergencyState.put(3, false);
		emergencyState.put(4, false);
		emergencyState.put(5, true);
		emergencyState.put(6, true);
		
		service.setEmergencyState(emergencyState);
		
		when(servletContext.getInitParameter("sns.sendNotification")).thenReturn("true");
		service.setServletContext(servletContext);
		
		CcLocationReportRecord record = mock(CcLocationReportRecord.class);
		when(record.getVehicleId()).thenReturn(5);
		when(record.getEmergencyCode()).thenReturn("1");

		service.process(record);
		
		assertEquals("Expecting no change in emergency code", true, emergencyState.get(5));
		
		verify(snsEventPublisher, times(0)).setData(isA(SNSApplicationEventData.class));
		verify(snsEventPublisher, times(0)).run();
	}
	
	@Test
	public void testEmergencyStatusReset() {
		Map<Integer, Boolean> emergencyState = new HashMap<Integer, Boolean>();
		emergencyState.put(1, false);
		emergencyState.put(2, false);
		emergencyState.put(3, false);
		emergencyState.put(4, false);
		emergencyState.put(5, true);
		emergencyState.put(6, true);
		
		service.setEmergencyState(emergencyState);
		
		when(servletContext.getInitParameter("sns.sendNotification")).thenReturn("true");
		service.setServletContext(servletContext);
		
		CcLocationReportRecord record = mock(CcLocationReportRecord.class);
		when(record.getVehicleId()).thenReturn(6);
		when(record.getEmergencyCode()).thenReturn(null);

		service.process(record);
		
		assertFalse("Expecting updated emergency code", emergencyState.get(6));
		
		verify(snsEventPublisher).setData(isA(SNSApplicationEventData.class));
		verify(snsEventPublisher).run();
	}
	
	@Test
	public void testEmergencyNotificationOff() {

		when(servletContext.getInitParameter("sns.sendNotification")).thenReturn("false");
		service.setServletContext(servletContext);
		
		CcLocationReportRecord record = mock(CcLocationReportRecord.class);
		when(record.getVehicleId()).thenReturn(6);
		when(record.getEmergencyCode()).thenReturn(null);

		service.process(record);
		
		
		verify(snsEventPublisher, times(0)).setData(isA(SNSApplicationEventData.class));
		verify(snsEventPublisher, times(0)).run();
	}

}
