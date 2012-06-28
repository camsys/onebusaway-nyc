package org.onebusaway.nyc.transit_data_manager.siri;

import java.io.StringReader;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.ServiceRequest;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveriesStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.SubscriptionRequest;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.spring.Autowire;

@Path("/situation-exchange")
@Component
@Scope("request")
@Autowire
public class SituationExchangeResource {

  static final boolean INCREMENTAL = true;

  private static Logger _log = LoggerFactory.getLogger(SituationExchangeResource.class);

  @Autowired
  NycSiriService _siriService;

  private JAXBContext jc;

  public SituationExchangeResource() throws JAXBException {
    super();
    jc = JAXBContext.newInstance(Siri.class);
  }

  @GET
  public Response handleGet() {
    _log.info("GET received, initiating re-subscribe.");
    try {
      for (AgencyWithCoverageBean agency: _siriService.getTransitDataService().getAgenciesWithCoverage()) {
        _log.info("Clearing service alerts for agency " + agency.getAgency().getId());
        _siriService.getTransitDataService().removeAllServiceAlertsForAgencyId(agency.getAgency().getId());
      }
      _log.info("Sending request.");
      _siriService.sendAndProcessSubscriptionAndServiceRequest();
    } catch (Exception e) {
      String message = "Re-subscribe failed: " + e.getMessage();
      _log.error(message);
      return Response.ok(message).build();
    }
    return Response.ok("Re-subscribed\n").build();
  }
  
  @POST
  @Produces("application/xml")
  @Consumes("application/xml")
  public Response handlePost(String body) throws Exception {
    return handleRequest(body);
  }

  public Response handleRequest(String body) throws JAXBException, Exception {
    _log.info("SituationExchangeResource.handlePost");
    _log.debug("---begin body---\n" + body + "\n---end body---");

    Unmarshaller u = jc.createUnmarshaller();
    Siri incomingSiri = (Siri) u.unmarshal(new StringReader(body));

    ServiceDelivery delivery = incomingSiri.getServiceDelivery();
    if (delivery != null) {
      SituationExchangeResults result = new SituationExchangeResults();
      _siriService.handleServiceDeliveries(result, delivery);
      _log.info(result.toString());
      return Response.ok(result).build();
    }

    Siri responseSiri = new Siri();

    ServiceRequest serviceRequest = incomingSiri.getServiceRequest();
    if (serviceRequest != null)
      _siriService.handleServiceRequests(serviceRequest, responseSiri);

    SubscriptionRequest subscriptionRequests = incomingSiri.getSubscriptionRequest();
    if (subscriptionRequests != null)
      _siriService.handleSubscriptionRequests(subscriptionRequests,
          responseSiri);

    if (serviceRequest == null && subscriptionRequests == null) {
      _log.warn("Bad request from client, did not contain service delivery, service request, nor subscription request.");
      return Response.status(Status.BAD_REQUEST).build();
    }

    _log.info(responseSiri.toString());
    return Response.ok(responseSiri).build();
  }

  // TODO I don't believe this is needed any more but it may still be called by
  // a test
  Siri generateSiriResponse(Date time,
      List<SituationExchangeDeliveriesStructure> sxDeliveries) {
    Siri siri = new Siri();
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    siri.setServiceDelivery(serviceDelivery);
    List<SituationExchangeDeliveryStructure> list = serviceDelivery.getSituationExchangeDelivery();
    SituationExchangeDeliveryStructure sxDeliveryStructure = new SituationExchangeDeliveryStructure();
    Situations situations = new Situations();
    List<PtSituationElementStructure> list2 = situations.getPtSituationElement();
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

  public NycSiriService getNycSiriService() {
    return _siriService;
  }

  public void setNycSiriService(NycSiriService _siriService) {
    this._siriService = _siriService;
  }

}
