package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.GtfsTripModificationsClient;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.GtfsTripModificationsClientImpl;
import org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications.TripModificationConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class NycGtfsTripModificationsClientImpl extends GtfsTripModificationsClientImpl implements GtfsTripModificationsClient {

    private static final String CONFIG_TRIP_MODS_REFRESH_INTERVAL = "tds.tripModificationsRefreshInterval";
    private static final String CONFIG_TRIP_MODS_URL = "tds.tripModificationsUrl";
    private static final String CONFIG_TRIP_MODS_ENABLED = "tds.tripModificationsEnabled";
    private static final String SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY = "tds.tripModifications.shapeOverlapThresholdMeters";
    private static final String DEFAULT_SHAPE_OVERLAP_THRESHOLD_METERS = "25.0";

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
    public synchronized void update() {
        while (_bundleManagementService == null || !_bundleManagementService.bundleIsReady()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        super.update();
    }

    @Refreshable(dependsOn = {CONFIG_TRIP_MODS_REFRESH_INTERVAL, CONFIG_TRIP_MODS_URL, CONFIG_TRIP_MODS_ENABLED,
            SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY})
    protected void refreshCache() {
        updateConfig();
        reapplyTripModifications();
    }

    private void updateConfig(){
        int refreshInterval = Integer.parseInt(_configurationService.getConfigurationValueAsString(
                CONFIG_TRIP_MODS_REFRESH_INTERVAL, "60"));

        String tripModificationsUrl = _configurationService.getConfigurationValueAsString(CONFIG_TRIP_MODS_URL, null);

        boolean tripModificationsEnabled = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString(
                CONFIG_TRIP_MODS_ENABLED, "false"));

        setRefreshInterval(refreshInterval);
        setGtfsTripModificationsUrl(tripModificationsUrl);
        setEnabled(tripModificationsEnabled);
        setTripModificationConfiguration(getTripModificationConfiguration());
    }

    private TripModificationConfiguration getTripModificationConfiguration() {
        TripModificationConfiguration tripModificationConfiguration = new TripModificationConfiguration();

        double shapeOverlapThresholdMeters = Double.parseDouble(_configurationService.getConfigurationValueAsString(
                SHAPE_OVERLAP_THRESHOLD_CONFIG_KEY, DEFAULT_SHAPE_OVERLAP_THRESHOLD_METERS));

        tripModificationConfiguration.setShapeOverlapThreshold(shapeOverlapThresholdMeters);

        return tripModificationConfiguration;
    }
}
