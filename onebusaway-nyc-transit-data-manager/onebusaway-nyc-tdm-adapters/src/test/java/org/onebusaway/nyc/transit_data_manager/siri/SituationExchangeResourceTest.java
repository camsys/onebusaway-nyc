package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveriesStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.StatusResponseStructure;
import uk.org.siri.siri.SubscriptionResponseStructure;

public class SituationExchangeResourceTest extends SituationExchangeResource {

	private static final boolean EXPECT_FAILURE = true;

  public SituationExchangeResourceTest() throws JAXBException {
    super();
  }
  
  @Before
  public void setup() {
      setNycSiriService(new NycSiriServiceGateway());
  }

  @Test
  public void testServiceAndSubscriptionRequest() throws Exception {
    setupServiceAlerts();

    String body = loadSample("service-and-subscription-requests.xml");
    Response response = handlePost(body);
    Siri responseSiri = verifySiriResponse(response);

    verifySituationExchangeDelivery(responseSiri);
    verifySubscriptionRequest(responseSiri);
}

  @Test
  public void testServiceAndSubscriptionRequestMissingRequiredElement() throws Exception {
    setupServiceAlerts();

    String body = loadSample("service-and-subscription-requests-missing.xml");
    Response response = handlePost(body);
    Siri responseSiri = verifySiriResponse(response);

    verifySituationExchangeDelivery(responseSiri);
    verifySubscriptionRequest(responseSiri, EXPECT_FAILURE);
}

  @Test
  public void testServiceRequest() throws Exception {
    setupServiceAlerts();

    String body = loadSample("service-request.xml");
    Response response = handlePost(body);
    Siri responseSiri = verifySiriResponse(response);
    
    verifySituationExchangeDelivery(responseSiri);
  }

  @Test
  public void testSubscriptionRequest() throws Exception {
    getNycSiriService().setPersister(new MockSiriServicePersister());
    String body = loadSample("subscription-request.xml");
    Response response = handlePost(body);
    Siri responseSiri = verifySiriResponse(response);

    verifySubscriptionRequest(responseSiri);
  }

  private void verifySituationExchangeDelivery(Siri responseSiri) {
    SituationExchangeDeliveryStructure situationExchange = responseSiri.getServiceDelivery().getSituationExchangeDelivery().get(0);
    PtSituationElementStructure situation = situationExchange.getSituations().getPtSituationElement().get(0);
    assertEquals("description", situation.getDescription().getValue());
  }

  private void verifySubscriptionRequest(Siri responseSiri) {
    verifySubscriptionRequest(responseSiri, false);
  }
    
