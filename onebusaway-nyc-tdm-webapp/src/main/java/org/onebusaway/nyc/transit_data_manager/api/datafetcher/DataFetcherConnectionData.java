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

package org.onebusaway.nyc.transit_data_manager.api.datafetcher;

import java.util.Map;

/**
 * Factory for selecting the appropriate DataFetcher implementation based on URL scheme.
 * Supports HTTP, HTTPS, S3, and file-based URLs.
 */
public class DataFetcherConnectionData {
    String url;
    String username;
    String password;
    Map<String, String> httpHeaders;
    int connectionTimeout;
    int readTimeout;

    public DataFetcherConnectionData() {
    }

    public DataFetcherConnectionData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public DataFetcherConnectionData(String username, String password, Map<String, String> httpHeaders) {
        this.username = username;
        this.password = password;
        this.httpHeaders = httpHeaders;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
}
