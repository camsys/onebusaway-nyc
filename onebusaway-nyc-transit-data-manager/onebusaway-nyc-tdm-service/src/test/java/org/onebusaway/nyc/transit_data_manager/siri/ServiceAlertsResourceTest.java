package org.onebusaway.nyc.transit_data_manager.siri;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/org/onebusaway/nyc/transit_data_manager/test-application-context.xml")
public class ServiceAlertsResourceTest extends ServiceAlertsResource {

  @Test
  public void testList() throws JAXBException {
    Response list = list();
    System.err.println(list);
  }

}
