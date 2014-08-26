package org.onebusaway.nyc.admin.service.bundle.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.aspectj.util.FileUtil;
import org.onebusaway.nyc.admin.service.bundle.BundleDeployer;
import org.onebusaway.nyc.admin.service.bundle.BundleStager;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class BundleStagerImpl implements BundleStager {
  
  private static Logger _log = LoggerFactory
      .getLogger(BundleStagerImpl.class);
  
  private FileUtility _fileUtil;
  
  private File _masterBundleDirectory;
  private File _stagingDirectory;
  

  public void setMasterBundleDirectory(String masterBundleDirectory) {
    _masterBundleDirectory = new File(masterBundleDirectory);
  }

  public void setStagedBundleDirectory(String stagingDirectory) {
    _stagingDirectory = new File(stagingDirectory);
  }
  
  public File getMasterBundleDirectory() {
    return _masterBundleDirectory;
  }

  public File getStagedBundleDirectory() {
    return _stagingDirectory;
  }


  @PostConstruct
  public void setup() {
      _fileUtil = new FileUtility();
  }

  @Override
  public void stage(String env, String bundleDir, String bundleName)
      throws Exception {
    File srcDir = new File(this.getMasterBundleDirectory().toString()
        + File.separator + bundleDir + File.separator + "builds"
        + File.separator + bundleName);
    File srcFile = new File(srcDir, bundleName + ".tar.gz");
    File destDir = this.getStagedBundleDirectory();
    _log.info("deleting " + destDir);
    // cleanup from past run
    try {
      FileUtils.deleteDirectory(destDir);
    } catch (Exception any) {
      _log.info("deleteDir failed with :", any);
    }

    _log.info("making directory" + destDir);
    destDir.mkdir();

    _log.info("expanding " + srcFile + " to " + destDir);
    _fileUtil.unTargz(srcFile, destDir);
    File oldDir = new File(destDir + File.separator + bundleName);
    File newDir = new File(destDir + File.separator + env);
    _log.info("moving " + oldDir + " to " + newDir);
    FileUtils.moveDirectory(oldDir, newDir);
  }

}
