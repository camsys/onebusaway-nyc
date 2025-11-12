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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import org.apache.commons.lang3.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * S3 implementation for fetching depot assignments
 */
@Component
public class S3DepotAssignsSource implements DepotAssignsSource {

    private static final Logger _log = LoggerFactory.getLogger(S3DepotAssignsSource.class);

    private ConfigurationService _configurationService;
    private AmazonS3 _s3Client;

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @Override
    public InputStream fetchDepotAssignments() throws IOException {
        String bucket = getBucket();
        String key = getKey();

        if (StringUtils.isBlank(bucket) || StringUtils.isBlank(key)) {
            throw new IOException("S3 bucket or key not configured");
        }

        try {
            AmazonS3 s3Client = getS3Client();

            long start = System.currentTimeMillis();
            _log.info("Fetching depot assignments from S3: s3://{}/{}", bucket, key);

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            S3Object s3Object = s3Client.getObject(getObjectRequest);

            _log.debug("Retrieved s3://{}/{} in {} ms", bucket, key, (System.currentTimeMillis() - start));

            return s3Object.getObjectContent();
        } catch (Exception e) {
            throw new IOException("Error fetching depot assignments from S3", e);
        }
    }

    @Override
    public String getSourceType() {
        return "S3";
    }

    @Override
    public boolean isAvailable() {
        String bucket = getBucket();
        String key = getKey();

        if (StringUtils.isBlank(bucket) || StringUtils.isBlank(key)) {
            return false;
        }

        try {
            AmazonS3 s3Client = getS3Client();
            // Check if the bucket exists and is accessible
            return s3Client.doesBucketExistV2(bucket);
        } catch (Exception e) {
            _log.warn("Unable to verify S3 availability", e);
            return false;
        }
    }

    /**
     * Get or create the S3 client
     */
    private synchronized AmazonS3 getS3Client() {
        if (_s3Client == null) {
            String region = getRegion();
            String accessKeyId = getAccessKeyId();
            String secretAccessKey = getSecretAccessKey();

            AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

            if (StringUtils.isNotBlank(region)) {
                builder.withRegion(region);
            }

            // Use explicit credentials if provided, otherwise use default credential provider chain
            if (StringUtils.isNotBlank(accessKeyId) && StringUtils.isNotBlank(secretAccessKey)) {
                BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
                builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
            } else {
                // This will use IAM role credentials if running on EC2, or environment variables, etc.
                builder.withCredentials(DefaultAWSCredentialsProviderChain.getInstance());
            }

            _s3Client = builder.build();
        }
        return _s3Client;
    }

    private String getBucket() {
        String bucket = System.getProperty("AWS_S3_DEPOT_ASSIGNS_BUCKET_NAME");

        if (StringUtils.isNotBlank(bucket)) {
            _log.debug("Using S3 bucket: {}", bucket);
        }

        return bucket;
    }

    private String getKey() {
        String keyPattern = System.getProperty("AWS_S3_DEPOT_ASSIGNS_KEY");

        if (StringUtils.isBlank(keyPattern)) {
            keyPattern = _configurationService.getConfigurationValueAsString("tdm.depotAssigns.s3.key", null);
        }

        if (StringUtils.isBlank(keyPattern)) {
            return null;
        }

        return keyPattern;
    }

    private String getRegion() {
        String region = System.getProperty("AWS_REGION");

        if (StringUtils.isBlank(region)) {
            region = System.getProperty("AWS_DEFAULT_REGION");
        }

        if (StringUtils.isBlank(region)) {
            region = _configurationService.getConfigurationValueAsString("tdm.depotAssigns.s3.region", "us-east-1");
        }

        return region;
    }

    private String getAccessKeyId() {
        String accessKeyId = System.getProperty("AWS_ACCESS_KEY_ID");

        if (StringUtils.isBlank(accessKeyId)) {
            accessKeyId = System.getProperty("s3.user");
        }

        if (StringUtils.isBlank(accessKeyId)) {
            accessKeyId = System.getProperty("s3.username");
        }

        if (StringUtils.isBlank(accessKeyId)) {
            accessKeyId = _configurationService.getConfigurationValueAsString("tdm.depotAssigns.s3.accessKeyId", null);
        }

        return accessKeyId;
    }


    private String getSecretAccessKey() {
        String secretAccessKey = System.getProperty("AWS_SECRET_ACCESS_KEY");

        if (StringUtils.isBlank(secretAccessKey)) {
            secretAccessKey = System.getProperty("s3.password");
        }

        if (StringUtils.isBlank(secretAccessKey)) {
            secretAccessKey = _configurationService.getConfigurationValueAsString("tdm.depotAssigns.s3.secretAccessKey", null);
        }

        return secretAccessKey;
    }
}