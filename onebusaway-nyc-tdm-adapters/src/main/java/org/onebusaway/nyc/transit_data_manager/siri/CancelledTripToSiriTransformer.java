/**
 * Copyright (C) 2022 Cambridge Systematics, Inc.
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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.util.AgencyAndIdLibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceConditionEnumeration;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationSourceStructure;
import uk.org.siri.siri.SituationSourceTypeEnumeration;

import java.util.Date;
import java.util.List;

/**
 * Transform Cancelled Trip models into simple service alerts via
 * SIRI PtSituationElementStructures
 */
public class CancelledTripToSiriTransformer {

  protected static Logger _log = LoggerFactory.getLogger(CancelledTripToSiriTransformer.class);

  private static String DEFAULT_LANG = "EN";

  private NycTransitDataService _nycTransitDataService;
  private ConfigurationService _configService;
  private boolean _performMerge;

  public CancelledTripToSiriTransformer(NycTransitDataService nycTransitDataService, ConfigurationService configurationService, boolean merge) {
    _nycTransitDataService = nycTransitDataService;
    _configService = configurationService;
    _performMerge = merge;
  }

  public ServiceDelivery mergeImpactedAlerts(ServiceDelivery serviceDelivery) {
    // make sure we have a valid ServiceDelivery object to add to
    if (serviceDelivery == null) {
      serviceDelivery = new ServiceDelivery();
    }
    if (isDisabled()) {
      return serviceDelivery;
    }
    SituationExchangeDeliveryStructure.Situations s;
    if (serviceDelivery.getSituationExchangeDelivery() == null
            || serviceDelivery.getSituationExchangeDelivery().isEmpty()) {
      SituationExchangeDeliveryStructure seds = new SituationExchangeDeliveryStructure();
      s = new SituationExchangeDeliveryStructure.Situations();
      seds.setSituations(s);
      serviceDelivery.getSituationExchangeDelivery().add(seds);
    } else {
      s = serviceDelivery.getSituationExchangeDelivery().get(0).getSituations();
    }

    if (_performMerge) {
      int addedAlerts = 0;
      if (_nycTransitDataService != null) {
        List<CancelledTripBean> cancelledTripBeans = _nycTransitDataService.getAllCancelledTrips().getList();
        // no retrieve cancelled trips from the TDS and add to the above ServiceDelivery instance
        for (CancelledTripBean cancelledTrip : cancelledTripBeans) {
          // convert a cancelled trip model into a situation element
          PtSituationElementStructure pt = fillPtSituationElement(cancelledTrip);
          if (pt != null) {
            s.getPtSituationElement().add(pt);
            addedAlerts++;
          }
        }
        _log.info("generated " + addedAlerts + " service alerts for "
                + cancelledTripBeans.size() + " CAPI trips");
      }
    } else {
      _log.debug("skipping merge");
    }
    return serviceDelivery;
  }

  private boolean isDisabled() {
    if (_configService == null) return false;
    boolean disabled = _configService.getConfigurationValueAsBoolean("tdm.disableCapiAlerts", false);
    if (disabled) {
      _log.error("CAPI Alerts Disabled!");
    }
    return disabled;
  }

