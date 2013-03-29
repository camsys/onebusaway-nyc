package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.CsvSignCodeToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.SignCodeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tcip_final_3_0_5_1.CCDestinationSignMessage;

public class DscResourceTest extends ResourceTest {


  private static Logger _log = LoggerFactory.getLogger(DscResourceTest.class);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void test1() throws Exception {
    File tmpInFile = File.createTempFile("tmp", ".tmp");
    tmpInFile.deleteOnExit();
    
    // this draft depot assignments has additional fields in it to test leniency of parser
    InputStream resource = this.getClass().getResourceAsStream("mult-agency-dsc.csv");
    assertNotNull(resource);
    copy(resource, tmpInFile.getCanonicalPath());
    
    SignCodeData data = null;
    CsvSignCodeToDataCreator process = new CsvSignCodeToDataCreator(tmpInFile);
    data = process.generateDataObject();
    List<CCDestinationSignMessage> msgs = data.getAllDisplays();
    assertNotNull(msgs);
    assertTrue(msgs.size() > 1);
    CCDestinationSignMessage msg = data.getDisplayForCode(6l);
    assertNotNull(msg);
    assertNotNull(msg.getRouteID().getAgencydesignator());
    assertNull(msg.getRouteID().getAgencyId());  // not set in CSV file -- we don't have this
    assertEquals("NOT IN SERVICE", msg.getMessageText());
    
    msg = data.getDisplayForCode(60l);
    assertNotNull(msg);
    assertNotNull(msg.getRouteID().getAgencydesignator());
    assertEquals("JAMAICA 165 ST TERM via SUTPHIN BL", msg.getMessageText());
  }
 

}