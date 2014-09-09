package org.onebusaway.nyc.admin.service.bundle.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.util.FileUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.admin.service.bundle.BundleDeployer;
import org.onebusaway.nyc.admin.service.bundle.BundleStager;
import org.onebusaway.nyc.transit_data_manager.bundle.AbstractBundleSource;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleMetadata;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class DirectoryBundleStagerImpl implements BundleStager {
  
  private static Logger _log = LoggerFactory
      .getLogger(DirectoryBundleStagerImpl.class);
  
  private String _builtBundleDirectory;
  private String _stagingDirectory;
  
  @Autowired
  private ConfigurationServiceClient _configClient;
  
  @Autowired
  private RemoteConnectionService _remoteConnectionService;
  

  public void setBuiltBundleDirectory(String _builtBundleDirectory) {
    this._builtBundleDirectory = _builtBundleDirectory;
  }

  public void setStagedBundleDirectory(String _stagingDirectory) {
    this._stagingDirectory = _stagingDirectory;
  }
  
  public String getBuiltBundleDirectory() {
    return _builtBundleDirectory;
  }

  public String getStagedBundleDirectory() {
    return _stagingDirectory;
  }
  
  @Override
  /**
  * Copy the bundle from the bundle build directory to the staging bundle directory
  */
  public void stage(String env, String bundleDir, String bundleName)
      throws Exception {
    File srcDir = new File(this.getBuiltBundleDirectory()
        + File.separator + bundleDir + File.separator + "builds"
        + File.separator + bundleName);
    File srcFile = new File(srcDir, bundleName + ".tar.gz");
    File destDir = new File(this.getStagedBundleDirectory()
        + File.separator + env);
   
    // cleanup from past run
    /* _log.info("deleting " + destDir);
    try {
      FileUtils.deleteDirectory(destDir);
    } catch (Exception any) {
      _log.info("deleteDir failed with :", any);
    }*/

    _log.info("moving " + srcFile + " to " + destDir);
    FileUtils.copyFileToDirectory(srcFile, destDir, true);
  }
  
  public void notifyOTP(String bundleName) throws Exception {
    String otpNotificationUrl = _configClient.getItem("admin", "otpNotificationUrl");
    if (otpNotificationUrl == null) return;
    otpNotificationUrl = otpNotificationUrl.replaceAll(":uuid", (bundleName==null?"":bundleName));
    _remoteConnectionService.getContent(otpNotificationUrl);
  }

}
