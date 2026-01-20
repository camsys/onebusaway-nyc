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

import org.onebusaway.nyc.transit_data_manager.api.dao.DataFetcherDao;
import org.onebusaway.nyc.transit_data_manager.api.dao.FileDataFetcherDao;
import org.onebusaway.nyc.transit_data_manager.api.dao.HttpDataFetcherDao;
import org.onebusaway.nyc.transit_data_manager.api.dao.S3DataFetcherDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate DataFetcher implementation based on URL scheme.
 * Supports HTTP, HTTPS, S3, and file-based URLs.
 */
@Component
public class DataFetcherFactory {

    private static final Logger log = LoggerFactory.getLogger(DataFetcherFactory.class);

    private DataFetcherDao httpFetcher;
    private DataFetcherDao s3Fetcher;
    private DataFetcherDao fileFetcher;

    /**
     * Selects the appropriate data fetcher based on the URL scheme.
     *
     * @param connectionData connection data
     * @return the appropriate DataFetcher implementation
     */
    public DataFetcherDao getDataFetcher(DataFetcherConnectionData connectionData) {
        String url = connectionData.getUrl();

        if (url == null || url.trim().isEmpty()) {
            log.warn("No URL configured, no data fetcher selected");
            return null;
        }

        String lowerUrl = url.toLowerCase().trim();

        if (lowerUrl.startsWith("s3://")) {
            log.info("Using S3 data fetcher for URL: {}", url);
            return new S3DataFetcherDao(connectionData);
        } else if (lowerUrl.startsWith("file://") || lowerUrl.startsWith("/") ||
                lowerUrl.matches("^[a-zA-Z]:.*")) { // Handle Windows paths like C:\
            log.info("Using file data fetcher for URL: {}", url);
            return new FileDataFetcherDao(connectionData);
        } else if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
            log.info("Using HTTP data fetcher for URL: {}", url);
            return new HttpDataFetcherDao(connectionData);
        } else {
            log.warn("Unknown URL scheme for: {}, defaulting to HTTP fetcher", url);
            return new HttpDataFetcherDao(connectionData);
        }
    }

    // Setters for Spring dependency injection

    public void setHttpFetcher(DataFetcherDao httpFetcher) {
        this.httpFetcher = httpFetcher;
    }

    public void setS3Fetcher(DataFetcherDao s3Fetcher) {
        this.s3Fetcher = s3Fetcher;
    }

    public void setFileFetcher(DataFetcherDao fileFetcher) {
        this.fileFetcher = fileFetcher;
    }

    // Getters for testing/verification

    public DataFetcherDao getHttpFetcher() {
        return httpFetcher;
    }

    public DataFetcherDao getS3Fetcher() {
        return s3Fetcher;
    }

    public DataFetcherDao getFileFetcher() {
        return fileFetcher;
    }
}