package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.GtfsTripModificationsClient;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.GtfsTripModificationsClientImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.TripModificationConfiguration;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.model.TripModificationsFeedDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class NycGtfsTripModificationsClientImpl extends GtfsTripModificationsClientImpl implements GtfsTripModificationsClient {

    private static final Logger _log = LoggerFactory.getLogger(NycGtfsTripModificationsClientImpl.class);

    // A JSON array of {feedId, url, enabled, refreshIntervalSeconds, priority} objects, e.g.
    // [{"feedId":"primary","url":"http://...","enabled":true,"refreshIntervalSeconds":60,"priority":0}]
    private static final String CONFIG_TRIP_MODS_FEEDS = "tds.tripModificationsFeeds";
    private static final String SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY = "tds.tripModifications.shapeOverlapThresholdMeters";
    private static final String DEFAULT_SHAPE_OVERLAP_THRESHOLD_METERS = "25.0";
    private static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 60;

    @Autowired
    private BundleManagementService _bundleManagementService;

    private final ConfigurationService _configurationService;

    @Autowired
    public NycGtfsTripModificationsClientImpl(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @Override
    @PostConstruct
    public void init() {
        updateConfig();
        super.init();
    }

    @Override
    public void update(String feedId) {
        while (_bundleManagementService == null || !_bundleManagementService.bundleIsReady()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        super.update(feedId);
    }

    @Refreshable(dependsOn = {CONFIG_TRIP_MODS_FEEDS, SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY})
    protected void refreshCache() {
        updateConfig();
        reapplyTripModifications();
    }

    private void updateConfig(){
        String feedsJson = _configurationService.getConfigurationValueAsString(CONFIG_TRIP_MODS_FEEDS, "[]");
        setFeedDefinitions(parseFeedDefinitions(feedsJson));
        setTripModificationConfiguration(getTripModificationConfiguration());
    }

    private List<TripModificationsFeedDefinition> parseFeedDefinitions(String feedsJson) {
        List<TripModificationsFeedDefinition> feedDefinitions = new ArrayList<>();
        try {
            JsonArray feeds = new JsonParser().parse(feedsJson).getAsJsonArray();
            for (JsonElement feedElement : feeds) {
                JsonObject feed = feedElement.getAsJsonObject();
                String feedId = feed.get("feedId").getAsString();
                String url = feed.has("url") ? feed.get("url").getAsString() : null;
                boolean enabled = feed.has("enabled") && feed.get("enabled").getAsBoolean();
                int refreshIntervalSeconds = feed.has("refreshIntervalSeconds")
                        ? feed.get("refreshIntervalSeconds").getAsInt() : DEFAULT_REFRESH_INTERVAL_SECONDS;
                int priority = feed.has("priority") ? feed.get("priority").getAsInt() : 0;
                feedDefinitions.add(new TripModificationsFeedDefinition(feedId, url, enabled, refreshIntervalSeconds, priority));
            }
        } catch (Exception e) {
            _log.error("Unable to parse {} config value '{}'; no Trip Modifications feeds will be polled until this is fixed",
                    CONFIG_TRIP_MODS_FEEDS, feedsJson, e);
        }
        return feedDefinitions;
    }

    private TripModificationConfiguration getTripModificationConfiguration() {
        TripModificationConfiguration tripModificationConfiguration = new TripModificationConfiguration();

        double shapeOverlapThresholdMeters = Double.parseDouble(_configurationService.getConfigurationValueAsString(
                SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY, DEFAULT_SHAPE_OVERLAP_THRESHOLD_METERS));

        tripModificationConfiguration.setShapeOverlapThreshold(shapeOverlapThresholdMeters);

        return tripModificationConfiguration;
    }
}