  // do the conversion of a bean to PtSituationElementStructure
  private PtSituationElementStructure fillPtSituationElement(CancelledTripBean cancelledTrip) {
    if (cancelledTrip.getRouteId() == null) return null;
    AgencyAndId affectedRoute = null;
    try {
      affectedRoute = AgencyAndIdLibrary.convertFromString(cancelledTrip.getRouteId());
    } catch (IllegalStateException ise) {
      TripBean trip = _nycTransitDataService.getTrip(cancelledTrip.getTrip());
      if (trip != null) {
        affectedRoute = AgencyAndIdLibrary.convertFromString(trip.getRoute().getId());
      }
    }
    if (affectedRoute == null) return null;
    PtSituationElementStructure pt = new PtSituationElementStructure();

    EntryQualifierStructure s = new EntryQualifierStructure();
    s.setValue(cancelledTrip.getTrip());
    pt.setSituationNumber(s);
    // ideally this would be configurable/templated
    // we lookup the last stop name, if that succees the format is:
    // The 10:49am B38 bus to [dest terminal] is canceled
    // if that lookup fails, we fall back to
    // The 10:49am B38 bus from [orig terminal] is canceled

    String lastStopName = lookupLastStopName(cancelledTrip.getTrip());
    String descriptionText = null;
    if (lastStopName == null) {
      descriptionText = "The " + formatTime(cancelledTrip.getFirstStopDepartureTime())
              + " " + affectedRoute.getId()
              + " from "
              +  lookupStopName(cancelledTrip.getFirstStopId())
              + " is canceled";

    } else {
      descriptionText = "The " + formatTime(cancelledTrip.getFirstStopDepartureTime())
              + " " + affectedRoute.getId()
              + " to "
              + lastStopName
              + " is canceled";
    }

    pt.setSummary(toText("Bus Cancellation on " + affectedRoute.getId()));
    pt.setDescription(toText(descriptionText));
    pt.setCreationTime(new Date(cancelledTrip.getTimestamp()));
    pt.setPlanned(false);
    SituationSourceStructure source = new SituationSourceStructure();
    source.setSourceType(SituationSourceTypeEnumeration.DIRECT_REPORT);
    pt.setSource(source);

    // now set affects
    AffectsScopeStructure affects = new AffectsScopeStructure();
    pt.setAffects(affects);
    AffectsScopeStructure.VehicleJourneys vj = new AffectsScopeStructure.VehicleJourneys();
    affects.setVehicleJourneys(vj);
    // for legacy reasons routes expect direction 0 and 1 to be set
    // direction 0
    AffectedVehicleJourneyStructure avj0 = new AffectedVehicleJourneyStructure();
    pt.getAffects().getVehicleJourneys().getAffectedVehicleJourney().add(avj0);
    avj0.setLineRef(toLineRef(affectedRoute.toString()));
    avj0.setDirectionRef(toDirectionRef("0"));

    // direction 1
    AffectedVehicleJourneyStructure avj1 = new AffectedVehicleJourneyStructure();
    pt.getAffects().getVehicleJourneys().getAffectedVehicleJourney().add(avj1);
    avj1.setLineRef(toLineRef(affectedRoute.toString()));
    avj1.setDirectionRef(toDirectionRef("1"));


    // consequences
    // mark the alert as disrupted to differentiate from traditional alerts
    PtConsequencesStructure ptConsequences = new PtConsequencesStructure();
    pt.setConsequences(ptConsequences);
    PtConsequenceStructure ptConsequenceStructure = new PtConsequenceStructure();
    ServiceConditionEnumeration serviceCondition = ServiceConditionEnumeration.DISRUPTED;
    ptConsequenceStructure.setCondition(serviceCondition);
    ptConsequences.getConsequence().add(ptConsequenceStructure);

    return pt;
  }

  // query the TDS for the stop list for the trip and return the last stop name
  private String lookupLastStopName(String trip) {
    TripDetailsQueryBean query = new TripDetailsQueryBean();
    query.setTripId(trip);
    ListBean<TripDetailsBean> tripDetails = _nycTransitDataService.getTripDetails(query);
    if (tripDetails == null || tripDetails.getList() == null
        || tripDetails.getList().isEmpty()) {
      _log.error("no trip details");
      return null;
    }
    TripDetailsBean tripDetailsBean = tripDetails.getList().get(0);
    if (tripDetailsBean.getSchedule() == null
            || tripDetailsBean.getSchedule().getStopTimes() == null) {
      _log.error("no schedule");
      return null;
    }
    List<TripStopTimeBean> stopTimes = tripDetailsBean.getSchedule().getStopTimes();
    int size = stopTimes.size();
    if (size <= 1) {
      _log.error("no stop times");
      return null;
    }
    TripStopTimeBean tripStopTimeBean = stopTimes.get(size - 1);
    return tripStopTimeBean.getStop().getName();
  }

  String formatTime(String firstStopDepartureTime) {
    if (firstStopDepartureTime == null) return null;
    if (firstStopDepartureTime.contains(":")) {
      String[] parts = firstStopDepartureTime.split(":");
      if (parts.length > 2) {
        try {
          // convert 24h to local
          int hour = Integer.parseInt(parts[0]) % 24;
          int minute = Integer.parseInt(parts[1]);
          String am_pm = hour < 12 ? "am" : "pm";
          if(hour == 0){
            return (hour+12) + ":" + leftPad(minute) + am_pm;
          }
          else if (hour <= 12) {
            return hour + ":" + leftPad(minute) + am_pm;
          }
          else {
            return (hour-12) + ":" + leftPad(minute) + am_pm;
          }
        } catch (NumberFormatException nfe) {
          _log.error("invalid time format " + firstStopDepartureTime);
        }
      }
    }
    return firstStopDepartureTime;
  }

  String leftPad(int minute) {
    String minuteStr = String.valueOf(minute);
    if (minuteStr == null || minuteStr.length() == 0) {
      minuteStr = "00";
    }
    if (minuteStr.length() < 2) {
      minuteStr = "0" + minuteStr;
    }
    return minuteStr;
  }

  private String lookupStopName(String stopId) {
    StopBean stop = _nycTransitDataService.getStop(stopId);
    if (stop == null) return stopId;
    return stop.getName();
  }


  private DefaultedTextStructure toText(String text) {
    DefaultedTextStructure s = new DefaultedTextStructure();
    s.setLang(DEFAULT_LANG);
    s.setValue(text);
    return s;
  }

  private LineRefStructure toLineRef(String routeId) {
    LineRefStructure s = new LineRefStructure();
    s.setValue(routeId);
    return s;
  }

  private DirectionRefStructure toDirectionRef(String directionId) {
    DirectionRefStructure d = new DirectionRefStructure();
    d.setValue(directionId);
    return d;
  }
}
