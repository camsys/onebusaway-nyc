package org.onebusaway.nyc.transit_data_manager.siri;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriService;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.ServiceAlert;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlertsService;

import uk.org.siri.siri.AffectedStopPointStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.AffectsScopeStructure.StopPoints;
import uk.org.siri.siri.AffectsScopeStructure.VehicleJourneys;
import uk.org.siri.siri.BlockingStructure;
import uk.org.siri.siri.BoardingStructure;
import uk.org.siri.siri.CasualtiesStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.DelaysStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.HalfOpenTimestampRangeStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.PtAdviceStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequenceStructure.Suitabilities;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SeverityEnumeration;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveriesStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.StopPointRefStructure;

public class SituationExchangeResourceTest extends SituationExchangeResource {

	@Test
	public void testHandlePost() throws Exception {
		ServiceAlertsService saService = mock(ServiceAlertsService.class);

		SiriService siriService = new SiriService();
		siriService.setServiceAlertService(saService);
		setSiriService(siriService);
		InputStream stream = getClass().getResourceAsStream("t.xml");
		byte[] b = new byte[10240];
		int read = new DataInputStream(stream).read(b);
		String body = new String(b, 0, read);
		Response response = handlePost(body);

		// Somewhat lame test at the moment.
		verify(saService).createOrUpdateServiceAlert(
				any(ServiceAlert.Builder.class), anyString());

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
				"Until further notice Brooklyn bound buses will not make the stop at the near side of Cedar St due to police barricades and will now make the stop at the far side of Cedar St. Please allow additional travel time.",
				"MTA NYCT_56754", "2011-10-26T00:00:00.000Z", "2011-11-25T23:59:00.000Z", "MTA NYCT_X27,MTA NYCT_X28,MTA NYCT_X37,MTA NYCT_X38", "D");		
		siriHelper.addPtSituationElementStructure("S79 buses detoured due to road work",
				"Buses are being detoured from Hylan Blvd between Midland Av and Steuben St",
				"MTA NYCT_56755", "2011-11-08T00:00:00.000Z", "", "MTA NYCT_S79", "D");

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



}
