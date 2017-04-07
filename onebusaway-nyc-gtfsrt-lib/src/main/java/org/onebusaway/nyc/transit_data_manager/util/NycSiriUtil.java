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
package org.onebusaway.nyc.transit_data_manager.util;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.siri.AffectedApplicationStructure;
import org.onebusaway.siri.OneBusAwayAffects;
import org.onebusaway.siri.OneBusAwayAffectsStructure;
import org.onebusaway.siri.OneBusAwayConsequence;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;
import org.onebusaway.transit_data_federation.impl.realtime.siri.SiriEndpointDetails;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.service_alerts.ServiceAlerts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri.AffectedCallStructure;
import uk.org.siri.siri.AffectedOperatorStructure;
import uk.org.siri.siri.AffectedStopPointStructure;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.HalfOpenTimestampRangeStructure;
import uk.org.siri.siri.OperatorRefStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.SeverityEnumeration;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.VehicleJourneyRefStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NycSiriUtil {

  public static final String ALL_OPERATORS = "__ALL_OPERATORS__";

  private static Logger _log = LoggerFactory.getLogger(NycSiriUtil.class);

  public static List<ServiceAlertBean> getSiriAsServiceAlertBeans(Siri siri) {
    List<ServiceAlertBean> beans = new ArrayList<ServiceAlertBean>();

    for (SituationExchangeDeliveryStructure s : siri.getServiceDelivery().getSituationExchangeDelivery()) {
      SiriEndpointDetails endpointDetails = new SiriEndpointDetails();
      SituationExchangeDeliveryStructure.Situations situations = s.getSituations();
      if (situations != null) {
        for (PtSituationElementStructure ptSituation : situations.getPtSituationElement()) {

          ServiceAlertBean bean = getPtSituationAsServiceAlertBean(
                  ptSituation, endpointDetails);
          beans.add(bean);
        }
      }
    }

    return beans;
  }

  public static ServiceAlertBean getPtSituationAsServiceAlertBean(
          PtSituationElementStructure ptSituation,
          SiriEndpointDetails endpointDetails) {
    ServiceAlertBean serviceAlert = new ServiceAlertBean();
    try {
      EntryQualifierStructure serviceAlertNumber = ptSituation.getSituationNumber();
      String situationId = StringUtils.trim(serviceAlertNumber.getValue());

      if (!endpointDetails.getDefaultAgencyIds().isEmpty()) {
        String agencyId = endpointDetails.getDefaultAgencyIds().get(0);
        serviceAlert.setId(AgencyAndId.convertToString(new AgencyAndId(
                agencyId, situationId)));
      } else {
        AgencyAndId id = AgencyAndIdLibrary.convertFromString(situationId);
        serviceAlert.setId(AgencyAndId.convertToString(id));
      }

      if (ptSituation.getCreationTime() != null)
        serviceAlert.setCreationTime(ptSituation.getCreationTime().getTime());

      handleDescriptions(ptSituation, serviceAlert);
      handleOtherFields(ptSituation, serviceAlert);
      // TODO not yet implemented
      // handlReasons(ptSituation, serviceAlert);
      handleAffects(ptSituation, serviceAlert);
      handleConsequences(ptSituation, serviceAlert);
    } catch (Exception e) {
      _log.error("Failed to convert SIRI to service alert: " + e.getMessage());
    }

    return serviceAlert;
  }

  private static void handleDescriptions(PtSituationElementStructure ptSituation,
                                  ServiceAlertBean serviceAlert) {
    ServiceAlerts.TranslatedString summary = translation(ptSituation.getSummary());
    if (summary != null)
      serviceAlert.setSummaries(naturalLanguageStringBeanFromTranslatedString(summary));

    ServiceAlerts.TranslatedString description = translation(ptSituation.getDescription());
    if (description != null)
      serviceAlert.setDescriptions(naturalLanguageStringBeanFromTranslatedString(description));
  }

  private static List<NaturalLanguageStringBean> naturalLanguageStringBeanFromTranslatedString(
          ServiceAlerts.TranslatedString translatedString) {
    List<NaturalLanguageStringBean> nlsb = new ArrayList<NaturalLanguageStringBean>();
    for (ServiceAlerts.TranslatedString.Translation t : translatedString.getTranslationList()) {
      nlsb.add(new NaturalLanguageStringBean(StringUtils.trim(t.getText()),
              StringUtils.trim(t.getLanguage())));
    }
    return nlsb;
  }

  private static void handleOtherFields(PtSituationElementStructure ptSituation,
                                 ServiceAlertBean serviceAlert) {

    SeverityEnumeration severity = ptSituation.getSeverity();
    if (severity != null) {
      ESeverity severityEnum = ESeverity.valueOfTpegCode(StringUtils.trim(severity.value()));
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

  @SuppressWarnings("unused")
  private void handlReasons(PtSituationElementStructure ptSituation,
                            ServiceAlertBean serviceAlert) {
    throw new RuntimeException("handleReasons not implemented");
  }

  private static void handleAffects(PtSituationElementStructure ptSituation,
                             ServiceAlertBean serviceAlert) {
    AffectsScopeStructure affectsStructure = ptSituation.getAffects();

    if (affectsStructure == null)
      return;

    List<SituationAffectsBean> allAffects = new ArrayList<SituationAffectsBean>();

    AffectsScopeStructure.Operators operators = affectsStructure.getOperators();

    if (operators != null) {
      if (operators.getAllOperators() != null) {
        addAffectsOperator(allAffects, ALL_OPERATORS);
      }
      if (!CollectionsLibrary.isEmpty(operators.getAffectedOperator())) {

        for (AffectedOperatorStructure operator : operators.getAffectedOperator()) {
          OperatorRefStructure operatorRef = operator.getOperatorRef();
          if (operatorRef == null || operatorRef.getValue() == null)
            continue;
          addAffectsOperator(allAffects, StringUtils.trim(operatorRef.getValue()));
        }
      }
    }

    AffectsScopeStructure.StopPoints stopPoints = affectsStructure.getStopPoints();

    if (stopPoints != null
            && !CollectionsLibrary.isEmpty(stopPoints.getAffectedStopPoint())) {

      for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoint()) {
        StopPointRefStructure stopRef = stopPoint.getStopPointRef();
        if (stopRef == null || stopRef.getValue() == null)
          continue;
        SituationAffectsBean sab = new SituationAffectsBean();
        sab.setStopId(StringUtils.trim(stopRef.getValue()));
        allAffects.add(sab);
      }
    }

    AffectsScopeStructure.VehicleJourneys vjs = affectsStructure.getVehicleJourneys();
    if (vjs != null
            && !CollectionsLibrary.isEmpty(vjs.getAffectedVehicleJourney())) {

      for (AffectedVehicleJourneyStructure vj : vjs.getAffectedVehicleJourney()) {

        SituationAffectsBean sab = new SituationAffectsBean();

        if (vj.getLineRef() != null) {
          sab.setRouteId(StringUtils.trim(vj.getLineRef().getValue()));
        }

        if (vj.getDirectionRef() != null)
          sab.setDirectionId(StringUtils.trim(vj.getDirectionRef().getValue()));

        List<VehicleJourneyRefStructure> tripRefs = vj.getVehicleJourneyRef();
        AffectedVehicleJourneyStructure.Calls stopRefs = vj.getCalls();

        boolean hasTripRefs = !CollectionsLibrary.isEmpty(tripRefs);
        boolean hasStopRefs = stopRefs != null
                && !CollectionsLibrary.isEmpty(stopRefs.getCall());

        if (!(hasTripRefs || hasStopRefs)) {
          if (sab.getRouteId() != null && !sab.getRouteId().isEmpty()) {
            allAffects.add(sab);
          }
        } else if (hasTripRefs && hasStopRefs) {
          for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
            sab.setTripId(StringUtils.trim(vjRef.getValue()));
            for (AffectedCallStructure call : stopRefs.getCall()) {
              sab.setStopId(call.getStopPointRef().getValue());
              allAffects.add(sab);
            }
          }
        } else if (hasTripRefs) {
          for (VehicleJourneyRefStructure vjRef : vj.getVehicleJourneyRef()) {
            sab.setTripId(StringUtils.trim(vjRef.getValue()));
            allAffects.add(sab);
          }
        } else {
          for (AffectedCallStructure call : stopRefs.getCall()) {
            sab.setStopId(StringUtils.trim(call.getStopPointRef().getValue()));
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

        OneBusAwayAffectsStructure.Applications applications = obaAffects.getApplications();
        if (applications != null
                && !CollectionsLibrary.isEmpty(applications.getAffectedApplication())) {

          List<AffectedApplicationStructure> apps = applications.getAffectedApplication();

          for (AffectedApplicationStructure sApp : apps) {
            SituationAffectsBean sab = new SituationAffectsBean();
            sab.setApplicationId(StringUtils.trim(sApp.getApiKey()));
            allAffects.add(sab);
          }
        }
      }
    }

    if (!allAffects.isEmpty())
      serviceAlert.setAllAffects(allAffects);
  }

  private static void addAffectsOperator(List<SituationAffectsBean> allAffects,
                                  String operator) {
    SituationAffectsBean sab = new SituationAffectsBean();
    sab.setAgencyId(operator);
    allAffects.add(sab);
  }

  private static void handleConsequences(PtSituationElementStructure ptSituation,
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
            situationConsequenceBean.setDetourPath(StringUtils.trim(obaConsequence.getDiversionPath()));
        }
      }
      if (situationConsequenceBean.getDetourPath() != null
              || situationConsequenceBean.getEffect() != null)
        consequencesList.add(situationConsequenceBean);
    }

    if (!consequencesList.isEmpty())
      serviceAlert.setConsequences(consequencesList);
  }

  private static EEffect getConditionAsEffect(ServiceConditionEnumeration condition) {
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

  private static ServiceAlerts.TranslatedString translation(DefaultedTextStructure text) {
    if (text == null)
      return null;
    String value = StringUtils.trim(text.getValue());
    if (value == null || value.isEmpty())
      return null;

    ServiceAlerts.TranslatedString.Translation.Builder translation = ServiceAlerts.TranslatedString.Translation.newBuilder();
    translation.setText(value);
    if (text.getLang() != null)
      translation.setLanguage(StringUtils.trim(text.getLang()));

    ServiceAlerts.TranslatedString.Builder tsBuilder = ServiceAlerts.TranslatedString.newBuilder();
    tsBuilder.addTranslation(translation);
    return tsBuilder.build();
  }
}
