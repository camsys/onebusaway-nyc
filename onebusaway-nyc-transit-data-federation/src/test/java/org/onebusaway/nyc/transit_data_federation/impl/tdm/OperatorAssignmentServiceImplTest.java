package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.util.impl.RestApiLibrary;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class OperatorAssignmentServiceImplTest {

  @SuppressWarnings("unused")
  @Mock
  private ConfigurationServiceImpl configService;

  @Mock
  private ConfigurationServiceClient mockApiLibrary;

  @InjectMocks
  private OperatorAssignmentServiceImpl service;

  @Before
  public void setupApiLibrary() throws Exception {
    RestApiLibrary ral = new RestApiLibrary("localhost", null, "api");
    String json = "{\"crew\": [{\"agency-id\": \"MTA NYCT\",\"pass-id\": \"123456\",\"depot\": \"JG\",\"run-route\": \"63\",\"run-number\": \"200\",\"service-date\": \"2011-11-02\",\"updated\": \"2011-11-02T10:11:10-05:00\"}],\"status\": \"OK\"}";

    when(mockApiLibrary.getItemsForRequest("crew", "2011-10-14", "list"))
      .thenReturn(ral.getJsonObjectsForString(json));
    
    service.refreshData();
  }

  @Test
  public void getAll() throws Exception {
    ServiceDate date = ServiceDate.parseString("20111014");
    Collection<OperatorAssignmentItem> items = service.getOperatorsForServiceDate(date);
    OperatorAssignmentItem item = items.iterator().next();

    assertEquals(item.getPassId(), "123456");
    assertEquals(item.getAgencyId(), "MTA NYCT");
    assertEquals(item.getRunRoute(), "63");
    assertEquals(item.getRunNumber(), "200");
    assertEquals(item.getDepot(), "JG");
  }

  @Test
  public void getForServiceDate() throws Exception {
    ServiceDate date = ServiceDate.parseString("20111014");
    OperatorAssignmentItem item = service.getOperatorAssignmentItemForServiceDate(date, new AgencyAndId("MTA NYCT", "123456"));

    assertEquals(item.getPassId(), "123456");
    assertEquals(item.getAgencyId(), "MTA NYCT");
    assertEquals(item.getRunRoute(), "63");
    assertEquals(item.getRunNumber(), "200");
    assertEquals(item.getDepot(), "JG");
  }

  @Test
  public void isApplicable() {
    Calendar cal = Calendar.getInstance();
    ServiceDate now = new ServiceDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    
    assertTrue(service.isApplicable(new ServiceDate(now)));
    Date stillValid1 = new Date(now.getAsDate().getTime() - (1 * 24 * 60 * 60 * 1000));
    assertTrue(service.isApplicable(new ServiceDate(stillValid1)));
    Date longAgo = new Date(now.getAsDate().getTime() - (2 * 24 * 60 * 60 * 1000));
    assertFalse(service.isApplicable(new ServiceDate(longAgo)));
    Date stillValid2 = new Date(now.getAsDate().getTime() + (1 * 24 * 60 * 60 * 1000));
    assertTrue(service.isApplicable(new ServiceDate(stillValid2)));
    Date aWaysOff = new Date(now.getAsDate().getTime() + (2 * 24 * 60 * 60 * 1000));
    assertFalse(service.isApplicable(new ServiceDate(aWaysOff)));
    
  }
}
