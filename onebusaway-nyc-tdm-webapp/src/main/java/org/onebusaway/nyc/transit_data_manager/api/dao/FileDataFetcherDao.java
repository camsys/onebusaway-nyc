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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class FileDataFetcherDao implements DataFetcherDao {
    
    private static final Logger log = LoggerFactory.getLogger(FileDataFetcherDao.class);

    private String filePath;

    public FileDataFetcherDao(DataFetcherConnectionData credentials) {
        this.filePath = credentials.getUrl();
    }

    @Override
    public InputStream fetchData() throws IOException {
        log.info("Fetching from file system: {}", filePath);

        try {
            File file;
            
            if (filePath.startsWith("file://")) {
                // Handle file:// URLs
                URI uri = URI.create(filePath);
                file = new File(uri);
            } else {
                // Handle direct file paths
                file = new File(filePath);
            }
            
            if (!file.exists()) {
                throw new IOException("File not found: " + file.getAbsolutePath());
            }
            
            if (!file.canRead()) {
                throw new IOException("File not readable: " + file.getAbsolutePath());
            }
            
            log.debug("Reading file: {}", file.getAbsolutePath());
            FileInputStream inputStream = new FileInputStream(file);
            log.info("Successfully opened file: {}", file.getAbsolutePath());
            return inputStream;
            
        } catch (Exception e) {
            log.error("Failed to read file: {}", e.getMessage(), e);
            throw new IOException("Failed to read file: " + filePath, e);
        }
    }
}