/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.siri.AffectedApplicationStructure;
import org.onebusaway.siri.OneBusAwayAffects;
import org.onebusaway.siri.OneBusAwayAffectsStructure.Applications;
import org.onebusaway.siri.OneBusAwayConsequence;
import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TranslatedString;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts.TranslatedString.Translation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.AbstractServiceDeliveryStructure;
import uk.org.siri.siri.AffectedCallStructure;
import uk.org.siri.siri.AffectedOperatorStructure;
import uk.org.siri.siri.AffectedStopPointStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure.Calls;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.AffectsScopeStructure.Operators;
import uk.org.siri.siri.AffectsScopeStructure.StopPoints;
import uk.org.siri.siri.AffectsScopeStructure.VehicleJourneys;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.HalfOpenTimestampRangeStructure;
import uk.org.siri.siri.OperatorRefStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.ServiceRequest;
import uk.org.siri.siri.SeverityEnumeration;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.SituationExchangeRequestStructure;
import uk.org.siri.siri.SituationExchangeSubscriptionStructure;
import uk.org.siri.siri.StatusResponseStructure;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.SubscriptionQualifierStructure;
import uk.org.siri.siri.SubscriptionRequest;
import uk.org.siri.siri.SubscriptionResponseStructure;
import uk.org.siri.siri.VehicleJourneyRefStructure;
import uk.org.siri.siri.WorkflowStatusEnumeration;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Component
public class NycSiriService {

  private static final Logger _log = LoggerFactory.getLogger(NycSiriService.class);

  @Autowired
  private TransitDataService _transitDataService;

  private String _mode;

  private String _serviceAlertsUrl;

  private Map<String, ServiceAlertBean> currentServiceAlerts = new HashMap<String, ServiceAlertBean>();
  private List<ServiceAlertSubscription> serviceAlertSubscriptions = new ArrayList<ServiceAlertSubscription>();

  @PostConstruct
  public void setup() throws Exception {
    _log.info("setup(), mode is: " + _mode + ", serviceAlertsUrl is: "
        + _serviceAlertsUrl);
    if (_mode.equals("client")) {
      String result = sendSubscriptionAndServiceRequest();
      Siri siri = SiriXmlSerializer.fromXml(result);
      SituationExchangeResults handleResult = new SituationExchangeResults();
      handleServiceDeliveries(handleResult, siri.getServiceDelivery());
      // TODO Probably doesn't do the right thing.
      _log.info(handleResult.toString());
    }
  }

    public void handleServiceDeliveries(SituationExchangeResults result,
        ServiceDelivery delivery) {
    Set<String> incomingAgencies = collectAgencies(delivery);
    List<String> preAlertIds = getExistingAlertIds(incomingAgencies);
    for (SituationExchangeDeliveryStructure s : delivery.getSituationExchangeDelivery()) {
      SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
      handleServiceDelivery(delivery, s, ESiriModuleType.SITUATION_EXCHANGE,
          endpointDetails, result);
    }
    List<String> postAlertIds = getExistingAlertIds(incomingAgencies);
    // TODO This is not done yet.
    boolean deletedIds = Collections.disjoint(preAlertIds, postAlertIds);
    // org.apache.commons.collections.SetUtils.
  }

  private List<String> getExistingAlertIds(Set<String> agencies) {
    List<String> alertIds = new ArrayList<String>();
    for (String agency : agencies) {
      ListBean<ServiceAlertBean> alerts = _transitDataService.getAllServiceAlertsForAgencyId(agency);
      for (ServiceAlertBean alert : alerts.getList()) {
        alertIds.add(alert.getId());
      }
    }
    return alertIds;
  }

  private Set<String> collectAgencies(ServiceDelivery delivery) {
    Set<String> agencies = new HashSet<String>();
    for (SituationExchangeDeliveryStructure s : delivery.getSituationExchangeDelivery()) {
      Situations situations = s.getSituations();
      for (PtSituationElementStructure element : situations.getPtSituationElement()) {
        String situationId = element.getSituationNumber().getValue();
        AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
        agencies.add(id.getAgencyId());
      }
    }
    return agencies;
  }

  public synchronized void handleServiceDelivery(
      ServiceDelivery serviceDelivery,
      AbstractServiceDeliveryStructure deliveryForModule,
      ESiriModuleType moduleType, SiriEndpointDetails endpointDetails,
      SituationExchangeResults result) {

    handleSituationExchange(serviceDelivery,
        (SituationExchangeDeliveryStructure) deliveryForModule,
        endpointDetails, result);

  }

