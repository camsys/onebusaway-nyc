package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.*;

import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotsToTcipXmlProcess;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * test that Depot API will ignore additional input, as long as its well formed.
 *
 */
public class DepotResourceTest extends ResourceTest {

  private static Logger _log = LoggerFactory.getLogger(DepotResourceTest.class);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void test() throws Exception {
    File tmpInFile = File.createTempFile("tmp", ".tmp");
    tmpInFile.deleteOnExit();
    File tmpOutFile = new File("/tmp/results.xml");
    
    // this draft depot assignments has additional fields in it to test leniency of parser
    InputStream resource = this.getClass().getResourceAsStream("draft_depot_assignments_with_extra_fields.xml");
    assertNotNull(resource);
    copy(resource, tmpInFile.getCanonicalPath());
    
    MtaBusDepotsToTcipXmlProcess process = new MtaBusDepotsToTcipXmlProcess(tmpInFile, tmpOutFile);
    process.executeProcess();
    process.writeToFile();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(tmpOutFile);
    XPath xpath = XPathFactory.newInstance().newXPath();
    

    NodeList nodes = (NodeList)xpath.evaluate("/cptFleetSubsets", doc, XPathConstants.NODESET);

    assertNotNull(nodes);
    assertEquals(1, nodes.getLength());
    
    nodes = (NodeList)xpath.evaluate("/cptFleetSubsets/subscriptionInfo", doc, XPathConstants.NODESET);
    assertNotNull(nodes);
    assertTrue(nodes.getLength() > 0);
    nodes = (NodeList)xpath.evaluate("/cptFleetSubsets/subscriptionInfo/requestedType", doc, XPathConstants.NODESET);
    assertNotNull(nodes);
    assertTrue(nodes.getLength() > 0);
    
    assertEquals("2", xpath.evaluate("/cptFleetSubsets/subscriptionInfo/requestedType/text()", doc, XPathConstants.STRING));
    
    assertEquals("CAST", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group/group-name/text()", doc, XPathConstants.STRING));
    assertEquals("1862", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group/group-members/group-member/vehicle-id/text()", doc, XPathConstants.STRING));
    assertEquals("1865", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group/group-members/group-member[2]/vehicle-id/text()", doc, XPathConstants.STRING));
    assertEquals("1925", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group[2]/group-members/group-member[1]/vehicle-id/text()", doc, XPathConstants.STRING));
    assertEquals("3117", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group[3]/group-members/group-member[1]/vehicle-id/text()", doc, XPathConstants.STRING));
    tmpOutFile.deleteOnExit();
  }


}
