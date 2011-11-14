package org.onebusaway.nyc.transit_data_manager.siri;

import java.io.StringReader;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveriesStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;

import com.sun.jersey.api.spring.Autowire;

@Path("/situation-exchange")
@Component
@Scope("request")
@Autowire
public class SituationExchangeResource {

	private static Logger _log = LoggerFactory
			.getLogger(SituationExchangeResource.class);

	private NycSiriService _siriService = new NycSiriService();

  private JAXBContext jc;

  public SituationExchangeResource() throws JAXBException {
    super();
    jc = JAXBContext.newInstance(Siri.class);
  }

	@POST
  @Produces("application/xml")
  @Consumes("application/xml")
  public Response handlePost(String body) throws JAXBException {
	_log.debug("---begin body---\n" + body + "\n---end body---");
	  _log.info("SituationExchangeResource.handlePost");
	  SituationExchangeResults result = new SituationExchangeResults();
		Unmarshaller u = jc.createUnmarshaller();
		Siri siri = (Siri) u.unmarshal(new StringReader(body));
		ServiceDelivery delivery = siri.getServiceDelivery();
		for (SituationExchangeDeliveryStructure s : delivery.getSituationExchangeDelivery()) {
		  SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
		  _siriService.handleServiceDelivery(delivery, s, ESiriModuleType.SITUATION_EXCHANGE, endpointDetails, result);
		}
		
		_log.info(result.toString());
		
		return Response.ok(result).build();
  }

	// TODO I don't believe this is needed any more but it may still be called by a test
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


	public NycSiriService getSiriService() {
		return _siriService;
	}

	public void setNycSiriService(NycSiriService _siriService) {
		this._siriService = _siriService;
	}

}
