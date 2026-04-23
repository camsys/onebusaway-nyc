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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import org.apache.commons.lang3.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

/**
 * S3 implementation for fetching depot assignments
 */
@Component
public class S3DepotAssignsSource implements DepotAssignsSource, ServletContextAware {

    private static final Logger _log = LoggerFactory.getLogger(S3DepotAssignsSource.class);

    private ConfigurationService _configurationService;
    private AmazonS3 _s3Client;

    private String _username;
    private String _password;
    private String _bucketName;
    private String _bucketKey;

    private AWSCredentials _awsCredentials;


    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @PostConstruct
    public void setup() {
        try {
            _log.info("setting up S3DepotAssignsSource with username=" + getUser()
                    + " and bucket=" + getBucketName() + " and bucketKey=" + getBucketKey());
            _awsCredentials = new BasicAWSCredentials(getUser(), getPassword());
            _s3Client = new AmazonS3Client(_awsCredentials);
        } catch (Exception ioe) {
            _log.error("S3DepotAssignsSource setup failed, likely due to missing or invalid s3 credentials");
            _log.error(ioe.toString());
        }

    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (servletContext != null) {
            String keyId = servletContext.getInitParameter("s3.depotAssigns.user");
            _log.info("servlet context provided s3.depotAssigns.user=" + keyId);
            if (keyId != null) {
                setKeyId(keyId);
            }
            String secretKey = servletContext.getInitParameter("s3.depotAssigns.password");
            if (secretKey != null) {
                setPassword(secretKey);
            }
            String bucketName = servletContext.getInitParameter("s3.depotAssigns.bucketName");
            if (bucketName != null) {
                _log.info("servlet context provided s3.depotAssigns.bucketName=" + bucketName);
                setBucketName(bucketName);
            } else {
                _log.info("servlet context missing bucketName, using " + getBucketName());
            }
            String bucketKey = servletContext.getInitParameter("s3.depotAssigns.bucketKey");
            if (bucketKey != null) {
                _log.info("servlet context provided s3.depotAssigns.bucketKey=" + bucketKey);
                setBucketKeyName(bucketKey);
            } else {
                _log.info("servlet context missing bucketKeyName, using " + getBucketKey());
            }
        }
    }

    private String getBucketKey() {
        return _bucketKey;
    }

    private String getBucketName() {
        return _bucketName;
    }

    private String getUser() {
        return _username;
    }

    private String getPassword() {
        return _password;
    }

    private void setBucketName(String bucketName) {
        _bucketName = bucketName;
    }

    private void setBucketKeyName(String bucketKeyName) {
        _bucketKey = bucketKeyName;
    }

    private void setPassword(String password) {
        _password = password;
    }

    private void setKeyId(String keyId) {
        _username = keyId;
    }

    @Override
    public InputStream fetchDepotAssignments() throws IOException {
        String bucket = getBucketName();
        String key = getBucketKey();

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

    private AmazonS3 getS3Client() {
        return _s3Client;
    }

    @Override
    public String getSourceType() {
        return "S3";
    }

    @Override
    public boolean isAvailable() {
        String bucket = getBucketName();
        String key = getBucketKey();

        if (StringUtils.isBlank(bucket) || StringUtils.isBlank(key)) {
            return false;
        }

        try {
            AmazonS3 s3Client = getS3Client();
            // Check if the bucket exists and is accessible
            return s3Client.doesBucketExist(bucket);
        } catch (Exception e) {
            _log.warn("Unable to verify S3 availability", e);
            return false;
        }
    }

}