  private void handleSituationExchange(ServiceDelivery serviceDelivery,
      SituationExchangeDeliveryStructure sxDelivery,
      SiriEndpointDetails endpointDetails, SituationExchangeResults result) {

    DeliveryResult deliveryResult = new DeliveryResult();
    result.delivery.add(deliveryResult);

    Situations situations = sxDelivery.getSituations();

    if (situations == null)
      return;

    List<ServiceAlertBean> serviceAlertsToUpdate = new ArrayList<ServiceAlertBean>();
    List<String> serviceAlertIdsToRemove = new ArrayList<String>();

    for (PtSituationElementStructure ptSituation : situations.getPtSituationElement()) {

      ServiceAlertBean serviceAlertBean = getPtSituationAsServiceAlertBean(
          ptSituation, endpointDetails);

      WorkflowStatusEnumeration progress = ptSituation.getProgress();
      boolean remove = (progress != null && (progress == WorkflowStatusEnumeration.CLOSING || progress == WorkflowStatusEnumeration.CLOSED));

      if (remove) {
        serviceAlertIdsToRemove.add(serviceAlertBean.getId());
      } else {
        serviceAlertsToUpdate.add(serviceAlertBean);
      }
    }

    String defaultAgencyId = null;
    if (!CollectionsLibrary.isEmpty(endpointDetails.getDefaultAgencyIds()))
      defaultAgencyId = endpointDetails.getDefaultAgencyIds().get(0);

    for (ServiceAlertBean serviceAlertBean : serviceAlertsToUpdate) {
      // TODO Needs to be create or update, not just create
      getTransitDataService().createServiceAlert(defaultAgencyId,
          serviceAlertBean);
      result.countPtSituationElementResult(deliveryResult, serviceAlertBean,
          "added");
      // _serviceAlertsService.createOrUpdateServiceAlert(serviceAlert,
      // defaultAgencyId);
    }
    // _serviceAlertsService.removeServiceAlerts(serviceAlertIdsToRemove);
    for (String serviceAlertId : serviceAlertIdsToRemove) {
      // TODO Confirm this conversion
      getTransitDataService().removeServiceAlert(serviceAlertId);
      result.countPtSituationElementResult(deliveryResult, serviceAlertId,
          "removed");
    }
  }

  public void handleServiceRequests(ServiceRequest serviceRequest,
      Siri responseSiri) {
    List<SituationExchangeRequestStructure> requests = serviceRequest.getSituationExchangeRequest();
    for (SituationExchangeRequestStructure request : requests) {
      handleServiceRequest(request, responseSiri);
    }
  }

  private void handleServiceRequest(SituationExchangeRequestStructure request, Siri responseSiri) {
    ServiceAlertsHelper helper = new ServiceAlertsHelper();
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    helper.addSituationExchangeToSiri(serviceDelivery, getCurrentServiceAlerts());
    responseSiri.setServiceDelivery(serviceDelivery);
    return;
  }

  public void handleSubscriptionRequests(SubscriptionRequest subscriptionRequests, Siri responseSiri) {
    String address = subscriptionRequests.getAddress();
    List<SituationExchangeSubscriptionStructure> requests = subscriptionRequests.getSituationExchangeSubscriptionRequest();
    for (SituationExchangeSubscriptionStructure request : requests) {
      handleSubscriptionRequest(request, responseSiri, address);
    }
  }

  private void handleSubscriptionRequest(SituationExchangeSubscriptionStructure request, Siri responseSiri, String address) {
    boolean status = true;
    String errorMessage = null;
    String subscriptionRef = UUID.randomUUID().toString();
    try {
      ServiceAlertSubscription subscription = new ServiceAlertSubscription();
      subscription.setAddress(address);
      subscription.setCreatedAt(new Date());
      subscription.setSubscriptionIdentifier(request.getSubscriptionIdentifier().getValue());
      subscription.setSubscriptionRef(subscriptionRef);
      getServiceAlertSubscriptions().add(subscription);
    } catch (Exception e) {
      errorMessage = "Failed to create service alert subscription: " + e.getMessage();
      _log.error(errorMessage);
      status = false;
    }
    
    SubscriptionResponseStructure response = new SubscriptionResponseStructure();
    StatusResponseStructure statusResponseStructure = new StatusResponseStructure();
    statusResponseStructure.setStatus(status);
    if (errorMessage != null) {
      statusResponseStructure.getErrorCondition().getDescription().setValue(errorMessage);
    }
    SubscriptionQualifierStructure subscriptionQualifierStructure = new SubscriptionQualifierStructure();
    subscriptionQualifierStructure.setValue(subscriptionRef);
    statusResponseStructure.setSubscriptionRef(subscriptionQualifierStructure );
    response.getResponseStatus().add(statusResponseStructure );
    responseSiri.setSubscriptionResponse(response );
    return;
  }

