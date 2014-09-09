package org.onebusaway.nyc.admin.service.bundle.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.onebusaway.nyc.admin.service.bundle.BundleDeployer;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

public class DirectoryBundleDeployerImpl implements BundleDeployer {
  
  private static final int MAX_RESULTS = -1;
  private static Logger _log = LoggerFactory
      .getLogger(DirectoryBundleDeployerImpl.class);
  
  private FileUtility _fileUtil;
  
  private String _stagingDirectory;
  private String _deployBundleDirectory;
  
  public void setStagedBundleDirectory(String stagingDirectory) {
    _stagingDirectory = stagingDirectory;
  }
  
  public void setDeployBundleDirectory(String localBundlePath) {
    _deployBundleDirectory = localBundlePath;
  }
  
  @PostConstruct
  public void setup() {
      _fileUtil = new FileUtility();
  }

  @Override
  public List<String> listBundlesForServing(String path) {
    List<String> bundleFiles = new ArrayList<String>();
    List<String> bundlePaths = listFiles(path, MAX_RESULTS);
    for (String bundle : bundlePaths) {
      bundleFiles.add(parseFileName(bundle, File.separator));
    }
    return bundleFiles;
  }
  
  public List<String> listFiles(String directory, int maxResults){
    File bundleDir = new File(directory);
    int fileCount = 1;
    List<String> fileList;
      if(maxResults > 0)
        fileList = new ArrayList<String>(maxResults);
      else
        fileList = new ArrayList<String>();
    if (bundleDir.isDirectory()) {
      for (File bundleFile : bundleDir.listFiles()) {
        if (maxResults >= 0 && fileCount > maxResults)
          break;
        else {
          try {
            fileList.add(bundleFile.getCanonicalPath());
          } catch (IOException e) {
            _log.error("Unable to access " + bundleFile.getName());
            _log.error(e.toString(), e);
            continue;
          }
          fileCount++;
        }
      }
    }
    return fileList;
  }

  /**
   * Copy the bundle from Staging to the Admin Server's bundle serving location, and arrange
   * as necessary.
   */
  private int deployBundleForServing(BundleDeployStatus status, String path) {
    _log.info("deployBundleForServing(" + path + ")");

    int bundlesDownloaded = 0;
    // list bundles at given path
    List<String> bundles = listFiles(path, MAX_RESULTS);

    if (bundles != null && !bundles.isEmpty()) {
      clearBundleDeployDirectory();
    } else {
      _log.error("no bundles found at path=" + path);
      return bundlesDownloaded;
    }

    for (String bundle : bundles) {
      _log.info("getting bundle = " + bundle);
      String bundleFilename = parseFileName(bundle, File.separator);
      // download and stage
      get(bundle, _deployBundleDirectory);
      // explode the tar file
      try {
        String bundleFileLocation = _deployBundleDirectory + File.separator
            + bundleFilename;
        _log.info("unGzip(" + bundleFileLocation + ", "
            + _deployBundleDirectory + ")");
        _fileUtil.unGzip(new File(bundleFileLocation), new File(
            _deployBundleDirectory));
        String tarFilename = parseTarName(bundleFileLocation);
        _log.info("unTar(" + tarFilename + ", " + _deployBundleDirectory + ")");
        _fileUtil.unTar(new File(tarFilename),
            new File(_deployBundleDirectory));
        _log.info("deleting bundle tar.gz=" + bundleFileLocation);
        status.addBundleName(bundleFilename);
        new File(tarFilename).delete();
        new File(bundleFileLocation).delete();
        bundlesDownloaded++;
      } catch (Exception e) {
        _log.error("exception exploding bundle=" + bundle, e);
      }
    }

    // now cleanup -- delete bundles
    clearBundleStagingDirectory();
    status.setStatus(BundleDeployStatus.STATUS_COMPLETE);
    return bundlesDownloaded;
  }

  private String parseTarName(String urlString) {
    int i = urlString.lastIndexOf(".");
    if (i < urlString.length()) {
      return urlString.substring(0, i);
    }
    return urlString;
  }

  /**
   * delete bundle staging directory.
   */
  private void clearBundleStagingDirectory() {
    _log.info("wiping bundle staging directory");
    try {
      _fileUtil.delete(new File(_stagingDirectory));
    } catch (IOException ioe) {
      _log.error("error wiping bundle dir:", ioe);
    }
    new File(_stagingDirectory).mkdir();
  }

  /**
   * delete bundle deploy directory.
   */
  private void clearBundleDeployDirectory(){
    _log.info("wiping bundle deploy directory");
    try {
      _fileUtil.delete(new File(_deployBundleDirectory));
    } catch (IOException ioe) {
      _log.error("error wiping bundle dir:", ioe);
    }
    new File(_deployBundleDirectory).mkdir();
  }
  
  private String parseFileName(String fullFileName, String separator) {
    
    int i = fullFileName.lastIndexOf(separator);
    
    if (i+1 < fullFileName.length()) {
      return fullFileName.substring(i+1, fullFileName.length());
    }
    if (i >= 0) {
      return fullFileName.substring(i, fullFileName.length());
    }
    return fullFileName;
  }

  /**
   * Retrieve the specified bundle file and store in the given directory.
   */
  public String get(String bundlePath, String destinationDirectory) {
    _log.info("get(" + bundlePath + ", " + destinationDirectory + ")");
    String filename = parseFileName(bundlePath, File.separator);
    _log.info("filename=" + filename);
    String pathAndFileName = destinationDirectory + File.separator + filename;
    try {
      //_fileUtil.copyDir(bundlePath, pathAndFileName);
      _fileUtil.moveFile(bundlePath, pathAndFileName);
    } catch (IOException e) {
      _log.error("exception copying bundle from " + bundlePath + "to " + pathAndFileName, e);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      _log.error("exception copying bundle from " + bundlePath + "to " + pathAndFileName, e);
    }
    return pathAndFileName;
  }

  
  @Override
  /**
   * Download bundles from s3://{bucketname}/activebundles/{environment} and stage
   * for downloading and loading on the TDM.
   */
  public void deploy(BundleDeployStatus status, String path) {
    try {
      status.setStatus(BundleDeployStatus.STATUS_STARTED);
      deployBundleForServing(status, path);
      status.setStatus(BundleDeployStatus.STATUS_COMPLETE);
    } catch (Exception e) {
      status.setStatus(BundleDeployStatus.STATUS_ERROR);
    }
  }
}
