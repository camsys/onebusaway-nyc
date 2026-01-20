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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherConnectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class S3DataFetcherDao implements DataFetcherDao {

    private static final Logger log = LoggerFactory.getLogger(S3DataFetcherDao.class);

    private String fullS3Path;
    private AmazonS3 s3Client;

    public S3DataFetcherDao(DataFetcherConnectionData credentials) {

        String url = credentials.getUrl();
        String accessKeyId = credentials.getUsername();
        String secretAccessKey = credentials.getPassword();

        if (url != null && accessKeyId != null && secretAccessKey != null) {
            this.fullS3Path = url;
            log.info("Initializing S3 client with credentials from servlet context");
            AWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
            s3Client = new AmazonS3Client(awsCredentials);
        } else {
            log.info("No S3 credentials in servlet context, using default credentials provider chain");
        }
    }


    @Override
    public InputStream fetchData() throws IOException {

        if (fullS3Path == null) {
            log.warn("S3 Path not set");
            return null;
        }
        log.info("Fetching from S3: {}", fullS3Path);

        if (s3Client == null) {
            log.warn("S3 client not initialized");
            return null;
        }

        try {
            // Parse S3 URL: s3://bucket-name/key/path
            String s3Path = fullS3Path.substring("s3://".length());
            int firstSlash = s3Path.indexOf('/');

            if (firstSlash == -1) {
                throw new IOException("Invalid S3 URL format: " + fullS3Path);
            }

            String bucket = s3Path.substring(0, firstSlash);
            String key = s3Path.substring(firstSlash + 1);

            log.debug("S3 bucket: {}, key: {}", bucket, key);

            // Create S3 client with custom timeouts for this request

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            S3Object s3Object = s3Client.getObject(getObjectRequest);

            if (s3Object == null) {
                throw new IOException("S3 object not found: " + fullS3Path);
            }

            InputStream inputStream = s3Object.getObjectContent();
            log.info("Successfully fetched from S3");
            return inputStream;

        } catch (AmazonServiceException e) {
            log.error("AWS Service error fetching from S3: {} (Status: {})",
                    e.getMessage(), e.getStatusCode(), e);
            throw new IOException("Failed to fetch from S3: " + fullS3Path + " - " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to fetch from S3: {}", e.getMessage(), e);
            throw new IOException("Failed to fetch from S3: " + fullS3Path, e);
        }
    }

}