  ServiceAlertBean getPtSituationAsServiceAlertBean(
      PtSituationElementStructure ptSituation,
      SiriEndpointDetails endpointDetails) {
    ServiceAlertBean serviceAlert = new ServiceAlertBean();
    EntryQualifierStructure serviceAlertNumber = ptSituation.getSituationNumber();
    String situationId = serviceAlertNumber.getValue();

    if (!endpointDetails.getDefaultAgencyIds().isEmpty()) {
      String agencyId = endpointDetails.getDefaultAgencyIds().get(0);
      // TODO Check this
      serviceAlert.setId(AgencyAndId.convertToString(new AgencyAndId(agencyId,
          situationId)));
    } else {
      AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
      serviceAlert.setId(AgencyAndId.convertToString(id));
    }

    handleDescriptions(ptSituation, serviceAlert);
    handleOtherFields(ptSituation, serviceAlert);
    // TODO not yet implemented
    // handlReasons(ptSituation, serviceAlert);
    handleAffects(ptSituation, serviceAlert);
    handleConsequences(ptSituation, serviceAlert);

    return serviceAlert;
  }

  // private ServiceAlert.Builder getPtSituationAsServiceAlert(
  // PtSituationElementStructure ptSituation,
  // SiriEndpointDetails endpointDetails) {
  //
  // ServiceAlert.Builder serviceAlert = ServiceAlert.newBuilder();
  // EntryQualifierStructure serviceAlertNumber =
  // ptSituation.getSituationNumber();
  // String situationId = serviceAlertNumber.getValue();
  //
  // if (!endpointDetails.getDefaultAgencyIds().isEmpty()) {
  // String agencyId = endpointDetails.getDefaultAgencyIds().get(0);
  // serviceAlert.setId(ServiceAlertLibrary.id(agencyId, situationId));
  // } else {
  // AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
  // serviceAlert.setId(ServiceAlertLibrary.id(id));
  // }
  //
  // handleDescriptions(ptSituation, serviceAlert);
  // handleOtherFields(ptSituation, serviceAlert);
  // handlReasons(ptSituation, serviceAlert);
  // handleAffects(ptSituation, serviceAlert);
  // handleConsequences(ptSituation, serviceAlert);
  //
  // return serviceAlert;
  // }
  //
  private void handleDescriptions(PtSituationElementStructure ptSituation,
      ServiceAlertBean serviceAlert) {
    TranslatedString summary = translation(ptSituation.getSummary());
    if (summary != null)
      serviceAlert.setSummaries(naturalLanguageStringBeanFromTranslatedString(summary));

    TranslatedString description = translation(ptSituation.getDescription());
    if (description != null)
      serviceAlert.setDescriptions(naturalLanguageStringBeanFromTranslatedString(description));
  }

  private List<NaturalLanguageStringBean> naturalLanguageStringBeanFromTranslatedString(
      TranslatedString translatedString) {
    List<NaturalLanguageStringBean> nlsb = new ArrayList<NaturalLanguageStringBean>();
    for (Translation t : translatedString.getTranslationList()) {
      nlsb.add(new NaturalLanguageStringBean(t.getText(), t.getLanguage()));
    }
    return nlsb;
  }

  // private void handleDescriptions(PtSituationElementStructure ptSituation,
  // ServiceAlert.Builder serviceAlert) {
  //
  // TranslatedString summary = translation(ptSituation.getSummary());
  // if (summary != null)
  // serviceAlert.setSummary(summary);
  //
  // TranslatedString description = translation(ptSituation.getDescription());
  // if (description != null)
  // serviceAlert.setDescription(description);
  // }
  //
  private void handleOtherFields(PtSituationElementStructure ptSituation,
      ServiceAlertBean serviceAlert) {

    SeverityEnumeration severity = ptSituation.getSeverity();
    if (severity != null) {
      ESeverity severityEnum = ESeverity.valueOfTpegCode(severity.value());
      serviceAlert.setSeverity(severityEnum);
    }

    if (ptSituation.getPublicationWindow() != null) {
      HalfOpenTimestampRangeStructure window = ptSituation.getPublicationWindow();
      TimeRangeBean range = new TimeRangeBean();
      if (window.getStartTime() != null)
        range.setFrom(window.getStartTime().getTime());
      if (window.getEndTime() != null)
        range.setTo(window.getEndTime().getTime());
      if (range.getFrom() != 0 || range.getTo() != 0)
        serviceAlert.setPublicationWindows(Arrays.asList(range));
    }
  }

