package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime.*;
import junit.framework.TestCase;
import org.junit.Test;
import org.onebusaway.nyc.gtfsrt.impl.ServiceAlertFeedBuilderImpl;
import org.onebusaway.nyc.gtfsrt.service.ServiceAlertFeedBuilder;
import org.onebusaway.nyc.gtfsrt.util.ServiceAlertReader;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;

import java.util.List;

public class ServiceAlertTest extends TestCase {
    private String inferenceFile;

    private ServiceAlertFeedBuilder feedBuilder = new ServiceAlertFeedBuilderImpl();

    public ServiceAlertTest(String inferenceFile) {
        this.inferenceFile = inferenceFile;
    }

    @Test
    public void test() {
        List<ServiceAlertBean> records = new ServiceAlertReader().getRecords(inferenceFile);
        assertFalse(records.isEmpty());

        for (ServiceAlertBean bean : records) {
            Alert alert = feedBuilder.getAlertFromServiceAlert(bean);
            assertServiceAlertMatches(bean, alert);
        }
    }

    private static void assertServiceAlertMatches(ServiceAlertBean bean, Alert feed) {
        assertActivePeriodMatches(bean.getPublicationWindows(), feed.getActivePeriodList());
        assertInformedEntityMatches(bean.getAllAffects(), feed.getInformedEntityList());

        if (feed.hasCause())
            assertCauseMatches(bean.getReason(), feed.getCause());
        else
            assertNull(bean.getReason());

        if (feed.hasEffect())
            assertEffectMatches(bean.getConsequences(), feed.getEffect());
        else
            assertNull(bean.getConsequences());

        if (feed.hasUrl())
            assertLanguageStringsMatch(bean.getUrls(), feed.getUrl());
        else
            assertNull(bean.getUrls());

        if (feed.hasHeaderText())
            assertLanguageStringsMatch(bean.getSummaries(), feed.getHeaderText());
        else
            assertNull(bean.getSummaries());

        if (feed.hasDescriptionText())
            assertLanguageStringsMatch(bean.getDescriptions(), feed.getDescriptionText());
        else
            assertNull(bean.getDescriptions());
    }

    private static void assertActivePeriodMatches(List<TimeRangeBean> windows, List<TimeRange> periods) {
        if (windows == null || windows.isEmpty()) {
            assertTrue(periods == null || periods.isEmpty());
        }
        assertEquals(windows.size(), periods.size());

        for (TimeRangeBean window : windows) {
            boolean foundMatch = false;
            for (TimeRange period : periods) {
                foundMatch |= windowPeriodMatch(window, period);
            }
            assertTrue(foundMatch);
        }
    }

    private static boolean windowPeriodMatch(TimeRangeBean window, TimeRange period) {
        if ((window.getFrom() > 0) != period.hasStart())
            return false;
        else if (period.hasStart() && period.getStart() != window.getFrom()/1000)
            return false;

        if ((window.getTo() > 0) != period.hasEnd())
            return false;
        else if (period.hasEnd() && period.getEnd() != window.getTo()/1000)
            return false;

        return true;
    }

    private static void assertInformedEntityMatches(List<SituationAffectsBean> affects, List<EntitySelector> entities) {
        if (affects == null || affects.isEmpty()) {
            assertTrue(entities == null || entities.isEmpty());
        }
        assertEquals(affects.size(), entities.size());
        for (SituationAffectsBean bean : affects) {
            boolean foundMatch = false;
            for (EntitySelector entity : entities) {
                foundMatch |= affectsEntityMatch(bean, entity);
            }
           assertTrue(foundMatch);
        }
    }

    private static boolean affectsEntityMatch(SituationAffectsBean affects, EntitySelector entity) {
        if ((affects.getAgencyId() != null) != entity.hasAgencyId())
            return false;
        if (affects.getAgencyId() != null && !affects.getAgencyId().equals(entity.getAgencyId()))
            return false;
        if ((affects.getRouteId() != null) != entity.hasRouteId())
            return false;
        if (affects.getRouteId() != null && !affects.getRouteId().equals(entity.getRouteId()))
            return false;
        if ((affects.getTripId() != null) != entity.hasTrip())
            return false;
        if (entity.hasTrip() && !entity.getTrip().getTripId().equals(affects.getTripId()))
            return false;
        if ((affects.getStopId() != null) != entity.hasStopId())
            return false;
        if (entity.hasStopId() && !affects.getStopId().equals(entity.getStopId()))
            return false;
        return true;
    }

    private static void assertCauseMatches(String reason, Alert.Cause cause) {
        assertEquals(cause.toString(), reason);
    }

    private static void assertEffectMatches(List<SituationConsequenceBean> consequences, Alert.Effect effect) {
        boolean foundMatch = false;
        for (SituationConsequenceBean bean : consequences) {
            EEffect beanEffect = bean.getEffect();
            foundMatch |= beanEffect.toString().equals(effect.toString());
        }
       assertTrue(foundMatch);
    }

    private static void assertLanguageStringsMatch(List<NaturalLanguageStringBean> beans, TranslatedString translatedString) {
        List<TranslatedString.Translation> translations = translatedString.getTranslationList();
        assertEquals(translations.size(), beans.size());
        for (NaturalLanguageStringBean bean : beans) {
            boolean foundMatch = false;
            for (TranslatedString.Translation ts : translations) {
                foundMatch |= bean.getLang().equals(ts.getLanguage()) && bean.getValue().equals(ts.getText());
            }
            assertTrue(foundMatch);
        }
    }
}
