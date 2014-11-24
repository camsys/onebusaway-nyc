package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.CsvSignCodeToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.SignCodeData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.SignMessageFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DestinationSign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tcip_final_4_0_0.CCDestinationSignMessage;

public class DscResourceTest extends ResourceTest {


  private static Logger _log = LoggerFactory.getLogger(DscResourceTest.class);
  private SignCodeData data = null;
  
  @Before
  public void setUp() throws Exception {
    File tmpInFile = File.createTempFile("tmp", ".tmp");
    tmpInFile.deleteOnExit();
    
    // this draft depot assignments has additional fields in it to test leniency of parser
    InputStream resource = this.getClass().getResourceAsStream("mult-agency-dsc.csv");
    assertNotNull(resource);
    copy(resource, tmpInFile.getCanonicalPath());
    
    data = null;
    CsvSignCodeToDataCreator process = new CsvSignCodeToDataCreator(tmpInFile);
    data = process.generateDataObject();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void test1() throws Exception {
 
    List<CCDestinationSignMessage> msgs = data.getAllDisplays();
    assertNotNull(msgs);
    assertTrue(msgs.size() > 1);
    CCDestinationSignMessage msg = data.getDisplayForCode(6l).get(0);
    assertNotNull(msg);
    assertNotNull(msg.getRouteID().getAgencydesignator());
    assertNull(msg.getRouteID().getAgencyId());  // not set in CSV file -- we don't have this
    assertEquals("NOT IN SERVICE", msg.getMessageText());
    
    msg = data.getDisplayForCode(60l).get(0);
    assertNotNull(msg);
    assertNotNull(msg.getRouteID().getAgencydesignator());
    assertEquals("JAMAICA 165 ST TERM via SUTPHIN BL", msg.getMessageText());
  }
 
  @Test
  public void test2() throws Exception {
 
    List<CCDestinationSignMessage> msgs = data.getDisplayForCode(22l);
    assertNotNull(msgs);
    assertEquals(2, msgs.size());
    
    CCDestinationSignMessage msg1 = msgs.get(0);
    assertEquals("MTA BUS", msg1.getMessageText());
    assertEquals("MTABC", msg1.getRouteID().getAgencydesignator());
    
    CCDestinationSignMessage msg2 = msgs.get(1);
    assertEquals("NYCT BUS", msg2.getMessageText());
    assertEquals("MTA NYCT", msg2.getRouteID().getAgencydesignator());
  }
  
  @Test
  public void test3() throws Exception {
 
    List<CCDestinationSignMessage> msgs = data.getDisplayForCode(1020l);
    assertNotNull(msgs);
    assertEquals(2, msgs.size());
    
    CCDestinationSignMessage msg1 = msgs.get(0);
    assertEquals("ASTORIA 27 AV via 31 ST", msg1.getMessageText());
    assertEquals("MTABC", msg1.getRouteID().getAgencydesignator());
    
    CCDestinationSignMessage msg2 = msgs.get(1);
    assertEquals("WASHINGTON HEIGHTS BROADWAY-168 ST via MADISON AV", msg2.getMessageText());
    assertEquals("MTA NYCT", msg2.getRouteID().getAgencydesignator());
  }
  
  @Test
  public void testNewLine() throws Exception {
 
    List<CCDestinationSignMessage> msgs = data.getDisplayForCode(3133l);
    assertNotNull(msgs);
    assertEquals(1, msgs.size());
    
    // verify input has a new line char
    CCDestinationSignMessage msg1 = msgs.get(0);
    assertEquals("PLIMPTON\nE.L. GRANT HWY", msg1.getMessageText());
    assertEquals("MTA NYCT", msg1.getRouteID().getAgencydesignator());
    
    // verify TCIP parsing removes it
    ModelCounterpartConverter<CCDestinationSignMessage, DestinationSign> tcipToJsonConverter = new SignMessageFromTcip();
    DestinationSign dsc = tcipToJsonConverter.convert(msg1);
    assertEquals("PLIMPTON E.L. GRANT HWY", dsc.getMessageText());
  }
}