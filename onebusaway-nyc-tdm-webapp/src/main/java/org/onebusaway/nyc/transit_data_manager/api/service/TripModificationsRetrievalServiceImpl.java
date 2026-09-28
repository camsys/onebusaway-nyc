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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import org.onebusaway.realtime.gtfsrt.util.GtfsRealtimeDeserializer;


import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class TripModificationsRetrievalServiceImpl implements TripModificationsRetreivalService, ServletContextAware {

    private static final Logger log = LoggerFactory.getLogger(TripModificationsRetrievalServiceImpl.class);

    // JSON array of {feedId, url, enabled, updateIntervalMs}, e.g.
    // [{"feedId":"primary","url":"http://...","enabled":true,"updateIntervalMs":60000}]
    private static final String CONFIG_TRIP_MODS_FEEDS = "tdm.tripModificationsFeeds";
    private static final String CONFIG_TRIP_MODS_TIMEOUT = "tdm.tripModificationsConnectionTimeout";
    private static final String CONFIG_TRIP_MODS_CACHE_TIMEOUT = "tdm.tripModificationCacheTimeout";

    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15);
    private static final long DEFAULT_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);
    private static final long DEFAULT_CACHE_EXPIRATION_MS = TimeUnit.SECONDS.toMillis(120);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConfigurationService configurationService;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final DataFetcherFactory dataFetcherFactory;

    private volatile int connectionTimeoutMs = (int) DEFAULT_CONNECTION_TIMEOUT_MS;
    private volatile long cacheExpirationMs = DEFAULT_CACHE_EXPIRATION_MS;

    private String credentialsUsername;
    private String credentialsPassword;
    private Map<String, String> credentialsAuthHeaderMap = new HashMap<>();

    private volatile Map<String, TripModificationsFeedConfig> feedConfigsById = new HashMap<>();
    private final Map<String, DataFetcherDao> fetcherByFeed = new ConcurrentHashMap<>();
    private final Map<String, FeedCacheEntry> cacheByFeed = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTaskByFeed = new ConcurrentHashMap<>();

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
    }

    @Refreshable(dependsOn = {CONFIG_TRIP_MODS_FEEDS, CONFIG_TRIP_MODS_TIMEOUT, CONFIG_TRIP_MODS_CACHE_TIMEOUT})
    public synchronized void refreshConfig() {
        if (configurationService == null) {
            log.warn("Configuration service not available");
            return;
        }

        this.connectionTimeoutMs = configurationService.getConfigurationValueAsInteger(
                CONFIG_TRIP_MODS_TIMEOUT, (int) DEFAULT_CONNECTION_TIMEOUT_MS);

        this.cacheExpirationMs = configurationService.getConfigurationValueAsInteger(
                CONFIG_TRIP_MODS_CACHE_TIMEOUT, (int) DEFAULT_CACHE_EXPIRATION_MS);

        String feedsJson = configurationService.getConfigurationValueAsString(CONFIG_TRIP_MODS_FEEDS, "[]");
        Map<String, TripModificationsFeedConfig> newFeedConfigs = parseFeedConfigs(feedsJson);

        Set<String> removedFeedIds = new HashSet<>(feedConfigsById.keySet());
        removedFeedIds.removeAll(newFeedConfigs.keySet());
        for (String removedFeedId : removedFeedIds) {
            cancelScheduledTask(removedFeedId);
            fetcherByFeed.remove(removedFeedId);
            cacheByFeed.remove(removedFeedId);
            log.info("Trip modifications feed {} removed from configuration", removedFeedId);
        }

        for (TripModificationsFeedConfig feedConfig : newFeedConfigs.values()) {
            TripModificationsFeedConfig oldConfig = feedConfigsById.get(feedConfig.getFeedId());

            DataFetcherConnectionData connectionData = new DataFetcherConnectionData(
                    credentialsUsername, credentialsPassword, credentialsAuthHeaderMap);
            connectionData.setUrl(feedConfig.getUrl());
            connectionData.setConnectionTimeout(connectionTimeoutMs);
            connectionData.setReadTimeout(connectionTimeoutMs);
            fetcherByFeed.put(feedConfig.getFeedId(), dataFetcherFactory.getDataFetcher(connectionData));

            cacheByFeed.computeIfAbsent(feedConfig.getFeedId(), id -> new FeedCacheEntry());

            boolean needsReschedule = oldConfig == null
                    || oldConfig.isEnabled() != feedConfig.isEnabled()
                    || oldConfig.getUpdateIntervalMs() != feedConfig.getUpdateIntervalMs();
            if (needsReschedule) {
                rescheduleUpdates(feedConfig);
            }
        }

        feedConfigsById = newFeedConfigs;

        log.debug("Configuration refreshed - {} feed(s) configured", feedConfigsById.size());
    }

    private Map<String, TripModificationsFeedConfig> parseFeedConfigs(String feedsJson) {
        Map<String, TripModificationsFeedConfig> feedConfigs = new HashMap<>();
        try {
            JsonNode feeds = OBJECT_MAPPER.readTree(feedsJson);
            for (JsonNode feed : feeds) {
                String feedId = feed.get("feedId").asText();
                String url = feed.hasNonNull("url") ? feed.get("url").asText() : null;
                boolean enabled = feed.path("enabled").asBoolean(false);
                long updateIntervalMs = feed.hasNonNull("updateIntervalMs")
                        ? feed.get("updateIntervalMs").asLong() : DEFAULT_UPDATE_INTERVAL_MS;
                feedConfigs.put(feedId, new TripModificationsFeedConfig(feedId, url, enabled, updateIntervalMs));
            }
        } catch (Exception e) {
            log.error("Unable to parse {} config value '{}'; no Trip Modifications feeds will be polled until this is fixed",
                    CONFIG_TRIP_MODS_FEEDS, feedsJson, e);
        }
        return feedConfigs;
    }

    private void rescheduleUpdates(TripModificationsFeedConfig feedConfig) {
        cancelScheduledTask(feedConfig.getFeedId());
        if (taskScheduler != null && feedConfig.isEnabled()) {
            ScheduledFuture<?> task = taskScheduler.scheduleWithFixedDelay(
                    () -> updateTripModifications(feedConfig.getFeedId()), feedConfig.getUpdateIntervalMs());
            scheduledTaskByFeed.put(feedConfig.getFeedId(), task);
            log.info("Scheduled trip modifications updates for feed {} every {}ms",
                    feedConfig.getFeedId(), feedConfig.getUpdateIntervalMs());
        } else if (taskScheduler == null) {
            log.warn("Task scheduler not available - trip modifications for feed {} will not auto-update", feedConfig.getFeedId());
        }
    }

    private void cancelScheduledTask(String feedId) {
        ScheduledFuture<?> existing = scheduledTaskByFeed.remove(feedId);
        if (existing != null && !existing.isCancelled()) {
            boolean cancelled = existing.cancel(false);
            log.info("Cancelled existing scheduled task for feed {}: {}", feedId, cancelled);
        }
    }

    /**
     * Periodic update task that fetches and processes trip modifications for a single feed.
     */
    private void updateTripModifications(String feedId) {
        TripModificationsFeedConfig feedConfig = feedConfigsById.get(feedId);
        if (feedConfig == null || !feedConfig.isEnabled()) {
            log.debug("Trip modifications feed {} disabled or removed, clearing data", feedId);
            setTripModifications(feedId, null);
            return;
        }

        log.debug("Refreshing trip modifications for feed {}...", feedId);

        try {
            FeedMessage feedMessage = fetchFeed(feedId, feedConfig);
            if (feedMessage != null) {
                setTripModifications(feedId, feedMessage);
                log.debug("Refresh complete for feed {} - {} modifications loaded", feedId, feedMessage.getEntityCount());
            } else {
                log.warn("Failed to fetch feed {}, keeping existing data", feedId);
            }
        } catch (Exception e) {
            log.error("Error updating trip modifications for feed {}", feedId, e);
        }
    }

    /**
     * Fetches the GTFS-RT feed for a single feed id using its configured data fetcher.
     *
     * @return FeedMessage or null if fetch fails
     */
    private FeedMessage fetchFeed(String feedId, TripModificationsFeedConfig feedConfig) {
        String feedUrl = feedConfig.getUrl();
        if (feedUrl == null || feedUrl.trim().isEmpty()) {
            log.warn("Trip modifications feed URL not configured for feed {}", feedId);
            return null;
        }

        DataFetcherDao fetcher = fetcherByFeed.get(feedId);
        if (fetcher == null) {
            log.error("No data fetcher available for feed {} (URL: {})", feedId, feedUrl);
            return null;
        }

        log.info("Fetching GTFS-RT feed for feed {} from: {} using {}", feedId, feedUrl,
                fetcher.getClass().getSimpleName());

        try (InputStream inputStream = fetcher.fetchData()) {

            if (inputStream == null) {
                log.error("No response received from trip modifications feed {}", feedId);
                return null;
            }

            byte[] message = inputStream.readAllBytes();

            FeedMessage feedMessage = GtfsRealtimeDeserializer.parseFeedMessage(message);

            log.info("Successfully fetched GTFS-RT feed {} with {} entities",
                    feedId, feedMessage.getEntityCount());

            return feedMessage;

        } catch (IOException e) {
            log.error("Failed to fetch GTFS-RT feed for feed {} from {}: {}", feedId, feedUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets the current trip modifications for a single feed.
     * If the cache has expired (older than the configured cache timeout), fetches fresh data.
     *
     * @return FeedMessage, or null if the feed id is unknown or has no data yet
     */
    @Override
    public FeedMessage getTripModifications(String feedId) {
        FeedCacheEntry entry = cacheByFeed.get(feedId);
        if (entry == null) {
            log.warn("Requested trip modifications for unknown feed id {}", feedId);
            return null;
        }

        if (isCacheExpired(entry)) {
            log.debug("Cache expired for feed {}, fetching fresh trip modifications", feedId);
            refreshCacheIfNeeded(feedId, entry);
        }
        entry.lock.readLock().lock();
        try {
            return entry.tripModifications;
        } finally {
            entry.lock.readLock().unlock();
        }
    }

    private boolean isCacheExpired(FeedCacheEntry entry) {
        long age = System.currentTimeMillis() - entry.lastUpdateTimestamp;
        return age > cacheExpirationMs;
    }

    private void refreshCacheIfNeeded(String feedId, FeedCacheEntry entry) {
        // Only one thread should refresh a given feed's cache at a time
        if (entry.lock.writeLock().tryLock()) {
            try {
                if (isCacheExpired(entry)) {
                    log.debug("Performing on-demand cache refresh for feed {}", feedId);
                    updateTripModifications(feedId);
                }
            } finally {
                entry.lock.writeLock().unlock();
            }
        } else {
            log.debug("Another thread is refreshing feed {}'s cache, waiting...", feedId);
        }
    }

    private void setTripModifications(String feedId, FeedMessage feedMessage) {
        FeedCacheEntry entry = cacheByFeed.computeIfAbsent(feedId, id -> new FeedCacheEntry());
        entry.lock.writeLock().lock();
        try {
            entry.tripModifications = feedMessage;
            entry.lastUpdateTimestamp = System.currentTimeMillis();
            log.debug("Cache updated for feed {} with {} modifications at timestamp {}",
                    feedId, feedMessage != null ? feedMessage.getEntityCount() : 0, entry.lastUpdateTimestamp);
        } finally {
            entry.lock.writeLock().unlock();
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (servletContext != null) {
            credentialsUsername = servletContext.getInitParameter("tripmods.user");
            log.info("servlet context provided tripmods.user=" + credentialsUsername);

            credentialsPassword = servletContext.getInitParameter("tripmods.password");
            if (credentialsPassword != null) {
                log.info("servlet context provided tripmods.password=[REDACTED]");
            }

            credentialsAuthHeaderMap = new HashMap<>();
            String credentialsAuthHeader = servletContext.getInitParameter("tripmods.authHeader");
            if (credentialsAuthHeader != null) {
                log.info("servlet context provided tripmods.header=" + credentialsAuthHeader);
                credentialsAuthHeaderMap.put(credentialsAuthHeader, credentialsPassword);
            }
        }
    }

    /** Per-feed cache state: the last fetched feed, when it was fetched, and a lock guarding both. */
    private static final class FeedCacheEntry {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private volatile FeedMessage tripModifications;
        private volatile long lastUpdateTimestamp = 0;
    }
}
