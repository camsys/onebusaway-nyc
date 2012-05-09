package org.onebusaway.nyc.admin.service.impl;

import org.onebusaway.nyc.admin.service.FileService;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

/**
 * Implements File operations over Amazon S3.
 * 
 */
public class FileServiceImpl implements FileService {

  private static Logger _log = LoggerFactory.getLogger(FileServiceImpl.class);
  private AWSCredentials credentials;
  private AmazonS3Client s3;
  @Autowired
  private String bucketName;

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  @PostConstruct
  public void setup() {
    try {
      credentials = new PropertiesCredentials(
          this.getClass().getResourceAsStream("AwsCredentials.properties"));
      s3 = new AmazonS3Client(credentials);
    } catch (IOException ioe) {
      _log.error(ioe.toString());
      throw new RuntimeException(ioe);
    }

  }

  @Override
  public boolean bundleDirectoryExists(String filename) {
    ListObjectsRequest request = new ListObjectsRequest(bucketName, filename,
        null, null, 1);
    ObjectListing listing = s3.listObjects(request);
    return listing.getObjectSummaries().size() > 0;
  }

  @Override
  public boolean createBundleDirectory(String filename) {
    try {
      /*
       *  a file needs to be written for a directory to exist
       *  create README file, which could optionally contain meta-data such as
       *  creator, production mode, etc.
       */
      File tmpFile = File.createTempFile("README", "txt");
      String contents = "Root of Bundle Build";
      FileWriter fw = new FileWriter(tmpFile);
      fw.append(contents);
      fw.close();
      PutObjectRequest request = new PutObjectRequest(bucketName, filename
          + "/README.txt", tmpFile);
      PutObjectResult result = s3.putObject(request);
      return result != null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  /**
   * Return tabular data (filename, flag, modified date) about bundle directories.
   */
  public List<String[]> listBundleDirectories(int maxResults) {
    ListObjectsRequest request = new ListObjectsRequest(bucketName, "", null,
        null, maxResults);
    ObjectListing listing = s3.listObjects(request);
    List<String[]> rows = new ArrayList<String[]>();
    for (S3ObjectSummary summary : listing.getObjectSummaries()) {
      // if its a directory at the root level
      if (summary.getKey().endsWith("/")) {
        // make sure its not a sub-directory
        int matches = summary.getKey().split("\\/").length - 1;
        if (matches == 0) {
          String[] columns = {
              summary.getKey(), " ", "" + summary.getLastModified().getTime()};
          rows.add(columns);
        }
      }
    }
    return rows;
  }

}
