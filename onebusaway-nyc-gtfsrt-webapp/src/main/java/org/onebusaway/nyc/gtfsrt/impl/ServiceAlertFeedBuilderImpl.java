package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.gtfsrt.service.ServiceAlertFeedBuilder;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceAlertFeedBuilderImpl implements ServiceAlertFeedBuilder {
    @Override
    public Alert.Builder getAlertFromServiceAlert(ServiceAlertBean alert) {

        Alert.Builder rtAlert = Alert.newBuilder();

        if (alert.getPublicationWindows() != null) {
            for (TimeRangeBean bean : alert.getPublicationWindows()) {
                rtAlert.addActivePeriod(range(bean));
            }
        }

        if (alert.getAllAffects() != null) {
            for (SituationAffectsBean affects : alert.getAllAffects()) {
                rtAlert.addInformedEntity(informedEntity(affects));
            }
        }

        if (alert.getReason() != null) {
            for (Alert.Cause cause : Alert.Cause.values()) {
                if (cause.toString().equals(alert.getReason())) {
                    rtAlert.setCause(cause);
                    break;
                }
            }
        }

        if (alert.getConsequences() != null && !alert.getConsequences().isEmpty()) {
            SituationConsequenceBean cb = alert.getConsequences().get(0);
            // Effect and EEffect perfectly match string values
            rtAlert.setEffect(Alert.Effect.valueOf(cb.getEffect().toString()));
        }

        if (alert.getUrls() != null) {
            rtAlert.setUrl(translatedString(alert.getUrls()));
        }

        if (alert.getSummaries() != null) {
            rtAlert.setHeaderText(translatedString(alert.getSummaries()));
        }

        if (alert.getDescriptions() != null) {
            rtAlert.setDescriptionText(translatedString(alert.getDescriptions()));
        }

        return rtAlert;
    }

    private static TimeRange.Builder range(TimeRangeBean range) {
        TimeRange.Builder builder = TimeRange.newBuilder();
        if (range.getFrom() > 0)
            builder.setStart(range.getFrom()/1000);
        if (range.getTo() > 0)
            builder.setEnd(range.getTo()/1000);
        return builder;
    }

    private static EntitySelector.Builder informedEntity(SituationAffectsBean bean) {
        EntitySelector.Builder builder = EntitySelector.newBuilder();
        if (bean.getAgencyId() != null)
            builder.setAgencyId(bean.getAgencyId());
        if (bean.getRouteId() != null)
            builder.setRouteId(bean.getRouteId());
        if (bean.getTripId() != null)
            builder.setTrip(TripDescriptor.newBuilder().setTripId(bean.getTripId()));
        if (bean.getStopId() != null)
            builder.setStopId(bean.getStopId());
        return builder;
    }

    private static TranslatedString.Builder translatedString(List<NaturalLanguageStringBean> beans) {
        TranslatedString.Builder string = TranslatedString.newBuilder();
        for (NaturalLanguageStringBean bean : beans) {
            TranslatedString.Translation.Builder tr = TranslatedString.Translation.newBuilder();
            tr.setLanguage(bean.getLang());
            tr.setText(bean.getValue());
            string.addTranslation(tr);
        }
        return string;
    }
}
