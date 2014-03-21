package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * obanyc-2153 buses with more than 30 minutes schedule deviation held from VM API 
 * when searching by LineRef.
 *
 */
public class SiriScheduleDeviation_IntegrationTest extends SiriIntegrationTestBase {
  protected static Logger _log = LoggerFactory.getLogger(SiriScheduleDeviation_IntegrationTest.class);
  private boolean _isSetUp = false;
  
  public SiriScheduleDeviation_IntegrationTest() {
    super(null);
  }
  
  @Before
  public void setUp() throws Throwable {
    
    if(_isSetUp == false) {
      BasicConfigurator.configure();
      // this trace is 4399 copied but schedule adjusted to have 50 minute schedule deviation
      setTrace("4399_late.csv");
      setBundle("2013Sept_Prod_r08_b04", "2013-12-25T11:53:00EDT");

      resetAll();
      loadRecords();
      
      _isSetUp = true;
    }

  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testLargeDeviationVM() throws HttpException, IOException, Throwable {
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("time", "1387993860000");
    params.put("LineRef", "BX10");

    HashMap<String,Object> vmResponse = getVmResponse(params);
    HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
    HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
    ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
    HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
    ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
    HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
    HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

    // we expect a response!
    assertTrue(mvj.get("LineRef") != null);

  }
}
