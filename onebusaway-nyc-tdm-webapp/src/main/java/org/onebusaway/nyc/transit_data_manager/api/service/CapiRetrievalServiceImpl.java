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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data_manager.api.IncomingNycCancelledTripBeansContainer;
import org.onebusaway.nyc.transit_data_manager.api.dao.DataFetcherDao;
import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherFactory;
import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherConnectionData;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class CapiRetrievalServiceImpl implements CapiRetrievalService, ServletContextAware {

    private static final Logger log = LoggerFactory.getLogger(CapiRetrievalServiceImpl.class);

    private static final String CONFIG_CAPI_ENABLED = "tdm.enableCapi";
    private static final String CONFIG_CAPI_URL = "tdm.capiUrl";
    private static final String CONFIG_CAPI_TIMEOUT = "tdm.capiConnectionTimeout";
    private static final String CONFIG_CAPI_UPDATE_INTERVAL = "tdm.capiRefreshInterval";
    private static final String CONFIG_CAPI_CACHE_TIMEOUT = "tdm.capiCacheTimeout";


    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long DEFAULT_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);
    private static final long DEFAULT_CACHE_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(120);

    private final ConfigurationService configurationService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private boolean enabled = true;
    private String feedUrl;
    private int connectionTimeoutMs;
    private long updateIntervalMs;
    private long cacheExpirationMs;
    private DataFetcherDao currentFetcher;
    private final DataFetcherFactory dataFetcherFactory;
    private DataFetcherConnectionData dataFetcherConnectionData;

    List<CancelledTripBean> _cancelledTripBeans;

    private ObjectReader _objectReader = new ObjectMapper()
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .setTimeZone(Calendar.getInstance().getTimeZone())
            .readerFor(IncomingNycCancelledTripBeansContainer.class);

    private long lastUpdateTimestamp = 0;

    // Lock for thread-safe cache updates
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // Scheduled task for periodic updates
    private ScheduledFuture<?> scheduledTask;

    @Autowired
    public CapiRetrievalServiceImpl(
            ConfigurationService configurationService,
            DataFetcherFactory dataFetcherFactory,
            ThreadPoolTaskScheduler taskScheduler) {
        this.configurationService = configurationService;
        this.taskScheduler = taskScheduler;
        this.dataFetcherFactory = dataFetcherFactory;
    }

    @PostConstruct
    public void setup() {
        log.info("Initializing CapiRetrievalService");
        refreshConfig();
        scheduleDataUpdates();
    }

    @Refreshable(dependsOn = {CONFIG_CAPI_ENABLED, CONFIG_CAPI_URL,CONFIG_CAPI_TIMEOUT,
            CONFIG_CAPI_UPDATE_INTERVAL, CONFIG_CAPI_CACHE_TIMEOUT})
    public synchronized void refreshConfig() {
        if (configurationService == null) {
            log.warn("Configuration service not available");
            return;
        }

        boolean oldEnabled = this.enabled;
        this.enabled = configurationService.getConfigurationValueAsBoolean(CONFIG_CAPI_ENABLED, false);

        this.feedUrl = configurationService.getConfigurationValueAsString(CONFIG_CAPI_URL, null);

        this.connectionTimeoutMs = configurationService.getConfigurationValueAsInteger(CONFIG_CAPI_URL,
                (int) DEFAULT_CONNECTION_TIMEOUT_MS);

        long oldUpdateIntervalMs = this.updateIntervalMs;
        this.updateIntervalMs = configurationService.getConfigurationValueAsInteger(
                CONFIG_CAPI_UPDATE_INTERVAL, (int) DEFAULT_UPDATE_INTERVAL_MS);

        this.cacheExpirationMs = configurationService.getConfigurationValueAsInteger(
                CONFIG_CAPI_CACHE_TIMEOUT, (int) DEFAULT_CACHE_EXPIRATION_MS);

        dataFetcherConnectionData.setUrl(this.feedUrl);
        dataFetcherConnectionData.setConnectionTimeout(this.connectionTimeoutMs);
        dataFetcherConnectionData.setReadTimeout(this.connectionTimeoutMs);

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
            scheduledTask = taskScheduler.scheduleWithFixedDelay(this::updateCancelledTripBeans, updateIntervalMs);
            log.info("Scheduled capi updates every {}ms", updateIntervalMs);
        } else {
            log.warn("Task scheduler not available or Capi disabled - Capi will not auto-update");
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

    public void updateCancelledTripBeans() {
        try {
            InputStream inputStream = fetchFeed();
            List<CancelledTripBean> cancelledTripBeans = convertCapiInputToCancelledTripBeans(inputStream);
            setCancelledTripBeans(cancelledTripBeans);
        }catch(Exception e) {
            log.error("Error while trying to fetch data from Capi {}", dataFetcherConnectionData.getUrl(), e);
        }
    }


    private List<CancelledTripBean> convertCapiInputToCancelledTripBeans(InputStream input) {
        log.debug("reading from stream...");
        try {
            IncomingNycCancelledTripBeansContainer beansContainer = _objectReader.readValue(input, IncomingNycCancelledTripBeansContainer.class);
            if (beansContainer != null && beansContainer.getBeans() != null) {
                log.debug("parsed " + beansContainer.getBeans().size() + " records");
            } else {
                log.debug("empty beanContainer");
            }
            List<CancelledTripBean> validBeans = new ArrayList<>();
            assert beansContainer != null;
            for (CancelledTripBean bean : beansContainer.getBeans()) {
                /*if (isValid(bean)) {
                    validBeans.add(bean);
                }*/
                validBeans.add(bean);
            }
            if(!beansContainer.getBeans().isEmpty() && validBeans.isEmpty()){
                log.warn("Found {} cancelled trips but none of them were valid", beansContainer.getBeans().size());
            }
            log.debug("found {} valid beans", validBeans.size());
            return validBeans;

        } catch (Exception any) {
            log.error("issue parsing json: " + any, any);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getLocation() {
        return dataFetcherConnectionData.getUrl();
    }

    @Override
    public List<CancelledTripBean> getCancelledTripBeans(){
        // Check if cache is expired
        if (isCacheExpired()) {
            log.debug("Cache expired, fetching fresh cancelled trips");
            refreshCacheIfNeeded();
        }
        cacheLock.readLock().lock();
        try {
            return _cancelledTripBeans;
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Fetches the CAPI feed from the configured URL using the appropriate data fetcher.
     *
     * @return InputStream or null if fetch fails
     */
    private InputStream fetchFeed() {
        if (feedUrl == null || feedUrl.trim().isEmpty()) {
            log.warn("Capi feed URL not configured");
            return null;
        }

        if (currentFetcher == null) {
            log.error("No data fetcher available for URL: {}", feedUrl);
            return null;
        }

        log.info("Fetching Capi feed from: {} using {}", feedUrl,
                currentFetcher.getClass().getSimpleName());

        try (InputStream inputStream = currentFetcher.fetchData()) {

            if (inputStream == null) {
                log.error("No response received from Capi feed");
                return null;
            }

            log.info("Successfully fetched Capi feed");

            return inputStream;

        } catch (IOException e) {
            log.error("Failed to fetch Capi feed from {}: {}", feedUrl, e.getMessage(), e);
            return null;
        }
    }


    /**
     * Checks if the cache has expired.
     *
     * @return true if cache is older than 60 seconds
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
                    updateCancelledTripBeans();
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
     * Sets the capi and updates the cache timestamp.
     *
     * @param cancelledTripBeans list of cancelled trip beans
     */
    @Override
    public void setCancelledTripBeans(List<CancelledTripBean> cancelledTripBeans) {
        cacheLock.writeLock().lock();
        try {
            _cancelledTripBeans = cancelledTripBeans;
            this.lastUpdateTimestamp = System.currentTimeMillis();
            log.debug("Capi cache updated at timestamp {}", lastUpdateTimestamp);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Checks if capi is enabled.
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
            String credentialsUsername = servletContext.getInitParameter("capi.user");
            log.info("servlet context provided capi.user=" + credentialsUsername);

            String credentialsPassword = servletContext.getInitParameter("capi.password");
            if (credentialsPassword != null) {
                log.info("servlet context provided capi.password=[REDACTED]");
            }

            Map<String, String> credentialsAuthHeaderMap = new HashMap<>();
            String credentialsAuthHeader = servletContext.getInitParameter("capi.authHeader");
            if (credentialsAuthHeader != null) {
                log.info("servlet context provided capi.header=" + credentialsAuthHeader);
                credentialsAuthHeaderMap.put(credentialsAuthHeader, credentialsPassword);
            }

            dataFetcherConnectionData = new DataFetcherConnectionData(credentialsUsername, credentialsPassword, credentialsAuthHeaderMap);
        }
    }
}