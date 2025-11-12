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

package org.onebusaway.nyc.transit_data_manager.job;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTTP implementation for fetching depot assignments
 */
@Component
public class HttpDepotAssignsSource implements DepotAssignsSource {

    private static final Logger _log = LoggerFactory.getLogger(HttpDepotAssignsSource.class);
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;

    private ConfigurationService _configurationService;

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @Override
    public InputStream fetchDepotAssignments() throws IOException {
        String url = getUrl();
        int connectionTimeout = getConnectionTimeout();

        if (StringUtils.isBlank(url)) {
            throw new IOException("Depot assigns URL not configured");
        }

        long start = System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(connectionTimeout);

        InputStream inputStream = connection.getInputStream();
        _log.debug("Retrieved {} in {} ms", url, (System.currentTimeMillis() - start));

        return inputStream;
    }

    @Override
    public String getSourceType() {
        return "HTTP";
    }

    @Override
    public boolean isAvailable() {
        String url = getUrl();
        return StringUtils.isNotBlank(url);
    }

    private String getUrl() {
        return _configurationService.getConfigurationValueAsString("tdm.depotAssigns.url", null);
    }

    private int getConnectionTimeout() {
        return _configurationService.getConfigurationValueAsInteger(
                "tdm.depotAssigns.connectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
    }
}