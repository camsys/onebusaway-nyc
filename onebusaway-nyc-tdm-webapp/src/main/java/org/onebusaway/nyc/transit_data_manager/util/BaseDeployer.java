package org.onebusaway.nyc.transit_data_manager.util;

import org.onebusaway.util.services.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.FileUtility;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

/**
 * Base class for impelmentation classes that deploy from S3.
 *
 */
public class BaseDeployer implements ServletContextAware {

  protected static Logger _log = LoggerFactory.getLogger(BaseDeployer.class);
  protected static final int MAX_RESULTS = -1;

  @Autowired
  protected ConfigurationService configurationService;

  protected AWSCredentials _credentials;
  protected AmazonS3Client _s3;
  protected FileUtility _fileUtil;
  protected String _bucketName;
  protected String _username;
  protected String _password;

  public void setUser(String user) {
    _username = user;
  }

  public void setPassword(String password) {
    _password = password;
  }

  public void setBucketName(String bucketName) {
    this._bucketName = bucketName;
  }

  public String getBucketName() {
    return this._bucketName;
  }
  
  @PostConstruct
  public void setup() {
    try {
      _log.info("setting up BundleDeployerImpl with username=" + _username 
          + " and bucket=" + _bucketName);
      _credentials = new BasicAWSCredentials(_username, _password);
      _s3 = new AmazonS3Client(_credentials);
      _fileUtil = new FileUtility();
    } catch (Exception ioe) {
      _log.error("BaseDeplyer setup failed, likely due to missing or invalid s3 credentials");
      _log.error(ioe.toString());
    }

  }

  /**
   * Retrieve the specified key from S3 and store in the given directory.
   */
  public String get(String s3Key, String destinationDirectory) {
    _log.info("get(" + s3Key + ", " + destinationDirectory + ")");
    String filename = parseFileName(s3Key);
    _log.info("filename=" + filename);
    GetObjectRequest request = new GetObjectRequest(this._bucketName, s3Key);
    S3Object file = _s3.getObject(request);
    String pathAndFileName = destinationDirectory + File.separator + filename;
    _fileUtil.copy(file.getObjectContent(), pathAndFileName);
    return pathAndFileName;
  }

  /**
   * list the files in the given directory.
   */
  public List<String> listFiles(String directory, int maxResults) {
    _log.info("list(s3://" + _bucketName + "/" + directory + ")");
    ListObjectsRequest request = new ListObjectsRequest(_bucketName, directory,
        File.separator, null, maxResults);
    ObjectListing listing = _s3.listObjects(request);
    List<String> rows = new ArrayList<String>();
    for (S3ObjectSummary summary : listing.getObjectSummaries()) {
      String key = summary.getKey();
      _log.info("found=" + key);
      // hide directories
      if (!key.endsWith(File.separator)) {
        rows.add(summary.getKey());
      }
    }
    return rows;
  }

  protected String parseFileName(String urlString) {
    int i = urlString.lastIndexOf("/");
    if (i+1 < urlString.length()) {
      return urlString.substring(i+1, urlString.length());
    }
    if (i >= 0) {
      return urlString.substring(i, urlString.length());
    }
    return urlString;
  }


  @Override
  public void setServletContext(ServletContext servletContext) {
    if (servletContext != null) {
      String user = servletContext.getInitParameter("s3.user");
      _log.info("servlet context provided s3.user=" + user);
      if (user != null) {
        setUser(user);
      }
      String password = servletContext.getInitParameter("s3.password");
      if (password != null) {
        setPassword(password);
      }
      String bucketName = servletContext.getInitParameter("s3.bundle.bucketName");
      if (bucketName != null) {
        _log.info("servlet context provided bucketName=" + bucketName);
        setBucketName(bucketName);
      } else {
        _log.info("servlet context missing bucketName, using "
            + getBucketName());
      }
    }
  }
  
}
