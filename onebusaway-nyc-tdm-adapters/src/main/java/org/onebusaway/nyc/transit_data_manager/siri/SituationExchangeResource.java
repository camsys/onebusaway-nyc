/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_manager.util.NycEnvironment;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.ErrorDescriptionStructure;
import uk.org.siri.siri.LineRefStructure;
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


@Path("/situation-exchange")
@Component
@Scope("request")
public class SituationExchangeResource {

  static final boolean INCREMENTAL = true;

  private static Logger _log = LoggerFactory.getLogger(SituationExchangeResource.class);

  @Autowired
  NycSiriService _siriService;

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  @Autowired
  private ConfigurationService _configurationService;

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
      CancelledTripToSiriTransformer transformer = new CancelledTripToSiriTransformer(_nycTransitDataService, _configurationService, deliveryIsFromExternal(delivery));
      SituationExchangeResults result = new SituationExchangeResults();
      _siriService.handleServiceDeliveries(result, transformer.mergeImpactedAlerts(ensureDirections(incomingSiri.getServiceDelivery())));
      _log.info(result.toString());
      return Response.ok(result).build();
    }

    Siri responseSiri = new Siri();

    ServiceRequest serviceRequest = incomingSiri.getServiceRequest();

    if (serviceRequest != null && requestIsForThisEnvironment(serviceRequest, responseSiri)) {
      // deliver CAPI and traditional alerts as part of service request
      CancelledTripToSiriTransformer transformer = new CancelledTripToSiriTransformer(_nycTransitDataService, _configurationService, deliveryIsFromExternal(delivery));
      ServiceDelivery serviceDelivery = transformer.mergeImpactedAlerts(ensureDirections(incomingSiri.getServiceDelivery()));
      responseSiri.setServiceDelivery(serviceDelivery);
      _siriService.handleServiceRequests(serviceRequest, responseSiri);
    }

    SubscriptionRequest subscriptionRequests = incomingSiri.getSubscriptionRequest();
    if (subscriptionRequests != null && requestIsForThisEnvironment(subscriptionRequests, responseSiri))
      _siriService.handleSubscriptionRequests(subscriptionRequests, responseSiri);

    if (serviceRequest == null && subscriptionRequests == null) {
      _log.warn("Bad request from client, did not contain service delivery, service request, nor subscription request.");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    _log.info(responseSiri.toString());
    return Response.ok(responseSiri).build();
  }

  // for legacy reasons add directions to a route
  private ServiceDelivery ensureDirections(ServiceDelivery incomingSiriServiceDelivery) {
    if (incomingSiriServiceDelivery == null) return null;
    if (incomingSiriServiceDelivery.getSituationExchangeDelivery() == null) return incomingSiriServiceDelivery;
    for (SituationExchangeDeliveryStructure situationExchangeDeliveryStructure : incomingSiriServiceDelivery.getSituationExchangeDelivery()) {
      if (situationExchangeDeliveryStructure == null
            || situationExchangeDeliveryStructure.getSituations() == null
            || situationExchangeDeliveryStructure.getSituations().getPtSituationElement() == null) continue;
      for (PtSituationElementStructure ptSituationElementStructure : situationExchangeDeliveryStructure.getSituations().getPtSituationElement()) {
        if (ptSituationElementStructure == null) continue;
        boolean foundDirection = false;
        AffectsScopeStructure affects = ptSituationElementStructure.getAffects();
        if (affects == null) continue;
        for (AffectedVehicleJourneyStructure affectedVehicleJourneyStructure : affects.getVehicleJourneys().getAffectedVehicleJourney()) {
          if (affectedVehicleJourneyStructure.getDirectionRef() != null
            && affectedVehicleJourneyStructure.getDirectionRef().getValue() != null) {
            foundDirection = true;
          }
        }
        if (!foundDirection) {
          insertDirections(affects.getVehicleJourneys());
        }
      }
    }
    return incomingSiriServiceDelivery;
  }

  // for legacy reasons a LineRef needs DirectionRefs 0 and 1
  private void insertDirections(AffectsScopeStructure.VehicleJourneys vehicleJourneys) {
    List<AffectedVehicleJourneyStructure> afj = vehicleJourneys.getAffectedVehicleJourney();
    if (afj.isEmpty()) return;
    if (afj.size() == 1) {
      AffectedVehicleJourneyStructure direction0 = afj.get(0);
      if (direction0.getLineRef() != null) {
        direction0.setDirectionRef(new DirectionRefStructure());
        direction0.getDirectionRef().setValue("0");
        afj.add(new AffectedVehicleJourneyStructure());
        AffectedVehicleJourneyStructure direction1 = afj.get(1);
        direction1.setDirectionRef(new DirectionRefStructure());
        direction1.getDirectionRef().setValue("1");
        direction1.setLineRef(new LineRefStructure());
        direction1.getLineRef().setValue(
                direction0.getLineRef().getValue()
        );
      }
    }

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

  // if we were called from an external source, we are likely the TDM
  // in this configuration we perform additional operations/merges of data
  private boolean deliveryIsFromExternal(ServiceDelivery delivery) {
    if (delivery == null)
      return true;
    if (delivery.getProducerRef() == null)
      return true;
    return false;
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
