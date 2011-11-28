package org.onebusaway.nyc.transit_data_manager.siri;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAlertsResourceTest extends ServiceAlertsResource {

  @InjectMocks
  NycSiriService nycSiriService = new NycSiriService();

  @Test
  public void testList() throws JAXBException {
    // This is not meant to be an automatic test, but can be used for manual investigation.
    // Needs other services running.
    
//    Response list = list();
//    System.err.println(list);
  }

}
