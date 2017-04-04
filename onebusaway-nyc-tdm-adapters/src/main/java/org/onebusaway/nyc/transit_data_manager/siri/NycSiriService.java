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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.siri.support.SiriXmlSerializer;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_manager.util.NycEnvironment;
import org.onebusaway.siri.core.ESiriModuleType;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.AbstractServiceDeliveryStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.ServiceRequest;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.SituationExchangeRequestStructure;
import uk.org.siri.siri.SituationExchangeSubscriptionStructure;
import uk.org.siri.siri.StatusResponseStructure;
import uk.org.siri.siri.SubscriptionQualifierStructure;
import uk.org.siri.siri.SubscriptionRequest;
import uk.org.siri.siri.SubscriptionResponseStructure;
import uk.org.siri.siri.WorkflowStatusEnumeration;

import static org.onebusaway.nyc.transit_data_manager.util.NycSiriUtil.getPtSituationAsServiceAlertBean;

@SuppressWarnings("restriction")
@Component
public abstract class NycSiriService {

  static final Logger _log = LoggerFactory.getLogger(NycSiriService.class);

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  private String _serviceAlertsUrl;

  private String _subscriptionPath;

  private WebResourceWrapper _webResourceWrapper;

  private String _subscriptionUrl;

  private SiriXmlSerializer _siriXmlSerializer = new SiriXmlSerializer();

  protected NycEnvironment _environment = new NycEnvironment();

  abstract void setupForMode() throws Exception, JAXBException;

  abstract List<String> getExistingAlertIds(Set<String> agencies);