  // private void handleOtherFields(PtSituationElementStructure ptSituation,
  // ServiceAlert.Builder serviceAlert) {
  //
  // SeverityEnumeration severity = ptSituation.getSeverity();
  // if (severity != null) {
  // ESeverity severityEnum = ESeverity.valueOfTpegCode(severity.value());
  // serviceAlert.setSeverity(ServiceAlertLibrary.convertSeverity(severityEnum));
  // }
  //
  // if (ptSituation.getPublicationWindow() != null) {
  // HalfOpenTimestampRangeStructure window =
  // ptSituation.getPublicationWindow();
  // TimeRange.Builder range = TimeRange.newBuilder();
  // if (window.getStartTime() != null)
  // range.setStart(window.getStartTime().getTime());
  // if (window.getEndTime() != null)
  // range.setEnd(window.getEndTime().getTime());
  // if (range.hasStart() || range.hasEnd())
  // serviceAlert.addPublicationWindow(range);
  // }
  // }
  //
  @SuppressWarnings("unused")
  private void handlReasons(PtSituationElementStructure ptSituation,
      ServiceAlertBean serviceAlert) {
    throw new RuntimeException("handleReasons not implemented");
  }

  // private void handlReasons(PtSituationElementStructure ptSituation,
  // ServiceAlert.Builder serviceAlert) {
  //
  // Cause cause = getReasonAsCause(ptSituation);
  // if (cause != null)
  // serviceAlert.setCause(cause);
  // }
  //
  // private Cause getReasonAsCause(PtSituationElementStructure ptSituation) {
  // if (ptSituation.getEnvironmentReason() != null)
  // return Cause.WEATHER;
  // if (ptSituation.getEquipmentReason() != null) {
  // switch (ptSituation.getEquipmentReason()) {
  // case CONSTRUCTION_WORK:
  // return Cause.CONSTRUCTION;
  // case CLOSED_FOR_MAINTENANCE:
  // case MAINTENANCE_WORK:
  // case EMERGENCY_ENGINEERING_WORK:
  // case LATE_FINISH_TO_ENGINEERING_WORK:
  // case REPAIR_WORK:
  // return Cause.MAINTENANCE;
  // default:
  // return Cause.TECHNICAL_PROBLEM;
  // }
  // }
  // if (ptSituation.getPersonnelReason() != null) {
  // switch (ptSituation.getPersonnelReason()) {
  // case INDUSTRIAL_ACTION:
  // case UNOFFICIAL_INDUSTRIAL_ACTION:
  // return Cause.STRIKE;
  // }
  // return Cause.OTHER_CAUSE;
  // }
  // /**
  // * There are really so many possibilities here that it's tricky to translate
  // * them all
  // */
  // if (ptSituation.getMiscellaneousReason() != null) {
  // switch (ptSituation.getMiscellaneousReason()) {
  // case ACCIDENT:
  // case COLLISION:
  // return Cause.ACCIDENT;
  // case DEMONSTRATION:
  // case MARCH:
  // return Cause.DEMONSTRATION;
  // case PERSON_ILL_ON_VEHICLE:
  // case FATALITY:
  // return Cause.MEDICAL_EMERGENCY;
  // case POLICE_REQUEST:
  // case BOMB_ALERT:
  // case CIVIL_EMERGENCY:
  // case EMERGENCY_SERVICES:
  // case EMERGENCY_SERVICES_CALL:
  // return Cause.POLICE_ACTIVITY;
  // }
  // }
  //
  // return null;
  // }

  /****
   * Affects
   ****/

