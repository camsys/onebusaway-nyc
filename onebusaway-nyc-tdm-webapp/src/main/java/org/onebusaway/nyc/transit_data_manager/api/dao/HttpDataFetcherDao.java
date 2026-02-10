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

package org.onebusaway.nyc.transit_data_manager.api.dao;

import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherConnectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * HTTP client for fetching data from remote endpoints.
 * Handles connection setup, timeouts, and error handling.
 */
public class HttpDataFetcherDao implements DataFetcherDao {

    private static final Logger log = LoggerFactory.getLogger(HttpDataFetcherDao.class);

    private static final long DEFAULT_CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final long DEFAULT_READ_TIMEOUT = 5000; // 5 seconds

    private String url;
    private int connectionTimeout;
    private int readTimeout;
    private String username;
    private String password;
    private Map<String, String> httpAuthHeaders;

    public HttpDataFetcherDao(DataFetcherConnectionData connectionData) {

        this.url = connectionData.getUrl();
        this.connectionTimeout = connectionData.getConnectionTimeout();
        this.readTimeout = connectionData.getReadTimeout();
        this.username = connectionData.getUsername();
        this.password = connectionData.getPassword();
        this.httpAuthHeaders = connectionData.getHttpHeaders();
    }

    /**
     * Fetches data from the assigned URL.
     * @return InputStream containing the data, or null if retrieval fails
     */
    @Override
    public InputStream fetchData() {
        if (url == null || url.trim().isEmpty()) {
            log.warn("URL not configured");
            return null;
        }

        HttpURLConnection connection = null;
        try {
            connection = openConnection(url, connectionTimeout, readTimeout);

            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            connection.setRequestProperty("Accept", "*/*");

            if (httpAuthHeaders != null && !httpAuthHeaders.isEmpty()) {
                for (Map.Entry<String, String> e : httpAuthHeaders.entrySet()) {
                    String k = e.getKey();
                    String v = e.getValue();
                    if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
                        connection.setRequestProperty(k, v);
                    }
                }
            }

            int status = connection.getResponseCode();

            // Prefer error stream on failure, but don't blow up if it's missing
            if (status >= 400) {
                InputStream err = connection.getErrorStream();
                log.warn("Failed to retrieve data from {}: HTTP {} {}",
                        url, status, safeResponseMessage(connection));
                return err;
            }

            return connection.getInputStream();

        } catch (IOException e) {
            log.error("Failed to retrieve data from {}: {}",
                    url, e.getMessage(), e);
            if (connection != null) {
                connection.disconnect();
            }
            return null;
        }
    }

    private static String safeResponseMessage(HttpURLConnection connection) {
        try {
            String msg = connection.getResponseMessage();
            return (msg == null) ? "" : msg;
        } catch (IOException ignored) {
            return "";
        }
    }


    /**
     * Opens an HTTP connection with the specified timeouts.
     */
    private HttpURLConnection openConnection(String urlString,
                                             int connectionTimeout,
                                             int readTimeout) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setRequestMethod("GET");
        return connection;
    }
}
