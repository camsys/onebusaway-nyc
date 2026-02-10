/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.transit_data_manager.api.service;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data_manager.api.dao.DataFetcherDao;
import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherFactory;
import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherConnectionData;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class TripModificationsRetrievalServiceImpl implements TripModificationsRetreivalService, ServletContextAware {

    private static final Logger log = LoggerFactory.getLogger(TripModificationsRetrievalServiceImpl.class);

    private static final String CONFIG_TRIP_MODS_URL = "tdm.tripModificationsUrl";
    private static final String CONFIG_TRIP_MODS_TIMEOUT = "tdm.tripModificationsConnectionTimeout";
    private static final String CONFIG_TRIP_MODS_ENABLED = "tdm.tripModificationsEnabled";
    private static final String CONFIG_TRIP_MODS_UPDATE_INTERVAL = "tdm.tripModificationsUpdateInterval";
    private static final String CONFIG_TRIP_MODS_CACHE_TIMEOUT = "tdm.tripModificationCacheTimeout";


    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long DEFAULT_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);
    private static final long DEFAULT_CACHE_EXPIRATION_MS = TimeUnit.SECONDS.toMillis(120);

    private final ConfigurationService configurationService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private String feedUrl;
    private boolean enabled;
    private int connectionTimeoutMs;
    private long updateIntervalMs;
    private long cacheExpirationMs;
    private DataFetcherDao currentFetcher;
    private final DataFetcherFactory dataFetcherFactory;
    private DataFetcherConnectionData dataFetcherConnectionData;

    private FeedMessage tripModifications;
    private long lastUpdateTimestamp = 0;

    // Lock for thread-safe cache updates
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // Scheduled task for periodic updates
    private ScheduledFuture<?> scheduledTask;

    @Autowired
    public TripModificationsRetrievalServiceImpl(
            ConfigurationService configurationService,
            DataFetcherFactory dataFetcherFactory,
            ThreadPoolTaskScheduler taskScheduler) {
        this.configurationService = configurationService;
        this.taskScheduler = taskScheduler;
        this.dataFetcherFactory = dataFetcherFactory;
    }

    @PostConstruct
    public void setup() {
        log.info("Initializing TripModificationsRetrievalService");
        refreshConfig();
        scheduleDataUpdates();
    }

    @Refreshable(dependsOn = {CONFIG_TRIP_MODS_URL, CONFIG_TRIP_MODS_TIMEOUT, CONFIG_TRIP_MODS_ENABLED,
            CONFIG_TRIP_MODS_UPDATE_INTERVAL, CONFIG_TRIP_MODS_CACHE_TIMEOUT})
    public synchronized void refreshConfig() {
        if (configurationService == null) {
            log.warn("Configuration service not available");
            return;
        }

        boolean oldEnabled = this.enabled;
        this.enabled = configurationService.getConfigurationValueAsBoolean(CONFIG_TRIP_MODS_ENABLED, false);

        this.feedUrl = configurationService.getConfigurationValueAsString(CONFIG_TRIP_MODS_URL, null);

        this.connectionTimeoutMs = configurationService.getConfigurationValueAsInteger(
                CONFIG_TRIP_MODS_TIMEOUT,
                (int) DEFAULT_CONNECTION_TIMEOUT_MS
        );

        long oldUpdateIntervalMs = this.updateIntervalMs;
        this.updateIntervalMs = configurationService.getConfigurationValueAsInteger(
                CONFIG_TRIP_MODS_UPDATE_INTERVAL,
                (int) DEFAULT_UPDATE_INTERVAL_MS
        );

        this.cacheExpirationMs = configurationService.getConfigurationValueAsInteger(
                CONFIG_TRIP_MODS_CACHE_TIMEOUT,
                (int) DEFAULT_CACHE_EXPIRATION_MS
        );

        dataFetcherConnectionData.setUrl(this.feedUrl);
        dataFetcherConnectionData.setConnectionTimeout((int) this.connectionTimeoutMs);
        dataFetcherConnectionData.setReadTimeout((int) this.connectionTimeoutMs);

        // Determine which data fetcher to use based on URL scheme
        this.currentFetcher = dataFetcherFactory.getDataFetcher(dataFetcherConnectionData);

        log.debug("Configuration refreshed - URL: {}, Enabled: {}, Update Interval: {}ms, Fetcher: {}",
                feedUrl != null ? feedUrl : "not set", enabled, updateIntervalMs,
                currentFetcher != null ? currentFetcher.getClass().getSimpleName() : "none");

        if(oldEnabled != this.enabled || oldUpdateIntervalMs != this.updateIntervalMs) {
            rescheduleUpdates();
        }
    }

    private void scheduleDataUpdates() {
        if (taskScheduler != null && enabled) {
            taskScheduler.scheduleWithFixedDelay(this::updateTripModifications, updateIntervalMs);
            log.info("Scheduled trip modifications updates every {}ms", updateIntervalMs);
        } else {
            log.warn("Task scheduler not available - trip modifications will not auto-update");
        }
    }

    private void rescheduleUpdates() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            boolean cancelled = scheduledTask.cancel(false);
            log.info("Cancelled existing scheduled task: {}", cancelled);
            scheduledTask = null;
        }
        scheduleDataUpdates();
    }



    // TODO
    private void rescheduleUpdatesIfNeeded() {
        log.debug("Update interval changed, new tasks will use updated interval");
    }

    /**
     * Periodic update task that fetches and processes trip modifications.
     */
    private void updateTripModifications() {
        if (!enabled) {
            log.debug("Trip modifications disabled, clearing data");
            setTripModifications(null);
            return;
        }

        log.debug("Refreshing trip modifications...");

        try {
            FeedMessage feedMessage = fetchFeed();
            if (feedMessage != null) {
                // Process the feed message and extract trip modifications
                //List<Object> modifications = processFeedMessage(feedMessage);
                setTripModifications(feedMessage);
                log.debug("Refresh complete - {} modifications loaded", feedMessage.getEntityCount());
            } else {
                log.warn("Failed to fetch feed, keeping existing data");
            }
        } catch (Exception e) {
            log.error("Error updating trip modifications", e);
        }
    }

    /**
     * Fetches the GTFS-RT feed from the configured URL using the appropriate data fetcher.
     *
     * @return FeedMessage or null if fetch fails
     */
    private FeedMessage fetchFeed() {
        if (feedUrl == null || feedUrl.trim().isEmpty()) {
            log.warn("Trip modifications feed URL not configured");
            return null;
        }

        if (currentFetcher == null) {
            log.error("No data fetcher available for URL: {}", feedUrl);
            return null;
        }

        log.info("Fetching GTFS-RT feed from: {} using {}", feedUrl,
                currentFetcher.getClass().getSimpleName());

        try (InputStream inputStream = currentFetcher.fetchData()) {

            if (inputStream == null) {
                log.error("No response received from trip modifications feed");
                return null;
            }

            FeedMessage feedMessage = FeedMessage.parseFrom(inputStream);
            log.info("Successfully fetched GTFS-RT feed with {} entities",
                    feedMessage.getEntityCount());
            return feedMessage;

        } catch (IOException e) {
            log.error("Failed to fetch GTFS-RT feed from {}: {}", feedUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Processes the feed message and extracts trip modifications.
     * Override this method to implement actual processing logic.
     *
     * @param feedMessage the GTFS-RT feed message
     * @return list of trip modifications
     */
    protected List<Object> processFeedMessage(FeedMessage feedMessage) {
        // TODO: Implement actual processing logic
        // This is a placeholder - replace with actual trip modification extraction
        log.debug("Processing feed message with {} entities", feedMessage.getEntityCount());
        return Collections.emptyList();
    }

    /**
     * Gets the current trip modifications.
     * If the cache has expired (older than 60 seconds), fetches fresh data.
     *
     * @return list of trip modifications
     */
    @Override
    public FeedMessage getTripModifications() {
        // Check if cache is expired
        if (isCacheExpired()) {
            log.debug("Cache expired, fetching fresh trip modifications");
            refreshCacheIfNeeded();
        }
        cacheLock.readLock().lock();
        try {
            return tripModifications;
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Checks if the cache has expired.
     *
     * @return true if cache is older than cacheExpirationMs
     */
    private boolean isCacheExpired() {
        long age = System.currentTimeMillis() - lastUpdateTimestamp;
        return age > cacheExpirationMs;
    }

    /**
     * Refreshes the cache if needed, preventing multiple concurrent refreshes.
     */
    private void refreshCacheIfNeeded() {
        // Only one thread should refresh at a time
        if (cacheLock.writeLock().tryLock()) {
            try {
                // Double-check expiration after acquiring lock
                if (isCacheExpired() && enabled) {
                    log.debug("Performing on-demand cache refresh");
                    updateTripModifications();
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        } else {
            // Another thread is already refreshing, wait for it
            log.debug("Another thread is refreshing cache, waiting...");
        }
    }

    /**
     * Sets the trip modifications and updates the cache timestamp.
     *
     * @param feedMessage list of trip modifications
     */
    protected void setTripModifications(FeedMessage feedMessage) {
        cacheLock.writeLock().lock();
        try {
            this.tripModifications = feedMessage;
            this.lastUpdateTimestamp = System.currentTimeMillis();
            log.debug("Cache updated with {} modifications at timestamp {}",
                    feedMessage != null ? feedMessage.getEntityCount() : 0, lastUpdateTimestamp);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Checks if trip modifications are enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the configured feed URL.
     *
     * @return feed URL
     */
    public String getFeedUrl() {
        return feedUrl;
    }

    /**
     * Gets the age of the current cache in milliseconds.
     *
     * @return cache age in milliseconds
     */
    public long getCacheAge() {
        return System.currentTimeMillis() - lastUpdateTimestamp;
    }

    /**
     * Gets the timestamp of the last cache update.
     *
     * @return timestamp in milliseconds since epoch
     */
    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (servletContext != null) {
            String credentialsUsername = servletContext.getInitParameter("tripmods.user");
            log.info("servlet context provided tripmods.user=" + credentialsUsername);

            String credentialsPassword = servletContext.getInitParameter("tripmods.password");
            if (credentialsPassword != null) {
                log.info("servlet context provided tripmods.password=[REDACTED]");
            }

            Map<String, String> credentialsAuthHeaderMap = new HashMap<>();
            String credentialsAuthHeader = servletContext.getInitParameter("tripmods.authHeader");
            if (credentialsAuthHeader != null) {
                log.info("servlet context provided tripmods.header=" + credentialsAuthHeader);
                credentialsAuthHeaderMap.put(credentialsAuthHeader, credentialsPassword);
            }

            dataFetcherConnectionData = new DataFetcherConnectionData(credentialsUsername, credentialsPassword, credentialsAuthHeaderMap);
        }
    }
}