  private void handleAffects(PtSituationElementStructure ptSituation,
      ServiceAlertBean serviceAlert) {
    AffectsScopeStructure affectsStructure = ptSituation.getAffects();

    if (affectsStructure == null)
      return;

    List<SituationAffectsBean> allAffects = new ArrayList<SituationAffectsBean>();

    Operators operators = affectsStructure.getOperators();

    if (operators != null
        && !CollectionsLibrary.isEmpty(operators.getAffectedOperator())) {

      for (AffectedOperatorStructure operator : operators.getAffectedOperator()) {
        OperatorRefStructure operatorRef = operator.getOperatorRef();
        if (operatorRef == null || operatorRef.getValue() == null)
          continue;
        String agencyId = operatorRef.getValue();
        SituationAffectsBean sab = new SituationAffectsBean();
        sab.setAgencyId(agencyId);
        allAffects.add(sab);
      }
    }

    StopPoints stopPoints = affectsStructure.getStopPoints();

    if (stopPoints != null
        && !CollectionsLibrary.isEmpty(stopPoints.getAffectedStopPoint())) {

      for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoint()) {
        StopPointRefStructure stopRef = stopPoint.getStopPointRef();
        if (stopRef == null || stopRef.getValue() == null)
          continue;
        SituationAffectsBean sab = new SituationAffectsBean();
        sab.setStopId(stopRef.getValue());
        allAffects.add(sab);
      }
    }

