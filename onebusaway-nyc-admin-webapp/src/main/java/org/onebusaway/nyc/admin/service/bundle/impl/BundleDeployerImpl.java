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

public class BundleDeployerImpl implements BundleDeployer {
  
  private static final int MAX_RESULTS = -1;
  private static Logger _log = LoggerFactory
      .getLogger(BundleDeployerImpl.class);
  
  private FileUtility _fileUtil;
  
  private String _localBundlePath;
  private String _localBundleStagingPath;
  

  public void setLocalBundleStagingPath(String localBundlePath) {
    this._localBundleStagingPath = localBundlePath;
  }

  public void setLocalBundlePath(String localBundlePath) {
    this._localBundlePath = localBundlePath;
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
    List<String> fileList = new ArrayList<String>(maxResults);
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
  private int stageBundleForServing(String path) {
    _log.info("stageBundleForServing(" + path + ")");

    int bundlesDownloaded = 0;
    // list bundles at given path
    List<String> bundles = listFiles(path, MAX_RESULTS);

    if (bundles != null && !bundles.isEmpty()) {
      clearBundleStagingDirectory();
    } else {
      _log.error("no bundles found at path=" + path);
      return bundlesDownloaded;
    }

    for (String bundle : bundles) {
      _log.info("getting bundle = " + bundle);
      String bundleFilename = parseFileName(bundle, File.separator);
      // download and stage
      get(bundle, _localBundleStagingPath);
      // explode the tar file
      try {
        String bundleFileLocation = _localBundleStagingPath + File.separator
            + bundleFilename;
        _log.info("unGzip(" + bundleFileLocation + ", "
            + _localBundleStagingPath + ")");
        _fileUtil.unGzip(new File(bundleFileLocation), new File(
            _localBundleStagingPath));
        String tarFilename = parseTarName(bundleFileLocation);
        _log.info("unTar(" + tarFilename + ", " + _localBundleStagingPath + ")");
        _fileUtil.unTar(new File(tarFilename),
            new File(_localBundleStagingPath));
        _log.info("deleting bundle tar.gz=" + bundleFileLocation);
        //status.addBundleName(bundleFilename);
        new File(tarFilename).delete();
        new File(bundleFileLocation).delete();
        bundlesDownloaded++;
      } catch (Exception e) {
        _log.error("exception exploding bundle=" + bundle, e);
      }
    }

    // now cleanup -- delete bundles
    //status.setStatus(BundleDeployStatus.STATUS_COMPLETE);
    return bundlesDownloaded;
  }

  /**
   * Copy the bundle from the bundle deployer directory to the bundle loading
   * directory, and arrange as necessary.
   */
  private int stageBundleForUse(BundleDeployStatus status, String s3Path) {
    int bundleCount = 0;
    // list directories of staged location
    File stagingDirectory = new File(this._localBundleStagingPath);
    String[] directories = stagingDirectory.list();
    if (directories == null) {
      _log.error("no bundles to copy");
      return bundleCount;
    }

    clearBundleDirectory();

    for (String directory : directories) {
      // copy data directory and rename as bundle name
      String bundleName = directory;
      // the actual data of the bundle is nested inside the "data" directory
      String source = this._localBundleStagingPath + File.separator + directory
          + File.separator + "data";
      String destination = _localBundlePath + File.separator + bundleName;
      try {
        File sourceDirectory = new File(source);
        if (sourceDirectory.exists() && sourceDirectory.isDirectory()) {
          _log.info("creating bundle output directory = " + destination);
          new File(destination).mkdirs();
          // recursively copy contents of "data" director to the bundle-named
          // loading directory
          File[] bundleFiles = sourceDirectory.listFiles();
          if (bundleFiles != null) {
            for (File bundleFile : bundleFiles) {
              _log.info("copying staged bundle=" + bundleFile
                  + " to deployed dir=" + destination);
              try {
                if (bundleFile.exists() && bundleFile.isFile()) {
                  FileUtils.copyFileToDirectory(bundleFile, new File(
                      destination));
                } else if (bundleFile.exists() && bundleFile.isFile()) {
                  FileUtils.copyDirectoryToDirectory(bundleFile, new File(
                      destination));
                }
              } catch (Exception e) {
                _log.error("copyDirToDir(" + bundleFile + ", " + destination
                    + ") failed:", e);
              }
            }
          }

          bundleCount++;
        }
      } catch (Exception e) {
        _log.error("Exception moving bundle=" + bundleName, e);
      }
    }
    return bundleCount;
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
      _fileUtil.delete(new File(_localBundleStagingPath));
    } catch (IOException ioe) {
      _log.error("error wiping bundle dir:", ioe);
    }
    new File(_localBundleStagingPath).mkdir();
  }

  /**
   * delete bundle loading directory.
   */
  private void clearBundleDirectory() {
    _log.info("wiping bundle directory");
    try {
      _fileUtil.delete(new File(_localBundlePath));
    } catch (IOException ioe) {
      _log.error("error wiping bundle dir:", ioe);
    }
    new File(_localBundleStagingPath).mkdir();
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
      _fileUtil.copyDir(bundlePath, pathAndFileName);
    } catch (IOException e) {
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
      //stageBundleForServing(status, s3Path);
      //status.setStatus(BundleDeployStatus.STATUS_STAGING_COMPLETE);
      stageBundleForUse(status, path);
      status.setStatus(BundleDeployStatus.STATUS_COMPLETE);
    } catch (Exception e) {
      status.setStatus(BundleDeployStatus.STATUS_ERROR);
    }
  }
  
  public void stage(String path) throws Exception {
      stageBundleForServing(path);

  }
}
