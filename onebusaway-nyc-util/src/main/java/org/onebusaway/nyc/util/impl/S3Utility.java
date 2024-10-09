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

package org.onebusaway.nyc.util.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.S3Object;

import java.io.InputStream;

public class S3Utility {
    private String username;
    private String password;
    private AmazonS3Client _s3;

    public S3Utility(String username, String password){
        this.username = username;
        this.password = password;
        setupClient(username, password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setupClient(String username, String password){
        AWSCredentials credentials = new BasicAWSCredentials(username, password);
        _s3 = new AmazonS3Client(credentials);
    }

    public static boolean isS3Path(String url) {
        return url != null && url.startsWith("s3://");
    }

    public InputStream getObject(String s3Path){
        AmazonS3URI s3URI = new AmazonS3URI(s3Path);
        return getObject(s3URI.getBucket(), s3URI.getKey());
    }

    private InputStream getObject(String bucketName, String keyName) {
        S3Object s3Object = _s3.getObject(bucketName, keyName);
        return s3Object.getObjectContent();
    }
}
