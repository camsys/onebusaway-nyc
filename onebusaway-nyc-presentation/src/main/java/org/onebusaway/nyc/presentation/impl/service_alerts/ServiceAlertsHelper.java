package org.onebusaway.nyc.presentation.impl.service_alerts;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.siri.OneBusAwayConsequence;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.services.TransitDataService;

import uk.org.siri.siri.AffectedLineStructure;
import uk.org.siri.siri.AffectedRouteStructure;
import uk.org.siri.siri.AffectedStopPointStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.DirectionStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.HalfOpenTimestampRangeStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.RouteRefStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SeverityEnumeration;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.AffectedLineStructure.Routes;
import uk.org.siri.siri.AffectsScopeStructure.Networks;
import uk.org.siri.siri.AffectsScopeStructure.Operators;
import uk.org.siri.siri.AffectsScopeStructure.StopPoints;
import uk.org.siri.siri.AffectsScopeStructure.VehicleJourneys;
import uk.org.siri.siri.AffectsScopeStructure.Networks.AffectedNetwork;
import uk.org.siri.siri.SituationExchangeDeliveryStructure.Situations;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.VehicleActivityStructure;

public class ServiceAlertsHelper {

  public void addSituationExchangeToSiri(ServiceDelivery serviceDelivery,
      List<VehicleActivityStructure> activities,
      TransitDataService transitDataService) {

    Map<String, PtSituationElementStructure> ptSituationElements = new HashMap<String, PtSituationElementStructure>();
    for (VehicleActivityStructure activity : activities) {
      if (activity.getMonitoredVehicleJourney() != null) {
        addSituationElement(transitDataService, ptSituationElements,
            activity.getMonitoredVehicleJourney().getSituationRef());
      }
    }
    addPtSituationElementsToServiceDelivery(serviceDelivery,
        ptSituationElements);
  }

  public void addSituationExchangeToSiriForStops(
      ServiceDelivery serviceDelivery,
      List<MonitoredStopVisitStructure> visits,
      TransitDataService transitDataService) {

    Map<String, PtSituationElementStructure> ptSituationElements = new HashMap<String, PtSituationElementStructure>();
    for (MonitoredStopVisitStructure visit : visits) {
      addSituationElement(transitDataService, ptSituationElements,
          visit.getMonitoredVehicleJourney().getSituationRef());
    }
    addPtSituationElementsToServiceDelivery(serviceDelivery,
        ptSituationElements);
  }

  private void addSituationElement(TransitDataService transitDataService,
      Map<String, PtSituationElementStructure> ptSituationElements,
      List<SituationRefStructure> situationRefs) {
    for (SituationRefStructure situationRef : situationRefs) {
      String situationId = situationRef.getSituationSimpleRef().getValue();
      ServiceAlertBean serviceAlert = transitDataService.getServiceAlertForId(situationId);
      PtSituationElementStructure e = getServiceAlertBeanAsPtSituationElementStructure(serviceAlert);
      ptSituationElements.put(situationId, e);
    }
  }

  private void addPtSituationElementsToServiceDelivery(
      ServiceDelivery serviceDelivery,
      Map<String, PtSituationElementStructure> ptSituationElements) {
    SituationExchangeDeliveryStructure situationExchangeDelivery = new SituationExchangeDeliveryStructure();
    Situations situations = new Situations();
    situationExchangeDelivery.setSituations(situations);

    for (PtSituationElementStructure p : ptSituationElements.values()) {
      situations.getPtSituationElement().add(p);
    }

    if (situationExchangeDelivery.getSituations() != null
        && !CollectionUtils.isEmpty(situationExchangeDelivery.getSituations().getPtSituationElement()))
      serviceDelivery.getSituationExchangeDelivery().add(
          situationExchangeDelivery);
  }

  public PtSituationElementStructure getServiceAlertBeanAsPtSituationElementStructure(
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
