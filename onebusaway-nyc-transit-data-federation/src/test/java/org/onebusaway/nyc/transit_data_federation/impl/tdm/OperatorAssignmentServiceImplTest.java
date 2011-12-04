package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.impl.RestApiLibrary;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;

@RunWith(MockitoJUnitRunner.class)
public class OperatorAssignmentServiceImplTest {

  @SuppressWarnings("unused")
  @Mock
  private ConfigurationServiceImpl configService;

  @Mock
  private TransitDataManagerApiLibrary mockApiLibrary;

  @InjectMocks
  private OperatorAssignmentServiceImpl service;

  @Before
  public void setupApiLibrary() throws Exception {
    RestApiLibrary ral = new RestApiLibrary("localhost", null, "api");
    String json = "{\"crew\": [{\"agency-id\": \"MTA NYCT\",\"pass-id\": \"123456\",\"run-route\": \"63\",\"run-number\": \"200\",\"service-date\": \"2011-11-02\",\"updated\": \"2011-11-02T10:11:10-05:00\"}],\"status\": \"OK\"}";
    when(mockApiLibrary.getItemsForRequest("crew", "2011-10-14", "list"))
      .thenReturn(ral.getJsonObjectsForString(json));
    
    service.refreshData();
  }

  @Test
  public void getAll() throws Exception {
    ServiceDate date = ServiceDate.parseString("2011-10-14");
    Collection<OperatorAssignmentItem> items = service.getOperatorsForServiceDate(date);
    OperatorAssignmentItem item = items.iterator().next();

    assertEquals(item.getPassId(), "123456");
    assertEquals(item.getAgencyId(), "MTA NYCT");
    assertEquals(item.getRunRoute(), "63");
    assertEquals(item.getRunNumber(), "200");
  }

  @Test
  public void getForServiceDate() throws Exception {
    ServiceDate date = ServiceDate.parseString("2011-10-14");
    OperatorAssignmentItem item = service.getOperatorAssignmentItemForServiceDate(date, "123456");

    assertEquals(item.getPassId(), "123456");
    assertEquals(item.getAgencyId(), "MTA NYCT");
    assertEquals(item.getRunRoute(), "63");
    assertEquals(item.getRunNumber(), "200");
  }

}
