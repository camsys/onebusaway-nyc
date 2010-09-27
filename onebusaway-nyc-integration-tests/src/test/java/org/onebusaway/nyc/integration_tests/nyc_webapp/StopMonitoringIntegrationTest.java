package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.*;

import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.StopMonitoringDelivery;

import com.thoughtworks.xstream.XStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class StopMonitoringIntegrationTest {

  @Test
  public void test() throws HttpException, IOException {

    HttpClient client = new HttpClient();
    String url = "http://localhost:9000/onebusaway-api-webapp/siri/stop-monitoring.xml?key=TEST&agencyId=2008&stopId=305344";
    GetMethod get = new GetMethod(url);

    client.executeMethod(get);
    String response = get.getResponseBodyAsString();
    assertTrue(response.startsWith("<Siri"));
    XStream xstream = new XStream();
    xstream.processAnnotations(Siri.class);
    Siri siri = (Siri) xstream.fromXML(response);
    assertNotNull(siri);
    // ...

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertFalse(serviceDelivery.MoreData);
    List<StopMonitoringDelivery> deliveries = serviceDelivery.stopMonitoringDeliveries;
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 0);
    StopMonitoringDelivery delivery = deliveries.get(0);
    /* we are so far operating off of only scheduled data, so Monitored is false */
    assertFalse(delivery.visits.get(0).MonitoredVehicleJourney.Monitored);
  }
}
