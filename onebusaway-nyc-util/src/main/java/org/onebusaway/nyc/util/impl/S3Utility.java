/**
 * Copyright (C) 2024 Metropolitan Transportation Authority
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


// S3Utility class, mostly coppied over from FileServiceImpl
public class S3Utility {

    private static Logger _log = LoggerFactory.getLogger(S3Utility.class);
    private AWSCredentials _credentials;
    private AmazonS3Client _s3;

    private String _bucketName;
    private String _username;
    private String _password;

    @Autowired
    public void setS3User(String user) {
        _username = user;
    }

    @Autowired
    public void setS3Password(String password) {
        _password = password;
    }

    public void setBucketName(String bucketName) {
        this._bucketName = bucketName;
    }

    public S3Utility(String username, String password){
        _password = password;
        _username = username;
        setup();
    }
    public S3Utility(String username, String password, String bucketName){
        _password = password;
        _username = username;
        _bucketName = bucketName;
        setup();
    }


    @PostConstruct
    public void setup() {
        try {
            _log.info("setting up s3user=" + _username);
            _credentials = new BasicAWSCredentials(_username, _password);
            _s3 = new AmazonS3Client(_credentials);
            _log.info("setup complete");
        } catch (Throwable t) {
            _log.error("FileServiceImpl setup failed, likely due to missing or invalid credentials");
            _log.error(t.toString());
        }

    }


    /**
     * delete an object from s3
     */
    public void deleteObject(String filename) {
        try {
            _s3.deleteObject(_bucketName, filename);
        } catch (AmazonClientException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    /**
     * Copies an s3 object from one location to another
     */
    public void copyS3Object(String fromObjectKey, String toObjectKey) {
        try {
            _s3.copyObject(_bucketName, fromObjectKey, _bucketName, toObjectKey);
        } catch( Exception e){
            throw new RuntimeException(e);
        }
    }



    /**
     * Return files' names at a specified location.
     */
    public List<String> listFiles(String directoryName, int maxResults) {
        List<String> rows = new ArrayList<String>();
        ListObjectsRequest request = new ListObjectsRequest(_bucketName, directoryName, null,
                "/", maxResults);

        ObjectListing listing = null;
        do {
            if (listing == null) {
                listing = _s3.listObjects(request);
            }
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                rows.add(summary.getKey());
            }

        } while (listing.isTruncated());
        return rows;
    }


    /**
     * Return fileName of objects in an S3 file.
     */
    public List<String> listObjects(String directoryName, int maxResults) {
        List<String> rows = new ArrayList<String>();
        HashMap<String, String> map = new HashMap<String, String>();
        ListObjectsRequest request = new ListObjectsRequest(_bucketName, directoryName, null,
                "/", maxResults);

        ObjectListing listing = null;
        if (listing == null) {
            listing = _s3.listObjects(request);
            if (listing.getObjectSummaries() != null) {
                // short circuit if common prefixes works
                List<S3ObjectSummary> s3objectSummaries = listing.getObjectSummaries();
                for (S3ObjectSummary s3ObjectSummary : s3objectSummaries) {
                    String[] keyParts = s3ObjectSummary.getKey().split("/");
                    rows.add(keyParts[keyParts.length - 1]);
                }
            }
        }
        return rows;
    }

    private Date getLastModifiedTimeForKey(String key) {
        ListObjectsRequest request = new ListObjectsRequest(_bucketName, key, null,
                "/", 1);
        ObjectListing listing = _s3.listObjects(request);
        if (!listing.getObjectSummaries().isEmpty())
            return listing.getObjectSummaries().get(0).getLastModified();
        return null;
    }


    /**
     * Retrieve the specified key from S3 and store in the given directory.
     */
    public String get(String key, String tmpDir) {
        _log.debug("get(" + key + ", " + tmpDir + ")");
        FileUtility fs = new FileUtility();
        String filename = parseFileName(key);
        _log.debug("filename=" + filename);
        GetObjectRequest request = new GetObjectRequest(this._bucketName, key);
        S3Object file = _s3.getObject(request);
        String pathAndFileName = tmpDir + File.separator + filename;
        fs.copy(file.getObjectContent(), pathAndFileName);
        return pathAndFileName;
    }


    public String parseFileName(String urlString) {
        if (urlString == null) return null;
        int i = urlString.lastIndexOf("/");
        if (i+1 < urlString.length()) {
            return urlString.substring(i+1, urlString.length());
        }
        if (i >= 0) {
            return urlString.substring(i, urlString.length());
        }
        return urlString;
    }

    public InputStream get(String key) {
        GetObjectRequest request = new GetObjectRequest(this._bucketName, key);
        S3Object file = _s3.getObject(request);
        return file.getObjectContent();
    }

    /**
     * push the contents of the directory to S3 at the given key location.
     */
    public String put(String key, String file) {
        if (new File(file).isDirectory()) {
            File dir = new File(file);
            for (File contents : dir.listFiles()) {
                try {
                    put(key, contents.getName(), contents.getCanonicalPath());
                } catch (IOException ioe) {
                    _log.error(ioe.toString(), ioe);
                }
            }
            return null;
        }
        PutObjectRequest request = new PutObjectRequest(this._bucketName, key,
                new File(file));
        PutObjectResult result = _s3.putObject(request);
        return result.getVersionId();
    }

    public String put(String prefix, String key, String file) {
        if (new File(file).isDirectory()) {
            File dir = new File(file);
            for (File contents : dir.listFiles()) {
                try {
                    put(prefix + "/" + key, contents.getName(),
                            contents.getCanonicalPath());
                } catch (IOException ioe) {
                    _log.error(ioe.toString(), ioe);
                }
            }
            return null;
        }
        String filename = prefix + "/" + key;
        _log.info("uploading " + file + " to " + filename);
        PutObjectRequest request = new PutObjectRequest(this._bucketName, filename,
                new File(file));
        PutObjectResult result = _s3.putObject(request);
        return result.getVersionId();

    }

    /**
     * list the files in the given directory.
     */
    public List<String> list(String directory, int maxResults) {
        ListObjectsRequest request = new ListObjectsRequest(_bucketName, directory,
                null, null, maxResults);
        ObjectListing listing = _s3.listObjects(request);
        List<String> rows = new ArrayList<String>();
        for (S3ObjectSummary summary : listing.getObjectSummaries()) {
            // if its a directory at the root level
            if (!summary.getKey().endsWith("/")) {
                rows.add(summary.getKey());
            }
        }
        return rows;
    }


    /**
     * Return tabular data (filename, flag, modified date) about objects on S3.
     */
    public List<Map<String,String>> listObjectsTabular (String directory, int maxResults){

        ListObjectsRequest request = new ListObjectsRequest(_bucketName, directory,
                null, null, maxResults);
        ObjectListing listing = _s3.listObjects(request);
        List<Map<String,String>> rows = new ArrayList();
        for (S3ObjectSummary summary : listing.getObjectSummaries()) {
            HashMap<String,String> objectDetails = new HashMap<String,String>();
            objectDetails.put("bucketName",summary.getBucketName());
            objectDetails.put("key",summary.getKey());
            objectDetails.put("eTag",summary.getETag());
            objectDetails.put("lastModified",summary.getLastModified().toString());
            rows.add(objectDetails);
        }
        return rows;
    }


    public static boolean isS3Path(String path){
        if(path.substring(0,5).equals("s3://")){
            return true;
        }
        return false;
    }

    public static String getBucketFromS3Path(String path){
        if(path.substring(0,5).equals("s3://")){
            return path.substring(5,5+path.substring(5).indexOf("/"));
        }
        return null;
    }

    public static String getKeyFromS3Path(String path){
        if(path.substring(0,5).equals("s3://")){
            return path.substring(5).substring(path.substring(5).indexOf("/")+1);
        }
        return null;
    }
}
