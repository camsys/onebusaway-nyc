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
	public void test1() throws Exception {
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

		// test that vehicles are in the proper depots with proper agency ids. 
		nodes = (NodeList)xpath.evaluate("//vehicle-id[contains(., '1862')]/parent::*", doc, XPathConstants.NODESET);
		assertTrue(nodes.getLength() > 0);
		
		//check agency id of NYCT bus
		assertEquals("2008", xpath.evaluate("//vehicle-id[contains(., '1862')]/following-sibling::agency-id/text()", doc, XPathConstants.STRING));
		
		// check agency id of MTABC bus
		assertEquals("2188", xpath.evaluate("//vehicle-id[contains(., '1889')]/following-sibling::agency-id/text()", doc, XPathConstants.STRING));
		
		// check depot-id, could probably be tuned
		assertEquals("CAST", xpath.evaluate("//vehicle-id[contains(., '1862')]/parent::*/parent::*/parent::*/group-name/text()", doc, XPathConstants.STRING));
		assertEquals("ECH", xpath.evaluate("//vehicle-id[contains(., '1889')]/parent::*/parent::*/parent::*/group-name/text()", doc, XPathConstants.STRING));
		
		
		tmpOutFile.deleteOnExit();
	}
	// obanyc-1693: try out extra fields in the data feed, ensure the don't affect parsing
	@Test
	public void test2() throws Exception {
		File tmpInFile = File.createTempFile("tmp1", ".tmp");
		tmpInFile.deleteOnExit();
		File tmpOutFile = new File("/tmp/results1.xml");

		// this draft depot assignments has additional fields in it to test leniency of parser
		InputStream resource = this.getClass().getResourceAsStream("spear082212.txt");
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

		//check agency id of NYCT bus
		assertEquals("2008", xpath.evaluate("//vehicle-id[contains(., '8018')]/following-sibling::agency-id/text()", doc, XPathConstants.STRING));
		
		// check agency id of MTABC bus
		assertEquals("2188", xpath.evaluate("//vehicle-id[contains(., '6013')]/following-sibling::agency-id/text()", doc, XPathConstants.STRING));
		
		// check depot-id, could probably be tuned
		assertEquals("CAST", xpath.evaluate("//vehicle-id[contains(., '8018')]/parent::*/parent::*/parent::*/group-name/text()", doc, XPathConstants.STRING));
		assertEquals("EC", xpath.evaluate("//vehicle-id[contains(., '6013')]/parent::*/parent::*/parent::*/group-name/text()", doc, XPathConstants.STRING));
		assertEquals("true", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group[*]/group-members/group-member[*]/vehicle-id/text() = \"57\"", doc, XPathConstants.STRING));
		assertEquals("false", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group[*]/group-members/group-member[*]/vehicle-id/text() = \"0057\"", doc, XPathConstants.STRING));
		assertEquals("true", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group[*]/group-members/group-member[*]/vehicle-id/text() = \"4836\"", doc, XPathConstants.STRING));
		// Test added for Jira issue OBANYC-2258
		assertEquals("CMOE", xpath.evaluate("/cptFleetSubsets/defined-groups/defined-group/group-name[../group-members/group-member/vehicle-id='2055']/text()", doc, XPathConstants.STRING));

		tmpOutFile.deleteOnExit();
	}

}