  abstract void removeServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, String serviceAlertId);

  abstract void addOrUpdateServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, ServiceAlertBean serviceAlertBean,
      String defaultAgencyId);

  abstract void postServiceDeliveryActions(SituationExchangeResults result,
      Collection<String> deletedIds) throws Exception;

  abstract void addSubscription(ServiceAlertSubscription subscription);

  abstract public List<ServiceAlertSubscription> getActiveServiceAlertSubscriptions();

  abstract public SiriServicePersister getPersister();

  abstract public void setPersister(SiriServicePersister _siriServicePersister);

  abstract public void deleteAllServiceAlerts();

  @PostConstruct
  public void setup() {
    _log.info("setup(), serviceAlertsUrl is: " + _serviceAlertsUrl);
    try {
      setupForMode();
    } catch (Exception e) {
      _log.error("********************\n"
          + "NycSiriService failed to start, message is: " + e.getMessage()
          + "\n********************");
    }
  }

  public void handleServiceDeliveries(SituationExchangeResults result,
      ServiceDelivery delivery) throws Exception {
    Set<String> incomingAgencies = collectAgencies(delivery);
    List<String> preAlertIds = getExistingAlertIds(incomingAgencies);

    for (SituationExchangeDeliveryStructure s : delivery.getSituationExchangeDelivery()) {
      SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
      handleServiceDelivery(delivery, s, ESiriModuleType.SITUATION_EXCHANGE,
          endpointDetails, result, preAlertIds);
    }

    List<String> postAlertIds = getExistingAlertIds(incomingAgencies);
    @SuppressWarnings("unchecked")
    Collection<String> deletedIds = CollectionUtils.subtract(preAlertIds,
        postAlertIds);
    postServiceDeliveryActions(result, deletedIds);

  }

  private Set<String> collectAgencies(ServiceDelivery delivery) {
    Set<String> agencies = new HashSet<String>();
    for (SituationExchangeDeliveryStructure s : delivery.getSituationExchangeDelivery()) {
      Situations situations = s.getSituations();
      for (PtSituationElementStructure element : situations.getPtSituationElement()) {
        String situationId = element.getSituationNumber().getValue();
        AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
        if (id != null)
          agencies.add(id.getAgencyId());
      }
    }
    return agencies;
  }

  public synchronized void handleServiceDelivery(
      ServiceDelivery serviceDelivery,
      AbstractServiceDeliveryStructure deliveryForModule,
      ESiriModuleType moduleType, SiriEndpointDetails endpointDetails,
      SituationExchangeResults result, List<String> preAlertIds) {

    handleSituationExchange(serviceDelivery,
        (SituationExchangeDeliveryStructure) deliveryForModule,
        endpointDetails, result, preAlertIds);
  }

  void handleSituationExchange(ServiceDelivery serviceDelivery,
      SituationExchangeDeliveryStructure sxDelivery,
      SiriEndpointDetails endpointDetails, SituationExchangeResults result,
      List<String> preAlertIds) {

    DeliveryResult deliveryResult = new DeliveryResult();
    result.getDelivery().add(deliveryResult);

    Situations situations = sxDelivery.getSituations();

    if (situations == null)
      return;

    List<ServiceAlertBean> serviceAlertsToUpdate = new ArrayList<ServiceAlertBean>();
    List<String> serviceAlertIdsToRemove = new ArrayList<String>();

    deleteAllServiceAlerts();

    for (PtSituationElementStructure ptSituation : situations.getPtSituationElement()) {

      ServiceAlertBean serviceAlertBean = getPtSituationAsServiceAlertBean(
          ptSituation, endpointDetails);

      String id = serviceAlertBean.getId();
      if (StringUtils.isEmpty(id)) {
        _log.warn("Service alert has no id, discarding.");
        continue;
      }
      WorkflowStatusEnumeration progress = ptSituation.getProgress();
      boolean remove = (progress != null && (progress == WorkflowStatusEnumeration.CLOSING || progress == WorkflowStatusEnumeration.CLOSED));

      if (remove) {
        serviceAlertIdsToRemove.add(id);
      } else {
        serviceAlertsToUpdate.add(serviceAlertBean);
        preAlertIds.remove(id);
      }

    }

    for (String id: preAlertIds) {
      serviceAlertIdsToRemove.add(id);
    }
    
    String defaultAgencyId = null;
    if (!CollectionsLibrary.isEmpty(endpointDetails.getDefaultAgencyIds()))
      defaultAgencyId = endpointDetails.getDefaultAgencyIds().get(0);

    for (ServiceAlertBean serviceAlertBean : serviceAlertsToUpdate) {
      addOrUpdateServiceAlert(result, deliveryResult, serviceAlertBean,
          defaultAgencyId);
    }
    for (String serviceAlertId : serviceAlertIdsToRemove) {
      removeServiceAlert(result, deliveryResult, serviceAlertId);
    }
  }

  public void handleServiceRequests(ServiceRequest serviceRequest,
      Siri responseSiri) {
    List<SituationExchangeRequestStructure> requests = serviceRequest.getSituationExchangeRequest();
    for (SituationExchangeRequestStructure request : requests) {
      handleServiceRequest(request, responseSiri);
    }
  }

  private void handleServiceRequest(SituationExchangeRequestStructure request,
      Siri responseSiri) {
    ServiceAlertsHelper helper = new ServiceAlertsHelper();
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    helper.addSituationExchangeToServiceDelivery(serviceDelivery,
        getPersister().getAllActiveServiceAlerts());
    responseSiri.setServiceDelivery(serviceDelivery);
    return;
  }

  public void handleSubscriptionRequests(
      SubscriptionRequest subscriptionRequests, Siri responseSiri) {
    String address = subscriptionRequests.getAddress();
    List<SituationExchangeSubscriptionStructure> requests = subscriptionRequests.getSituationExchangeSubscriptionRequest();
    for (SituationExchangeSubscriptionStructure request : requests) {
      handleSubscriptionRequest(request, responseSiri, address);
    }
  }

  private void handleSubscriptionRequest(
      SituationExchangeSubscriptionStructure request, Siri responseSiri,
      String address) {
    boolean status = true;
    String errorMessage = null;
    String subscriptionRef = UUID.randomUUID().toString();
    try {
      ServiceAlertSubscription subscription = new ServiceAlertSubscription();
      subscription.setAddress(address);
      subscription.setCreatedAt(new Date());
      if (request.getSubscriptionIdentifier() == null)
        throw new RuntimeException(
            "required element missing: subscriptionIdentifier");
      subscription.setSubscriptionIdentifier(request.getSubscriptionIdentifier().getValue());
      subscription.setSubscriptionRef(subscriptionRef);
      addSubscription(subscription);
    } catch (Exception e) {
      errorMessage = "Failed to create service alert subscription: "
          + e.getMessage();
      _log.error(errorMessage);
      status = false;
    }

    createSubscriptionSiriResponse(responseSiri, status, errorMessage,
        subscriptionRef);
    return;
  }

  private void createSubscriptionSiriResponse(Siri responseSiri,
      boolean status, String errorMessage, String subscriptionRef) {
    SubscriptionResponseStructure response = new SubscriptionResponseStructure();
    StatusResponseStructure statusResponseStructure = SiriHelper.createStatusResponseStructure(
        status, errorMessage);
    SubscriptionQualifierStructure subscriptionQualifierStructure = new SubscriptionQualifierStructure();
    subscriptionQualifierStructure.setValue(subscriptionRef);
    statusResponseStructure.setSubscriptionRef(subscriptionQualifierStructure);
    response.getResponseStatus().add(statusResponseStructure);
    responseSiri.setSubscriptionResponse(response);
  }

  protected void sendAndProcessSubscriptionAndServiceRequest() throws Exception {
    String result = sendSubscriptionAndServiceRequest();
    Siri siri = _siriXmlSerializer.fromXml(result);
    SituationExchangeResults handleResult = new SituationExchangeResults();
    handleServiceDeliveries(handleResult, siri.getServiceDelivery());
    _log.info(handleResult.toString());
  }

  String sendSubscriptionAndServiceRequest() throws Exception {
    Siri siri = createSubsAndSxRequest();
    String sendResult = getWebResourceWrapper().post(
        _siriXmlSerializer.getXml(siri), _serviceAlertsUrl);
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
    subscriptionRequest.setAddress(makeSubscriptionUrl(getSubscriptionPath()));
    subscriptionRequest.setRequestorRef(_environment.getParticipant() );
    siri.setSubscriptionRequest(subscriptionRequest);
    List<SituationExchangeSubscriptionStructure> exchangeSubscriptionRequests = subscriptionRequest.getSituationExchangeSubscriptionRequest();
    SituationExchangeSubscriptionStructure situationExchangeSubscriptionStructure = new SituationExchangeSubscriptionStructure();;
    exchangeSubscriptionRequests.add(situationExchangeSubscriptionStructure);
    SituationExchangeRequestStructure situationExchangeRequestStructure = new SituationExchangeRequestStructure();
    situationExchangeSubscriptionStructure.setSituationExchangeRequest(situationExchangeRequestStructure);
    SubscriptionQualifierStructure id = new SubscriptionQualifierStructure();
    id.setValue(UUID.randomUUID().toString());
    situationExchangeSubscriptionStructure.setSubscriptionIdentifier(id);
    situationExchangeRequestStructure.setRequestTimestamp(new Date());
    
    return siri;
  }

  private String makeSubscriptionUrl(String subscriptionPath)
      throws UnknownHostException {
    if (_subscriptionUrl != null)
      return _subscriptionUrl;
    String hostName = InetAddress.getLocalHost().getCanonicalHostName();
    _subscriptionUrl = "http://" + hostName + subscriptionPath;
    return _subscriptionUrl;
  }

  private void addSituationExchangeRequest(Siri siri) {
    ServiceRequest serviceRequest = new ServiceRequest();
    siri.setServiceRequest(serviceRequest);
    List<SituationExchangeRequestStructure> situationExchangeRequest = serviceRequest.getSituationExchangeRequest();
    SituationExchangeRequestStructure situationExchangeRequestStructure = new SituationExchangeRequestStructure();
    situationExchangeRequest.add(situationExchangeRequestStructure);
    situationExchangeRequestStructure.setRequestTimestamp(new Date());
    
    serviceRequest.setRequestorRef(_environment.getParticipant());
  }

  public NycTransitDataService getTransitDataService() {
    return _nycTransitDataService;
  }

  public void setTransitDataService(NycTransitDataService nycTransitDataService) {  
    this._nycTransitDataService = nycTransitDataService;
  }

  public String getServiceAlertsUrl() {
    return _serviceAlertsUrl;
  }

  public void setServiceAlertsUrl(String _serviceAlertsUrl) {
    this._serviceAlertsUrl = _serviceAlertsUrl;
  }

  public String getSubscriptionPath() {
    return _subscriptionPath;
  }

  public void setSubscriptionPath(String subscriptionPath) {
    this._subscriptionPath = subscriptionPath;
  }

  public WebResourceWrapper getWebResourceWrapper() {
    if (_webResourceWrapper == null)
      _webResourceWrapper = new WebResourceWrapper();
    return _webResourceWrapper;
  }

  public void setWebResourceWrapper(WebResourceWrapper _webResourceWrapper) {
    this._webResourceWrapper = _webResourceWrapper;
  }
  
}
