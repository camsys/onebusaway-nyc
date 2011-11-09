package org.onebusaway.nyc.transit_data_manager.siri;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.OperatorAssignmentFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.OperatorAssignmentsMessage;
import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.SCHOperatorAssignment;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveriesStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.spring.Autowire;

@Path("/situation-exchange")
@Component
@Scope("request")
@Autowire
public class SituationExchangeResource {

	private static Logger _log = LoggerFactory
			.getLogger(SituationExchangeResource.class);

	private SiriService _siriService = new SiriService();

	// @POST
	// @Produces("application/xml")
	// @Consumes("application/xml")
	// public Response handlePost(String body) throws JAXBException {
	// JAXBContext jc = JAXBContext.newInstance(Siri.class);
	// Unmarshaller u = jc.createUnmarshaller();
	// Siri siri = (Siri)u.unmarshal(new StringReader(body));
	// return Response.ok(siri).build();
	// }

	@POST
  @Produces("application/xml")
  @Consumes("application/xml")
  public Response handlePost(String body) throws JAXBException {
	_log.debug("---begin body---\n" + body + "\n---end body---");
//    try {
		JAXBContext jc = JAXBContext.newInstance(Siri.class);
		Unmarshaller u = jc.createUnmarshaller();
		Siri siri = (Siri) u.unmarshal(new StringReader(body));
		ServiceDelivery delivery = siri.getServiceDelivery();
		for (SituationExchangeDeliveryStructure s : delivery.getSituationExchangeDelivery()) {
		  SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
		  _siriService.handleServiceDelivery(delivery, s, ESiriModuleType.SITUATION_EXCHANGE, endpointDetails);
		}
		return Response.ok(siri).build();
//	} catch (Exception e) {
//		_log.error(e.getMessage());
//		return Response.serverError().build();
//	}
  }

	/** Generate a siri response for a set of VehicleActivities */
	Siri generateSiriResponse(Date time,
			List<SituationExchangeDeliveriesStructure> sxDeliveries) {
		Siri siri = new Siri();
		ServiceDelivery serviceDelivery = new ServiceDelivery();
		siri.setServiceDelivery(serviceDelivery);
		List<SituationExchangeDeliveryStructure> list = serviceDelivery
				.getSituationExchangeDelivery();
		SituationExchangeDeliveryStructure sxDeliveryStructure = new SituationExchangeDeliveryStructure();
		Situations situations = new Situations();
		List<PtSituationElementStructure> list2 = situations
				.getPtSituationElement();
		PtSituationElementStructure ptSituationElementStructure = new PtSituationElementStructure();
		list2.add(ptSituationElementStructure);
		DefaultedTextStructure detailText = new DefaultedTextStructure();
		detailText.setLang("EN");
		detailText.setValue("frobby morph");
		ptSituationElementStructure.setDetail(detailText);
		sxDeliveryStructure.setSituations(situations);
		list.add(sxDeliveryStructure);
		return siri;
	}

	@GET
	@Produces("application/json")
	public Response getCrewAssignments(
			@PathParam("serviceDate") String inputDateStr) {

		Response response = null;

		DateTimeFormatter dateDTF = ISODateTimeFormat.date();

		DateMidnight serviceDate = null;

		try {
			serviceDate = new DateMidnight(dateDTF.parseDateTime(inputDateStr));
		} catch (IllegalArgumentException e) {
			_log.debug(e.getMessage());
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}

		File inputFile = new File(System.getProperty("tdm.dataPath")
				+ System.getProperty("tdm.crewAssignFilename"));

		// First create a OperatorAssignmentData object
		UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(
				inputFile);

		OperatorAssignmentData data;
		try {
			data = process.generateDataObject();
		} catch (IOException e1) {
			_log.info("Exception loading data. Verify input file "
					+ inputFile.toString());
			_log.debug(e1.getMessage());
			throw new WebApplicationException(e1,
					Response.Status.INTERNAL_SERVER_ERROR);
		}

		List<OperatorAssignment> jsonOpAssigns = null;

		ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> tcipToJsonConverter = new OperatorAssignmentFromTcip();

		jsonOpAssigns = listConvertOpAssignTcipToJson(tcipToJsonConverter,
				data.getOperatorAssignmentsByServiceDate(serviceDate)); // grab
																		// the
																		// assigns
																		// for
																		// this
																		// date
																		// and
																		// convert
																		// to
																		// json

		OperatorAssignmentsMessage opAssignMessage = new OperatorAssignmentsMessage();
		opAssignMessage.setCrew(jsonOpAssigns);
		opAssignMessage.setStatus("OK");

		// Then I need to write it as json output.
		Gson gson = null;
		if (Boolean.parseBoolean(System.getProperty("tdm.prettyPrintOutput"))) {
			gson = new GsonBuilder()
					.setFieldNamingPolicy(
							FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
					.setPrettyPrinting().create();
		} else {
			gson = new GsonBuilder().setFieldNamingPolicy(
					FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
		}

		String output = gson.toJson(opAssignMessage);

		response = Response.ok(output).build();

		return response;

	}

	/*
	 * This method also exists in the same form in
	 * UtsCrewAssignsToJsonOutputProcess
	 */
	private List<OperatorAssignment> listConvertOpAssignTcipToJson(
			ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> conv,
			List<SCHOperatorAssignment> inputAssigns) {
		List<OperatorAssignment> outputAssigns = new ArrayList<OperatorAssignment>();

		Iterator<SCHOperatorAssignment> assignTcipIt = inputAssigns.iterator();

		while (assignTcipIt.hasNext()) {
			outputAssigns.add(conv.convert(assignTcipIt.next()));
		}

		return outputAssigns;
	}

	public SiriService getSiriService() {
		return _siriService;
	}

	public void setSiriService(SiriService _siriService) {
		this._siriService = _siriService;
	}

}