  private void verifySubscriptionRequest(Siri responseSiri, boolean expectFailure) {
    SubscriptionResponseStructure responseStructure = responseSiri.getSubscriptionResponse();
    assertTrue(responseStructure != null);
    List<StatusResponseStructure> status = responseStructure.getResponseStatus();
    assertTrue(status != null);
    assertEquals(1, status.size());
    StatusResponseStructure statusResponse = status.get(0);
    assertTrue(statusResponse != null);

    if (expectFailure) {
      assertEquals(false, statusResponse.isStatus());
      return;
    }

    // it's a Java UUID
    assertTrue(statusResponse.getSubscriptionRef().getValue().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    
    List<ServiceAlertSubscription> subscriptions = getNycSiriService().getActiveServiceAlertSubscriptions();
    assertEquals(1, subscriptions.size());
    ServiceAlertSubscription subscription = subscriptions.get(0);
    assertEquals("http://localhost/foo/bar",subscription.getAddress());
  }

  private Siri verifySiriResponse(Response response) {
    Object entity = response.getEntity();
    assertTrue(entity instanceof Siri);
    Siri responseSiri = (Siri)entity;
    return responseSiri;
  }

  private void setupServiceAlerts() {
    Map<String, ServiceAlertBean> currentServiceAlerts = new HashMap<String, ServiceAlertBean>();
    MockSiriServicePersister persister = new MockSiriServicePersister();
    persister.put("foo", ServiceAlertsTestSupport.createServiceAlertBean("MTA NYCT_1000"));
    getNycSiriService().setPersister(persister);
  }

	@Test
	public void testGenerateSiriResponse() throws JAXBException {
		Date time = new Date();
		List<SituationExchangeDeliveriesStructure> sxDeliveries = new ArrayList<SituationExchangeDeliveriesStructure>();
		Siri siri = generateSiriResponse(time, sxDeliveries);
		Writer writer = marshalToWriter(siri);
		dumpWriterToSyserr(writer);
		String result = writer.toString();
		// Somewhat lame test.
		assertTrue(result.matches("(?s).*<ServiceDelivery>.*<SituationExchangeDelivery>.*<Situations>.*<PtSituationElement>.*<Detail xml:lang=\"EN\">frobby morph</Detail>.*"));
	}

	@Test
	public void testGenerateSampleSiri() throws JAXBException, IOException {
		Siri siri = createSampleSiri1();

		Writer writer = marshalToWriter(siri);
		dumpWriterToSyserr(writer);
		  
		InputStream stream = getClass().getResourceAsStream("siri-sample4.xml");
		byte[] b = new byte[10240];
		int read = new DataInputStream(stream).read(b);
		String body = new String(b, 0, read);
	    JAXBContext jc = JAXBContext.newInstance(Siri.class);
	    Unmarshaller u = jc.createUnmarshaller();
	    Siri siri2 = (Siri) u.unmarshal(new StringReader(body));
	    // Somewhat lame test at the moment; go take a look.
	    assertEquals(extractDescription(siri), extractDescription(siri2));
		
	}

	private Siri createSampleSiri1() {
		SiriHelper siriHelper = new SiriHelper();

		siriHelper.addPtSituationElementStructure("X27, X28, X37 and X38 temporary bus stop change due a demonstration",
				"Until further notice Brooklyn bound buses will not make the\nstop at the near side of Cedar St due to police barricades and\nwill now make the stop at the far side of Cedar St. Please\nallow additional travel time.",
				"MTA NYCT_56754", "2011-10-26T00:00:00.000Z", "2011-11-25T23:59:00.000Z", "MTA NYCT_X27,MTA NYCT_X28,MTA NYCT_X37,MTA NYCT_X38", "D");		
    siriHelper.addPtSituationElementStructure("S79 buses detoured due to road work",
        "Buses are being detoured from Hylan Blvd between Midland Av and Steuben St",
        "MTA NYCT_56755", "2011-11-08T00:00:00.000Z", "", "MTA NYCT_S79", "D");
    siriHelper.addPtSituationElementStructure("S79 buses detoured due to road work",
        "Buses are being detoured from Hylan Blvd between Midland Av and Steuben St",
        "MTA NYCT_56755", "", "", "MTA NYCT_S79", "D");

		return siriHelper.getSiri();
	}

	private Writer marshalToWriter(Siri siri) throws JAXBException,
			PropertyException {
		JAXBContext jc = JAXBContext.newInstance(Siri.class);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		Writer writer = new StringWriter();
		marshaller.marshal(siri, writer);
		return writer;
	}

	private void dumpWriterToSyserr(Writer writer) {
		if (Boolean.getBoolean("dump.output")) {
			System.err.println(writer.toString());
		}
	}

	
	private String extractDescription(Siri siri) {
		ServiceDelivery delivery = siri.getServiceDelivery();
		List<SituationExchangeDeliveryStructure> list = delivery.getSituationExchangeDelivery();
		SituationExchangeDeliveryStructure structure = list.get(0);
		Situations situations = structure.getSituations();
		PtSituationElementStructure elementStructure = situations.getPtSituationElement().get(0);
		String description = elementStructure.getDescription().getValue();
		return description;
	}

  private String loadSample(String filename) throws IOException {
    InputStream stream = getClass().getResourceAsStream(filename);
    byte[] b = new byte[10240];
    int read = new DataInputStream(stream).read(b);
    String body = new String(b, 0, read);
    return body;
  }
  



}
