/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.siri.OneBusAwayConsequence;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.AffectedLineStructure;
import uk.org.siri.siri.AffectedLineStructure.Routes;
import uk.org.siri.siri.AffectedRouteStructure;
import uk.org.siri.siri.AffectedStopPointStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.AffectsScopeStructure.Networks;
import uk.org.siri.siri.AffectsScopeStructure.Networks.AffectedNetwork;
import uk.org.siri.siri.AffectsScopeStructure.Operators;
import uk.org.siri.siri.AffectsScopeStructure.StopPoints;
import uk.org.siri.siri.AffectsScopeStructure.VehicleJourneys;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.DirectionStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.HalfOpenTimestampRangeStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.RouteRefStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SeverityEnumeration;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;

public class VehicleMonitoringAction extends OneBusAwayNYCActionSupport
    implements ServletRequestAware {

  private static final long serialVersionUID = 1L;

  @Autowired
  protected TransitDataService _transitDataService;

  @Autowired
  private RealtimeService _realtimeService;

  private Siri _response;

  private HttpServletRequest _request;

  private Date _now = new Date();

  @Override
  public String execute() {
    String agencyId = _request.getParameter("OperatorRef");
    String vehicleId = _request.getParameter("VehicleRef");

    String directionId = _request.getParameter("DirectionRef");
    String routeId = _request.getParameter("LineRef");

    String detailLevel = _request.getParameter("VehicleMonitoringDetailLevel");

    boolean includeOnwardCalls = false;
    if (detailLevel != null) {
      includeOnwardCalls = detailLevel.equals("calls");
    }

    // *** CASE 1: by route
    if (agencyId != null && routeId != null) {
      String routeIdWithAgency = agencyId + "_" + routeId;

      _response = generateSiriResponse(_realtimeService.getVehicleActivityForRoute(
          routeIdWithAgency, directionId, includeOnwardCalls));

      return SUCCESS;
    }

    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();

    // *** CASE 2: single vehicle
    if (agencyId != null && vehicleId != null) {
      String vehicleIdWithAgency = agencyId + "_" + vehicleId;

      activities.add(_realtimeService.getVehicleActivityForVehicle(
          vehicleIdWithAgency, includeOnwardCalls));

      // *** CASE 3: all vehicles
    } else {
      ListBean<VehicleStatusBean> vehicles = _transitDataService.getAllVehiclesForAgency(
          agencyId, _now.getTime());

      for (VehicleStatusBean v : vehicles.getList()) {
        activities.add(_realtimeService.getVehicleActivityForVehicle(
            v.getVehicleId(), includeOnwardCalls));
      }
    }

    _response = generateSiriResponse(activities);

    return SUCCESS;
  }

  /** Generate a siri response for a set of VehicleActivities */
  private Siri generateSiriResponse(List<VehicleActivityStructure> activities) {

    VehicleMonitoringDeliveryStructure vehicleMonitoringDelivery = new VehicleMonitoringDeliveryStructure();
    vehicleMonitoringDelivery.setResponseTimestamp(_now);

    Calendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(_now);
    gregorianCalendar.add(Calendar.MINUTE, 1);
    vehicleMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());

    vehicleMonitoringDelivery.getVehicleActivity().addAll(activities);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    serviceDelivery.setResponseTimestamp(_now);
    serviceDelivery.getVehicleMonitoringDelivery().add(
        vehicleMonitoringDelivery);

    // TODO Actually, unclear whether we want service alert details sent with
    // this response, or should get pulled via a second call. Check w/ Jeff.
    addSituationExchange(serviceDelivery, activities);

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);

    return siri;
  }

  public String getVehicleMonitoring() throws Exception {
    return SiriJsonSerializer.getJson(_response,
        _request.getParameter("callback"));
  }

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this._request = request;
  }

  void addSituationExchange(ServiceDelivery serviceDelivery,
      List<VehicleActivityStructure> activities) {
    SituationExchangeDeliveryStructure situationExchangeDelivery = new SituationExchangeDeliveryStructure();
    Situations situations = new Situations();
    situationExchangeDelivery.setSituations(situations);
    for (VehicleActivityStructure activity : activities) {
      List<SituationRefStructure> situationRefs = activity.getMonitoredVehicleJourney().getSituationRef();
      for (SituationRefStructure situationRef : situationRefs) {
        String situationId = situationRef.getSituationSimpleRef().getValue();
        ServiceAlertBean serviceAlert = _transitDataService.getServiceAlertForId(situationId);
        PtSituationElementStructure e = getServiceAlertBeanAsPtSituationElementStructure(serviceAlert);
        situationExchangeDelivery.getSituations().getPtSituationElement().add(e);
      }
    }

    if (situationExchangeDelivery.getSituations() != null
        && !CollectionUtils.isEmpty(situationExchangeDelivery.getSituations().getPtSituationElement()))
      serviceDelivery.getSituationExchangeDelivery().add(
          situationExchangeDelivery);

  }

  // TODO This method should be moved to a common place, with the one in
  // NycSiriService that translates the other way.
  // Pending discussion w/ Jeff.
  private PtSituationElementStructure getServiceAlertBeanAsPtSituationElementStructure(
      ServiceAlertBean serviceAlert) {
    PtSituationElementStructure ptSit = new PtSituationElementStructure();

    EntryQualifierStructure value = new EntryQualifierStructure();
    value.setValue(serviceAlert.getId());
    ptSit.setSituationNumber(value);

    handleDescriptions(serviceAlert, ptSit);
    handleOtherFields(serviceAlert, ptSit);
    handleAffects(serviceAlert, ptSit);
    handleConsequences(serviceAlert, ptSit);

    return ptSit;
  }

  private void handleDescriptions(ServiceAlertBean serviceAlert,
      PtSituationElementStructure ptSituation) {

    for (NaturalLanguageStringBean summary : serviceAlert.getSummaries()) {
      ptSituation.setSummary(createDefaultedTextStructure(summary));
    }

    for (NaturalLanguageStringBean description : serviceAlert.getDescriptions()) {
      ptSituation.setDescription(createDefaultedTextStructure(description));
    }

  }

  private DefaultedTextStructure createDefaultedTextStructure(
      NaturalLanguageStringBean summary) {
    DefaultedTextStructure d = new DefaultedTextStructure();
    d.setLang(summary.getLang());
    d.setValue(summary.getValue());
    return d;
  }

  private void handleOtherFields(ServiceAlertBean serviceAlert,
      PtSituationElementStructure ptSituation) {

    // TODO Not handling severity yet.
    ptSituation.setSeverity(SeverityEnumeration.UNDEFINED);

    HalfOpenTimestampRangeStructure timestampRangeStructure = new HalfOpenTimestampRangeStructure();
    if (!CollectionUtils.isEmpty(serviceAlert.getPublicationWindows())) {
      timestampRangeStructure.setStartTime(serviceAlertTimeToDate(serviceAlert.getPublicationWindows().get(
          0).getFrom()));
      timestampRangeStructure.setEndTime(serviceAlertTimeToDate(serviceAlert.getPublicationWindows().get(
          0).getTo()));
      ptSituation.setPublicationWindow(timestampRangeStructure);
    }

  }

  public Date serviceAlertTimeToDate(long time) {
    if (time == 0)
      return null;
    return new Date(time);
  }

  @SuppressWarnings("unused")
  private void handlReasons(PtSituationElementStructure ptSituation,
      ServiceAlertBean serviceAlert) {
    throw new RuntimeException("handleReasons not implemented");
  }

  private void handleAffects(ServiceAlertBean serviceAlert,
      PtSituationElementStructure ptSituation) {

    AffectsScopeStructure affectsStructure = new AffectsScopeStructure(); 
    for (SituationAffectsBean affects : serviceAlert.getAllAffects()) {
      String agencyId = affects.getAgencyId();
      if (agencyId != null) {
        Operators operators = new Operators();
        operators.setAllOperators(agencyId);
        affectsStructure.setOperators(operators);
      }
      String routeId = affects.getRouteId(); // LineRef
      String stopId = affects.getStopId();
      String directionId = affects.getDirectionId(); // DirectionRef
      String tripId = affects.getTripId(); // vehiclejourneyref
      if (!StringUtils.isBlank(routeId)) {
        Networks networks = new Networks();
        List<AffectedNetwork> network = networks.getAffectedNetwork();
        AffectedNetwork affectedNetwork = new AffectedNetwork();
        List<AffectedLineStructure> affectedLine = affectedNetwork.getAffectedLine();
        AffectedLineStructure affectedLineStructure = new AffectedLineStructure();
        Routes routes = new Routes();
        List<AffectedRouteStructure> affectedRoute = routes.getAffectedRoute();
        AffectedRouteStructure affectedRouteStructure = new AffectedRouteStructure();
        RouteRefStructure routeRefStructure = new RouteRefStructure();
        routeRefStructure.setValue(routeId);
        affectedRouteStructure.setRouteRef(routeRefStructure);
        affectedRoute.add(affectedRouteStructure);
        affectedLineStructure.setRoutes(routes);
        DirectionStructure directionStructure = new DirectionStructure();
        NaturalLanguageStringStructure directionName = new NaturalLanguageStringStructure();
        directionName.setValue(directionId);
        directionStructure.setDirectionName(directionName);
        affectedLineStructure.getDirection().add(directionStructure);
        affectedLine.add(affectedLineStructure);
        network.add(affectedNetwork);
        affectsStructure.setNetworks(networks);
      }
      if (!StringUtils.isBlank(tripId)) {
        VehicleJourneys vehicleJourneys = new VehicleJourneys();
        AffectedVehicleJourneyStructure affectedVehicleJourneyStructure = new AffectedVehicleJourneyStructure();
        LineRefStructure lineRefStructure = new LineRefStructure();
        lineRefStructure.setValue(routeId);
        affectedVehicleJourneyStructure.setLineRef(lineRefStructure);
        if (!StringUtils.isBlank(directionId)) {
          DirectionRefStructure directionRefStructure = new DirectionRefStructure();
          directionRefStructure.setValue(directionId);
          affectedVehicleJourneyStructure.setDirectionRef(directionRefStructure);
        }
        vehicleJourneys.getAffectedVehicleJourney().add(
            affectedVehicleJourneyStructure);
        affectsStructure.setVehicleJourneys(vehicleJourneys);
      }
      if (!StringUtils.isBlank(stopId)) {
        StopPoints value = new StopPoints();
        List<AffectedStopPointStructure> stopPoints = value.getAffectedStopPoint();
        AffectedStopPointStructure affectedStopPointStructure = new AffectedStopPointStructure();
        StopPointRefStructure stopPointRef = new StopPointRefStructure();
        stopPointRef.setValue(stopId);
        affectedStopPointStructure.setStopPointRef(stopPointRef);
        stopPoints.add(affectedStopPointStructure);
        affectsStructure.setStopPoints(value);
      }
    }
    
    ptSituation.setAffects(affectsStructure);
    
  }

  private void handleConsequences(ServiceAlertBean serviceAlert,
      PtSituationElementStructure ptSituation) {

    List<SituationConsequenceBean> consequences = serviceAlert.getConsequences();
    if (CollectionUtils.isEmpty(consequences))
      return;

    PtConsequencesStructure ptConsequences = new PtConsequencesStructure(); 
    ptSituation.setConsequences(ptConsequences);

    for (SituationConsequenceBean consequence : consequences) {
      EEffect effect = consequence.getEffect();
      PtConsequenceStructure ptConsequenceStructure = new PtConsequenceStructure();
      ServiceConditionEnumeration serviceCondition = getEFfectAsCondition(effect);
      ptConsequenceStructure.setCondition(serviceCondition);

      String detourPath = consequence.getDetourPath();
      if (!StringUtils.isBlank(detourPath)) {
        ExtensionsStructure extensionStructure = new ExtensionsStructure();
        OneBusAwayConsequence oneBusAwayConsequence = new OneBusAwayConsequence();
        oneBusAwayConsequence.setDiversionPath(detourPath);
        extensionStructure.setAny(oneBusAwayConsequence);
        ptConsequenceStructure.setExtensions(extensionStructure);
      }

      ptConsequences.getConsequence().add(ptConsequenceStructure);
    }

  }

  private ServiceConditionEnumeration getEFfectAsCondition(EEffect effect) {
    switch (effect) {

      case NO_SERVICE:
        return ServiceConditionEnumeration.NO_SERVICE;

      case SIGNIFICANT_DELAYS:
        return ServiceConditionEnumeration.DELAYED;

      case DETOUR:
        return ServiceConditionEnumeration.DIVERTED;

      case ADDITIONAL_SERVICE:
        return ServiceConditionEnumeration.ADDITIONAL_SERVICE;

      case REDUCED_SERVICE:
        return ServiceConditionEnumeration.DISRUPTED;

      case MODIFIED_SERVICE:
        return ServiceConditionEnumeration.ALTERED;

      case OTHER_EFFECT:
        return ServiceConditionEnumeration.NORMAL_SERVICE;

      case UNKNOWN_EFFECT:
        return ServiceConditionEnumeration.UNKNOWN;

      default:
        return ServiceConditionEnumeration.UNKNOWN;
    }
  }

}
