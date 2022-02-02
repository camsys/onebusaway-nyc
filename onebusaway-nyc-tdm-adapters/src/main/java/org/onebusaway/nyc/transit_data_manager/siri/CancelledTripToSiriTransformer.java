package org.onebusaway.nyc.transit_data_manager.siri;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import uk.org.siri.siri.AffectedVehicleJourneyStructure;
import uk.org.siri.siri.AffectsScopeStructure;
import uk.org.siri.siri.DefaultedTextStructure;
import uk.org.siri.siri.EntryQualifierStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.PtConsequenceStructure;
import uk.org.siri.siri.PtConsequencesStructure;
import uk.org.siri.siri.PtSituationElementStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.SeverityEnumeration;
import uk.org.siri.siri.SituationExchangeDeliveryStructure;
import uk.org.siri.siri.SituationSourceStructure;
import uk.org.siri.siri.SituationSourceTypeEnumeration;

import java.util.Date;

/**
 * Transform Cancelled Trip models into simple service alerts via
 * SIRI PtSituationElementStructures
 */
public class CancelledTripToSiriTransformer {

  private static String DEFAULT_LANG = "EN";

  private NycTransitDataService _nycTransitDataService;
  public CancelledTripToSiriTransformer(NycTransitDataService nycTransitDataService) {
    _nycTransitDataService = nycTransitDataService;
  }

  public ServiceDelivery mergeImpactedAlerts(ServiceDelivery serviceDelivery) {
    // make sure we have a valid ServiceDelivery object to add to
    if (serviceDelivery == null) {
      serviceDelivery = new ServiceDelivery();
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

    if (_nycTransitDataService != null) {
      // no retrieve cancelled trips from the TDS and add to the above ServiceDelivery instance
      for (NycCancelledTripBean cancelledTrip : _nycTransitDataService.getAllCancelledTrips()) {
        // convert a cancelled trip model into a situation element
        PtSituationElementStructure pt = fillPtSituationElement(cancelledTrip);
        if (pt != null) {
          s.getPtSituationElement().add(pt);
        }
      }
    }

    return serviceDelivery;
  }

  // do the conversion of a bean to PtSituationElementStructure
  private PtSituationElementStructure fillPtSituationElement(NycCancelledTripBean cancelledTrip) {
    if (cancelledTrip.getRoute() == null) return null;
    AgencyAndId affectedRoute = null;
    try {
      affectedRoute = AgencyAndIdLibrary.convertFromString(cancelledTrip.getRoute());
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
    String descriptionText = "Trip departing at " + cancelledTrip.getFirstStopDepartureTime().toString()
            + " on route " + affectedRoute.getId() + " delayed or cancelled";
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
    AffectedVehicleJourneyStructure avj = new AffectedVehicleJourneyStructure();
    pt.getAffects().getVehicleJourneys().getAffectedVehicleJourney().add(avj);
    avj.setLineRef(toLineRef(affectedRoute.toString()));

    // consequences
    PtConsequenceStructure consequence = new PtConsequenceStructure();
    consequence.setSeverity(SeverityEnumeration.UNDEFINED);
    pt.setConsequences(new PtConsequencesStructure());
    pt.getConsequences().getConsequence().add(consequence);

    return pt;
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

}
