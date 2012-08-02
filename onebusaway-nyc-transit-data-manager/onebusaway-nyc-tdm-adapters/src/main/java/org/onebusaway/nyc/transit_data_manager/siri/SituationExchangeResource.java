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

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.transit_data_manager.util.NycEnvironment;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.ErrorDescriptionStructure;
import uk.org.siri.siri.ParticipantRefStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.ServiceDeliveryStructure.ErrorCondition;
import uk.org.siri.siri.ServiceRequest;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveriesStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.SubscriptionRequest;
import uk.org.siri.siri.SubscriptionResponseStructure;

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

  private NycEnvironment _environment = new NycEnvironment();

  public SituationExchangeResource() throws JAXBException {
    super();
    jc = JAXBContext.newInstance(Siri.class);
  }

  @GET
  public Response handleGet() {
    _log.info("GET received, initiating re-subscribe.");
    try {
      for (AgencyWithCoverageBean agency : _siriService.getTransitDataService().getAgenciesWithCoverage()) {
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

    if (delivery != null && deliveryIsForThisEnvironment(delivery)) {
      SituationExchangeResults result = new SituationExchangeResults();
      _siriService.handleServiceDeliveries(result, delivery);
      _log.info(result.toString());
      return Response.ok(result).build();
    }

    Siri responseSiri = new Siri();

    ServiceRequest serviceRequest = incomingSiri.getServiceRequest();

    if (serviceRequest != null && requestIsForThisEnvironment(serviceRequest, responseSiri))
      _siriService.handleServiceRequests(serviceRequest, responseSiri);

    SubscriptionRequest subscriptionRequests = incomingSiri.getSubscriptionRequest();
    if (subscriptionRequests != null && requestIsForThisEnvironment(subscriptionRequests, responseSiri))
      _siriService.handleSubscriptionRequests(subscriptionRequests, responseSiri);

    if (serviceRequest == null && subscriptionRequests == null) {
      _log.warn("Bad request from client, did not contain service delivery, service request, nor subscription request.");
      return Response.status(Status.BAD_REQUEST).build();
    }

    _log.info(responseSiri.toString());
    return Response.ok(responseSiri).build();
  }

  private boolean requestIsForThisEnvironment(SubscriptionRequest subscriptionRequest, Siri responseSiri) {
    if (subscriptionRequest == null)
      return true;
    CheckEnvironmentHandler checkEnvironment = checkEnvironment(subscriptionRequest.getRequestorRef());

    if (!checkEnvironment.isStatus()) {
      SubscriptionResponseStructure subResponse = new SubscriptionResponseStructure();
      subResponse.getResponseStatus().add(SiriHelper.createStatusResponseStructure(checkEnvironment.isStatus(), checkEnvironment.getMessage()));
      responseSiri.setSubscriptionResponse(subResponse);
    }
    return checkEnvironment.isStatus();
  }

  private boolean requestIsForThisEnvironment(ServiceRequest serviceRequest, Siri responseSiri) {
    if (serviceRequest == null)
      return true;
    CheckEnvironmentHandler checkEnvironment = checkEnvironment(serviceRequest.getRequestorRef());
    if (!checkEnvironment.isStatus()) {
      ServiceDelivery serviceDelivery = new ServiceDelivery();
      serviceDelivery.setStatus(false);
      ErrorCondition errorCondition = new ErrorCondition();
      ErrorDescriptionStructure errorDescriptionStructure = new ErrorDescriptionStructure();
      errorDescriptionStructure.setValue(checkEnvironment.getMessage());
      errorCondition.setDescription(errorDescriptionStructure );
      serviceDelivery.setErrorCondition(errorCondition);
      responseSiri.setServiceDelivery(serviceDelivery );
    }
    return checkEnvironment.isStatus();
  }

  private boolean deliveryIsForThisEnvironment(ServiceDelivery delivery) {
    if (delivery == null)
      return true;
    CheckEnvironmentHandler checkEnvironment = checkEnvironment(delivery.getProducerRef());
    return checkEnvironment.isStatus();
  }

  private CheckEnvironmentHandler checkEnvironment(ParticipantRefStructure participantRefStructure) {
    if (_environment.isUnknown()) {
      String message = "Local environment is unknown, processing.";
      _log.info(message);
      return new CheckEnvironmentHandler(true, message);
    }
    if (participantRefStructure == null) {
      String message = "Participant ref structure is null, processing.";
      _log.info(message);
      return new CheckEnvironmentHandler(true, message);
    }
    String incomingEnvironment = participantRefStructure.getValue();
    if (StringUtils.equals(incomingEnvironment, "unknown")) {
      String message = "Environment on incoming delivery is 'unknown', processing.";
      _log.info(message);
      return new CheckEnvironmentHandler(true, message);
    }
    if (!StringUtils.equals(incomingEnvironment, _environment.getEnvironment())) {
      String message = "Environment on incoming delivery '" + incomingEnvironment + "' does not equal local environment '"
          + _environment.getEnvironment() + "', discarding service delivery.";
      _log.info(message);
      return new CheckEnvironmentHandler(false, message);
    }
    return new CheckEnvironmentHandler(true, "ok");
  }

  // TODO I don't believe this is needed any more but it may still be called by
  // a test
  Siri generateSiriResponse(Date time, List<SituationExchangeDeliveriesStructure> sxDeliveries) {
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

  public void setEnvironment(NycEnvironment environment) {
    this._environment = environment;
  }
  
  public class CheckEnvironmentHandler {
    private boolean status;
    private String message;
    public CheckEnvironmentHandler(boolean status, String message) {
      this.status = status;
      this.message = message;
    }
    public boolean isStatus() {
      return status;
    }
    public void setStatus(boolean status) {
      this.status = status;
    }
    public String getMessage() {
      return message;
    }
    public void setMessage(String message) {
      this.message = message;
    }
  }



}