    VehicleJourneys vjs = affectsStructure.getVehicleJourneys();
    if (vjs != null
        && !CollectionsLibrary.isEmpty(vjs.getAffectedVehicleJourney())) {

      for (AffectedVehicleJourneyStructure vj : vjs.getAffectedVehicleJourney()) {

        SituationAffectsBean sab = new SituationAffectsBean();

        if (vj.getLineRef() != null) {
          sab.setRouteId(vj.getLineRef().getValue());
        }

        if (vj.getDirectionRef() != null)
          sab.setDirectionId(vj.getDirectionRef().getValue());

        List<VehicleJourneyRefStructure> tripRefs = vj.getVehicleJourneyRef();
        Calls stopRefs = vj.getCalls();

        boolean hasTripRefs = !CollectionsLibrary.isEmpty(tripRefs);
        boolean hasStopRefs = stopRefs != null
            && !CollectionsLibrary.isEmpty(stopRefs.getCall());

        if (!(hasTripRefs || hasStopRefs)) {
          if (sab.getRouteId() != null && !sab.getRouteId().isEmpty()) {
            allAffects.add(sab);
          }
        } else if (hasTripRefs && hasStopRefs) {
          for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
            sab.setTripId(vjRef.getValue());
            for (AffectedCallStructure call : stopRefs.getCall()) {
              sab.setStopId(call.getStopPointRef().getValue());
              allAffects.add(sab);
            }
          }
        } else if (hasTripRefs) {
          for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
            sab.setTripId(vjRef.getValue());
            allAffects.add(sab);
          }
        } else {
          for (AffectedCallStructure call : stopRefs.getCall()) {
            sab.setStopId(call.getStopPointRef().getValue());
            allAffects.add(sab);
          }
        }
      }
    }

    ExtensionsStructure extension = affectsStructure.getExtensions();
    if (extension != null && extension.getAny() != null) {
      Object ext = extension.getAny();
      if (ext instanceof OneBusAwayAffects) {
        OneBusAwayAffects obaAffects = (OneBusAwayAffects) ext;

        Applications applications = obaAffects.getApplications();
        if (applications != null
            && !CollectionsLibrary.isEmpty(applications.getAffectedApplication())) {

          List<AffectedApplicationStructure> apps = applications.getAffectedApplication();

          for (AffectedApplicationStructure sApp : apps) {
            SituationAffectsBean sab = new SituationAffectsBean();
            sab.setApplicationId(sApp.getApiKey());
            allAffects.add(sab);
          }
        }
      }
    }

    if (!allAffects.isEmpty())
      serviceAlert.setAllAffects(allAffects);
  }

  // private void handleAffects(PtSituationElementStructure ptSituation,
  // ServiceAlert.Builder serviceAlert) {
  //
  // AffectsScopeStructure affectsStructure = ptSituation.getAffects();
  //
  // if (affectsStructure == null)
  // return;
  //
  // Operators operators = affectsStructure.getOperators();
  //
  // if (operators != null
  // && !CollectionsLibrary.isEmpty(operators.getAffectedOperator())) {
  //
  // for (AffectedOperatorStructure operator : operators.getAffectedOperator())
  // {
  // OperatorRefStructure operatorRef = operator.getOperatorRef();
  // if (operatorRef == null || operatorRef.getValue() == null)
  // continue;
  // String agencyId = operatorRef.getValue();
  // Affects.Builder affects = Affects.newBuilder();
  // affects.setAgencyId(agencyId);
  // serviceAlert.addAffects(affects);
  // }
  // }
  //
  // StopPoints stopPoints = affectsStructure.getStopPoints();
  //
  // if (stopPoints != null
  // && !CollectionsLibrary.isEmpty(stopPoints.getAffectedStopPoint())) {
  //
  // for (AffectedStopPointStructure stopPoint :
  // stopPoints.getAffectedStopPoint()) {
  // StopPointRefStructure stopRef = stopPoint.getStopPointRef();
  // if (stopRef == null || stopRef.getValue() == null)
  // continue;
  // AgencyAndId stopId =
  // AgencyAndIdLibrary.convertFromString(stopRef.getValue());
  // Id id = ServiceAlertLibrary.id(stopId);
  // Affects.Builder affects = Affects.newBuilder();
  // affects.setStopId(id);
  // serviceAlert.addAffects(affects);
  // }
  // }
  //
  // VehicleJourneys vjs = affectsStructure.getVehicleJourneys();
  // if (vjs != null
  // && !CollectionsLibrary.isEmpty(vjs.getAffectedVehicleJourney())) {
  //
  // for (AffectedVehicleJourneyStructure vj : vjs.getAffectedVehicleJourney())
  // {
  //
  // Affects.Builder affects = Affects.newBuilder();
  // if (vj.getLineRef() != null) {
  // AgencyAndId routeId =
  // AgencyAndIdLibrary.convertFromString(vj.getLineRef().getValue());
  // Id id = ServiceAlertLibrary.id(routeId);
  // affects.setRouteId(id);
  // }
  //
  // if (vj.getDirectionRef() != null)
  // affects.setDirectionId(vj.getDirectionRef().getValue());
  //
  // List<VehicleJourneyRefStructure> tripRefs = vj.getVehicleJourneyRef();
  // Calls stopRefs = vj.getCalls();
  //
  // boolean hasTripRefs = !CollectionsLibrary.isEmpty(tripRefs);
  // boolean hasStopRefs = stopRefs != null
  // && !CollectionsLibrary.isEmpty(stopRefs.getCall());
  //
  // if (!(hasTripRefs || hasStopRefs)) {
  // if (affects.hasRouteId())
  // serviceAlert.addAffects(affects);
  // } else if (hasTripRefs && hasStopRefs) {
  // for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
  // AgencyAndId tripId =
  // AgencyAndIdLibrary.convertFromString(vjRef.getValue());
  // affects.setTripId(ServiceAlertLibrary.id(tripId));
  // for (AffectedCallStructure call : stopRefs.getCall()) {
  // AgencyAndId stopId =
  // AgencyAndIdLibrary.convertFromString(call.getStopPointRef().getValue());
  // affects.setStopId(ServiceAlertLibrary.id(stopId));
  // serviceAlert.addAffects(affects);
  // }
  // }
  // } else if (hasTripRefs) {
  // for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
  // AgencyAndId tripId =
  // AgencyAndIdLibrary.convertFromString(vjRef.getValue());
  // affects.setTripId(ServiceAlertLibrary.id(tripId));
  // serviceAlert.addAffects(affects);
  // }
  // } else {
  // for (AffectedCallStructure call : stopRefs.getCall()) {
  // AgencyAndId stopId =
  // AgencyAndIdLibrary.convertFromString(call.getStopPointRef().getValue());
  // affects.setStopId(ServiceAlertLibrary.id(stopId));
  // serviceAlert.addAffects(affects);
  // }
  // }
  // }
  // }
  //
  // ExtensionsStructure extension = affectsStructure.getExtensions();
  // if (extension != null && extension.getAny() != null) {
  // Object ext = extension.getAny();
  // if (ext instanceof OneBusAwayAffects) {
  // OneBusAwayAffects obaAffects = (OneBusAwayAffects) ext;
  //
  // Applications applications = obaAffects.getApplications();
  // if (applications != null
  // && !CollectionsLibrary.isEmpty(applications.getAffectedApplication())) {
  //
  // List<AffectedApplicationStructure> apps =
  // applications.getAffectedApplication();
  //
  // for (AffectedApplicationStructure sApp : apps) {
  // Affects.Builder affects = Affects.newBuilder();
  // affects.setApplicationId(sApp.getApiKey());
  // serviceAlert.addAffects(affects);
  // }
  // }
  // }
  // }
  // }

  private void handleConsequences(PtSituationElementStructure ptSituation,
      ServiceAlertBean serviceAlert) {

    List<SituationConsequenceBean> consequencesList = new ArrayList<SituationConsequenceBean>();

    PtConsequencesStructure consequences = ptSituation.getConsequences();

    if (consequences == null || consequences.getConsequence() == null)
      return;

    for (PtConsequenceStructure consequence : consequences.getConsequence()) {
      SituationConsequenceBean situationConsequenceBean = new SituationConsequenceBean();

      if (consequence.getCondition() != null)
        situationConsequenceBean.setEffect(getConditionAsEffect(consequence.getCondition()));
      ExtensionsStructure extensions = consequence.getExtensions();
      if (extensions != null) {
        Object obj = extensions.getAny();
        if (obj instanceof OneBusAwayConsequence) {
          OneBusAwayConsequence obaConsequence = (OneBusAwayConsequence) obj;
          if (obaConsequence.getDiversionPath() != null)
            situationConsequenceBean.setDetourPath(obaConsequence.getDiversionPath());
        }
      }
      if (situationConsequenceBean.getDetourPath() != null
          || situationConsequenceBean.getEffect() != null)
        consequencesList.add(situationConsequenceBean);
    }

    if (!consequencesList.isEmpty())
      serviceAlert.setConsequences(consequencesList);
  }

  // private void handleConsequences(PtSituationElementStructure ptSituation,
  // ServiceAlert.Builder serviceAlert) {
  //
  // PtConsequencesStructure consequences = ptSituation.getConsequences();
  //
  // if (consequences == null || consequences.getConsequence() == null)
  // return;
  //
  // for (PtConsequenceStructure consequence : consequences.getConsequence()) {
  // Consequence.Builder builder = Consequence.newBuilder();
  // if (consequence.getCondition() != null)
  // builder.setEffect(getConditionAsEffect(consequence.getCondition()));
  // ExtensionsStructure extensions = consequence.getExtensions();
  // if (extensions != null) {
  // Object obj = extensions.getAny();
  // if (obj instanceof OneBusAwayConsequence) {
  // OneBusAwayConsequence obaConsequence = (OneBusAwayConsequence) obj;
  // if (obaConsequence.getDiversionPath() != null)
  // builder.setDetourPath(obaConsequence.getDiversionPath());
  // }
  // }
  // if (builder.hasDetourPath() || builder.hasEffect())
  // serviceAlert.addConsequence(builder);
  // }
  // }

  private EEffect getConditionAsEffect(ServiceConditionEnumeration condition) {
    switch (condition) {

      case CANCELLED:
      case NO_SERVICE:
        return EEffect.NO_SERVICE;

      case DELAYED:
        return EEffect.SIGNIFICANT_DELAYS;

      case DIVERTED:
        return EEffect.DETOUR;

      case ADDITIONAL_SERVICE:
      case EXTENDED_SERVICE:
      case SHUTTLE_SERVICE:
      case SPECIAL_SERVICE:
      case REPLACEMENT_SERVICE:
        return EEffect.ADDITIONAL_SERVICE;

      case DISRUPTED:
      case INTERMITTENT_SERVICE:
      case SHORT_FORMED_SERVICE:
        return EEffect.REDUCED_SERVICE;

      case ALTERED:
      case ARRIVES_EARLY:
      case REPLACEMENT_TRANSPORT:
      case SPLITTING_TRAIN:
        return EEffect.MODIFIED_SERVICE;

      case ON_TIME:
      case FULL_LENGTH_SERVICE:
      case NORMAL_SERVICE:
        return EEffect.OTHER_EFFECT;

      case UNDEFINED_SERVICE_INFORMATION:
      case UNKNOWN:
        return EEffect.UNKNOWN_EFFECT;

      default:
        _log.warn("unknown condition: " + condition);
        return EEffect.UNKNOWN_EFFECT;
    }
  }

  // private Effect getConditionAsEffect(ServiceConditionEnumeration condition)
  // {
  // switch (condition) {
  //
  // case CANCELLED:
  // case NO_SERVICE:
  // return Effect.NO_SERVICE;
  //
  // case DELAYED:
  // return Effect.SIGNIFICANT_DELAYS;
  //
  // case DIVERTED:
  // return Effect.DETOUR;
  //
  // case ADDITIONAL_SERVICE:
  // case EXTENDED_SERVICE:
  // case SHUTTLE_SERVICE:
  // case SPECIAL_SERVICE:
  // case REPLACEMENT_SERVICE:
  // return Effect.ADDITIONAL_SERVICE;
  //
  // case DISRUPTED:
  // case INTERMITTENT_SERVICE:
  // case SHORT_FORMED_SERVICE:
  // return Effect.REDUCED_SERVICE;
  //
  // case ALTERED:
  // case ARRIVES_EARLY:
  // case REPLACEMENT_TRANSPORT:
  // case SPLITTING_TRAIN:
  // return Effect.MODIFIED_SERVICE;
  //
  // case ON_TIME:
  // case FULL_LENGTH_SERVICE:
  // case NORMAL_SERVICE:
  // return Effect.OTHER_EFFECT;
  //
  // case UNDEFINED_SERVICE_INFORMATION:
  // case UNKNOWN:
  // return Effect.UNKNOWN_EFFECT;
  //
  // default:
  // _log.warn("unknown condition: " + condition);
  // return Effect.UNKNOWN_EFFECT;
  // }
  // }

  private TranslatedString translation(DefaultedTextStructure text) {
    if (text == null)
      return null;
    String value = text.getValue();
    if (value == null)
      return null;

    Translation.Builder translation = Translation.newBuilder();
    translation.setText(value);
    if (text.getLang() != null)
      translation.setLanguage(text.getLang());

    TranslatedString.Builder tsBuilder = TranslatedString.newBuilder();
    tsBuilder.addTranslation(translation);
    return tsBuilder.build();
  }

  String sendSubscriptionAndServiceRequest() throws Exception {
    Siri siri = createSubsAndSxRequest();
    String sendResult = post(SiriXmlSerializer.getXml(siri), _serviceAlertsUrl);
    return sendResult;
  }

  Siri createSubsAndSxRequest() throws Exception {
    Siri siri = createSubscriptionRequest();
    addSituationExchangeRequest(siri);
    return siri;
  }

  Siri createSubscriptionRequest() throws Exception {
    Siri siri = new Siri();
    SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
    // TODO I think this needs to be changed, no?
    subscriptionRequest.setAddress("http://localhost/foo/bar");
    siri.setSubscriptionRequest(subscriptionRequest);
    List<SituationExchangeSubscriptionStructure> exchangeSubscriptionRequests = subscriptionRequest.getSituationExchangeSubscriptionRequest();
    SituationExchangeSubscriptionStructure situationExchangeSubscriptionStructure = new SituationExchangeSubscriptionStructure();;
    exchangeSubscriptionRequests.add(situationExchangeSubscriptionStructure);
    SituationExchangeRequestStructure situationExchangeRequestStructure = new SituationExchangeRequestStructure();
    situationExchangeSubscriptionStructure.setSituationExchangeRequest(situationExchangeRequestStructure);
    situationExchangeRequestStructure.setRequestTimestamp(new Date());

    return siri;
  }

  public String post(String siri, String tdm) {
    String postResult = "";
    // _log.debug("result=" + siri);
    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource r = client.resource(tdm);
    postResult = r.accept(MediaType.APPLICATION_XML_TYPE).type(
        MediaType.APPLICATION_XML_TYPE).post(String.class, siri);
    // _log.debug("postResult=" + postResult);
    return postResult;
  }

  private void addSituationExchangeRequest(Siri siri) {
    ServiceRequest serviceRequest = new ServiceRequest();
    siri.setServiceRequest(serviceRequest);
    List<SituationExchangeRequestStructure> situationExchangeRequest = serviceRequest.getSituationExchangeRequest();
    SituationExchangeRequestStructure situationExchangeRequestStructure = new SituationExchangeRequestStructure();
    situationExchangeRequest.add(situationExchangeRequestStructure);
    situationExchangeRequestStructure.setRequestTimestamp(new Date());
  }

  public TransitDataService getTransitDataService() {
    return _transitDataService;
  }

  public void setTransitDataService(TransitDataService _transitDataService) {
    this._transitDataService = _transitDataService;
  }

  public String getMode() {
    return _mode;
  }

  public void setMode(String mode) {
    this._mode = mode;
  }

  public String getServiceAlertsUrl() {
    return _serviceAlertsUrl;
  }

  public void setServiceAlertsUrl(String _serviceAlertsUrl) {
    this._serviceAlertsUrl = _serviceAlertsUrl;
  }

  public Map<String, ServiceAlertBean> getCurrentServiceAlerts() {
    return currentServiceAlerts;
  }

  public void setCurrentServiceAlerts(Map<String, ServiceAlertBean> currentServiceAlerts) {
    this.currentServiceAlerts = currentServiceAlerts;
  }

  public List<ServiceAlertSubscription> getServiceAlertSubscriptions() {
    return serviceAlertSubscriptions;
  }

  public void setServiceAlertSubscriptions(List<ServiceAlertSubscription> serviceAlertSubscriptions) {
    this.serviceAlertSubscriptions = serviceAlertSubscriptions;
  }

}
