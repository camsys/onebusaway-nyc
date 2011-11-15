package util;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.ISODateTimeFormat;

import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.HalfOpenTimestampRangeStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.WorkflowStatusEnumeration;
import uk.org.siri.siri.AffectsScopeStructure.VehicleJourneys;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;

public class SiriHelper {

	Siri siri = new Siri();

	public SiriHelper() {
		ServiceDelivery serviceDelivery = new ServiceDelivery();
		siri.setServiceDelivery(serviceDelivery);
		List<SituationExchangeDeliveryStructure> list = serviceDelivery
				.getSituationExchangeDelivery();
		SituationExchangeDeliveryStructure sxDeliveryStructure = new SituationExchangeDeliveryStructure();
		Situations situations = new Situations();
		sxDeliveryStructure.setSituations(situations);
		list.add(sxDeliveryStructure);
	}

	public Siri getSiri() {
		return siri;
	}

	public void addPtSituationElementStructure(String summaryText,
			String descriptionText, String idNumber, String begins,
			String expires, String lines, String statusType, String progress) {

		List<PtSituationElementStructure> list = siri.getServiceDelivery()
				.getSituationExchangeDelivery().get(0).getSituations()
				.getPtSituationElement();

		PtSituationElementStructure ptSit = createPtSituationElementStructure(
				summaryText, descriptionText, idNumber, begins, expires, lines,
				statusType, progress);
		list.add(ptSit);

	}

	private PtSituationElementStructure createPtSituationElementStructure(
			String summaryText, String descriptionText, String idNumber,
			String begins, String expires, String lines, String statusType, String progress) {
		PtSituationElementStructure ptSit = new PtSituationElementStructure();
		ptSit.setSummary(defaultedTextStructure(summaryText));
		ptSit.setDescription(defaultedTextStructure(descriptionText));
		ptSit.setSituationNumber(createSiriNumberElement(idNumber));

		ptSit.setConsequences(createConsequences());

		HalfOpenTimestampRangeStructure s = new HalfOpenTimestampRangeStructure();
		if (begins != null && !begins.isEmpty()) {
			s.setStartTime(ISODateTimeFormat.dateTime().parseDateTime(begins)
					.toDate());
		}
		if (expires != null && !expires.isEmpty()) {
			s.setEndTime(ISODateTimeFormat.dateTime().parseDateTime(expires)
					.toDate());
		}

		ptSit.setPublicationWindow(s);

		AffectsScopeStructure affects = createAffects(lines);
		ptSit.setAffects(affects);

		if (false) {
			ptSit.setDetail(defaultedTextStructure("detail text"));
			ptSit.setAdvice(defaultedTextStructure("advice text"));

			// AffectsScopeStructure affects = new AffectsScopeStructure();
			// StopPoints stopPoints = new StopPoints();
			// AffectedStopPointStructure stopPoint = new
			// AffectedStopPointStructure();
			// StopPointRefStructure stopPointRef = new StopPointRefStructure();
			// stopPoint.setStopPointRef(stopPointRef);
			// stopPointRef.setValue("stoppoint ref");
			// stopPoints.getAffectedStopPoint().add(stopPoint);
			// affects.setStopPoints(stopPoints);

		}
		
		if (!StringUtils.isBlank(progress)) {
		  ptSit.setProgress(WorkflowStatusEnumeration.fromValue(progress));
		}
		return ptSit;
	}

	private AffectsScopeStructure createAffects(String lines) {
		AffectsScopeStructure affects = new AffectsScopeStructure();
		VehicleJourneys vehicleJourneys = new VehicleJourneys();
		affects.setVehicleJourneys(vehicleJourneys);

		for (String line : lines.split(",")) {
			addLineReference(line, vehicleJourneys);
		}
		return affects;
	}

	private void addLineReference(String lineReference,
			VehicleJourneys vehicleJourneys) {
		if (lineReference == null) {
			return;
		}
		AffectedVehicleJourneyStructure vehicleJourney = new AffectedVehicleJourneyStructure();
		LineRefStructure lineRef = new LineRefStructure();
		lineRef.setValue(lineReference);
		vehicleJourney.setLineRef(lineRef);
		vehicleJourneys.getAffectedVehicleJourney().add(vehicleJourney);
	}

	private EntryQualifierStructure createSiriNumberElement(String value) {
		EntryQualifierStructure number = new EntryQualifierStructure();
		number.setValue(value);
		return number;
	}

	private PtConsequencesStructure createConsequences() {
		PtConsequencesStructure c = new PtConsequencesStructure();
		PtConsequenceStructure cons = new PtConsequenceStructure();
		c.getConsequence().add(cons);

		cons.setCondition(ServiceConditionEnumeration.ALTERED);
		// cons.setAdvice(new PtAdviceStructure());
		// cons.setAffects(new AffectsScopeStructure());
		// cons.setBlocking(new BlockingStructure());
		// cons.setBoarding(new BoardingStructure());
		// cons.setCasualties(new CasualtiesStructure());
		// cons.setDelays(new DelaysStructure());
		// cons.setPeriod(new HalfOpenTimestampRangeStructure());
		// cons.setSeverity(SeverityEnumeration.SEVERE);
		// cons.setSuitabilities(new Suitabilities());

		return c;
	}

	public DefaultedTextStructure defaultedTextStructure(String value) {
		DefaultedTextStructure detailText = new DefaultedTextStructure();
		detailText.setLang("EN");
		detailText.setValue(value);
		return detailText;
	}

	public String asString() {
		JAXBContext jc;
		Writer writer = new StringWriter();
		try {
			jc = JAXBContext.newInstance(Siri.class);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			marshaller.marshal(siri, writer);
		} catch (JAXBException e) {
			return e.toString();
		}
		return writer.toString();
	}

}
