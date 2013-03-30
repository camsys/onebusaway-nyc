package org.onebusaway.nyc.transit_data_manager.importers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CSVCcAnnouncementInfoConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CcAnnouncementInfoConverter;

import tcip_final_3_0_5_1.CCDestinationMessageIden;
import tcip_final_3_0_5_1.CCDestinationSignMessage;
import tcip_final_3_0_5_1.CPTRowMetaData;
import tcip_final_3_0_5_1.CcAnnouncementInfo;
import tcip_final_3_0_5_1.SCHRouteIden;

public class CSVCcAnnouncementInfoConverterTest {

  CcAnnouncementInfoConverter inConverter = null;

  @Before
  public void setup() throws URISyntaxException {
    ClassLoader classLoader = CSVCcAnnouncementInfoConverterTest.class.getClassLoader();
    //InputStream in = classLoader.getResourceAsStream("dsc.csv");
    File inputFile = new File(classLoader.getResource("dsc.csv").toURI());
    //Reader csvInputReader = new InputStreamReader(in);
    //inConverter = new CSVCcAnnouncementInfoConverter(csvInputReader);
    inConverter = new CSVCcAnnouncementInfoConverter(inputFile);
  }

  @Test
  /*
   * Sample file: routeName, routeId, messageText, direction
   */
  public void testGetDestinations() {
    CcAnnouncementInfo.Destinations destinations = inConverter.getDestinations();
    assertNotNull(destinations);
    assertTrue(destinations.getDestination().size() > 0);
    CCDestinationSignMessage msg = destinations.getDestination().get(0);
    compare(msg, "", 6, "NOT IN SERVICE", "");
    msg = destinations.getDestination().get(1);
    compare(msg, "", 7, "SUBWAY\nSHUTTLE", "");
    msg = destinations.getDestination().get(2);
    compare(msg, "", 10, "SEE SIGN BELOW", "");
    msg = destinations.getDestination().get(3);
    compare(msg, "", 11, "NEXT BUS PLEASE", "");
    msg = destinations.getDestination().get(4);
    compare(msg, "", 12, "NOT IN SERVICE", "");
    msg = destinations.getDestination().get(5);
    compare(msg, "", 13, "TRAINING BUS", "");
    msg = destinations.getDestination().get(6);
    compare(msg, "", 22, "NYCT BUS", "");
    msg = destinations.getDestination().get(7);
    compare(msg, "M1", 1010, "HARLEM 147 ST via MADISON AV", "N");
    msg = destinations.getDestination().get(8);
    compare(msg, "M1", 1011, "EAST VILLAGE 8 ST via 5 AV", "S");
    int size = destinations.getDestination().size() - 1;
    msg = destinations.getDestination().get(size);
    // make sure last row is captures
    compare(msg, "", 9500, "POLICE BUS", "");
  }

  private void compare(CCDestinationSignMessage msg, String routeName,
      int routeId, String messageText, String direction) {
    assertNotNull(msg);
    assertNotNull(msg.getMessageText());
    assertEquals(messageText, msg.getMessageText());
    // direction semantics have changed, its now null (not collected even if present)
    assertEquals(null, msg.getDirection());
    SCHRouteIden rId = msg.getRouteID();
    assertNotNull(rId);
    assertEquals(0, rId.getRouteId());
    assertEquals(routeName, rId.getRouteName());
    CPTRowMetaData md = msg.getMetadata();
    assertNotNull(md);
    assertNotNull(md.getUpdated());
    CCDestinationMessageIden msgId = msg.getMessageID();
    assertEquals(routeId, msgId.getMsgID());
  }
}