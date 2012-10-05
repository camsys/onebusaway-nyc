package org.onebusaway.nyc.transit_data_manager.bundle.impl;

import org.onebusaway.nyc.transit_data_manager.bundle.BundleDeployer;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleDeployStatus;
import org.onebusaway.nyc.transit_data_manager.util.BaseDeployer;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of deploying bundles to the TDM (and hence the rest of the
 * environment).  This implementation relies heavily on usage conventions of AWS S3;
 * namely that s3://{bucketName}/activebundles/{environment} contain bundle.tar.gz
 *
 */
public class BundleDeployerImpl extends BaseDeployer implements BundleDeployer {

  private String _localBundlePath;
  private String _localBundleStagingPath;


  public void setLocalBundleStagingPath(String localBundlePath) {
    this._localBundleStagingPath = localBundlePath;
  }

  public void setLocalBundlePath(String localBundlePath) {
    this._localBundlePath = localBundlePath;
  }

  @Override
  public List<String> listBundlesForServing(String s3Path) {
    List<String> bundleFiles = new ArrayList<String>();
    List<String> bundlePaths = listFiles(s3Path, MAX_RESULTS);
    for (String bundle : bundlePaths) {
      bundleFiles.add(parseFileName(bundle));
    }
    return bundleFiles;
  }
  
  /**
   * Copy the bundle from S3 to the TDM's bundle serving location, and arrange as
   * necessary. 
   */
  private int stageBundleForServing(BundleDeployStatus status, String s3Path) {
    _log.info("stageBundleForServing(" + s3Path + ")");
    
    int bundlesDownloaded = 0;
    // list bundles at given path
    List<String> bundles = listFiles(s3Path, MAX_RESULTS);

    if (bundles != null && !bundles.isEmpty()) {
      clearBundleStagingDirectory();
    } else {
      _log.error("no bundles found at path=" + s3Path);
      return bundlesDownloaded;
    }

    for (String bundle : bundles) {
      _log.info("getting bundle=" + bundle);
      String bundleFilename = parseFileName(bundle);
      // download and stage
      get(bundle, _localBundleStagingPath);
      // explode the tar file
      try {
        String bundleFileLocation = _localBundleStagingPath + File.separator + bundleFilename;
        _log.info("unGzip(" + bundleFileLocation + ", " + _localBundleStagingPath + ")" );
        _fileUtil.unGzip(new File(bundleFileLocation), new File(_localBundleStagingPath));
        String tarFilename = parseTarName(bundleFileLocation);
        _log.info("unTar(" + tarFilename + ", " + _localBundleStagingPath + ")" );
        _fileUtil.unTar(new File(tarFilename), new File(_localBundleStagingPath));
        _log.info("deleting bundle tar.gz=" + bundleFileLocation);
        status.getBundleNames().add(bundleFilename);
        new File(tarFilename).delete();
        new File(bundleFileLocation).delete();
        bundlesDownloaded++;
      } catch (Exception e) {
        _log.error("exception exploding bundle=" + bundle, e);
      }
    }
    
    // now cleanup -- delete bundles
    status.setStatus(BundleDeployStatus.STATUS_COMPLETE);
    return bundlesDownloaded;
  }

  /**
   * Copy the bundle from the bundle deployer directory to the bundle loading directory,
   * and arrange as necessary.
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
      String source = this._localBundleStagingPath + File.separator + directory + File.separator + "data";
      String destination = _localBundlePath + File.separator + bundleName;
      try {
        File sourceDirectory = new File(source);
        if (sourceDirectory.exists() && sourceDirectory.isDirectory()) {
          _log.info("creating bundle output directory = " + destination);
          new File(destination).mkdirs();
          // recursively copy contents of "data" director to the bundle-named loading directory
          File[] bundleFiles = sourceDirectory.listFiles();
          if (bundleFiles != null) {
            for (File bundleFile : bundleFiles) {
              _log.info("copying staged bundle=" + bundleFile + " to deployed dir=" + destination);
              try {
                if (bundleFile.exists() && bundleFile.isFile()) {
                  FileUtils.copyFileToDirectory(bundleFile, new File(destination));
                } else if (bundleFile.exists() && bundleFile.isFile()) {
                  FileUtils.copyDirectoryToDirectory(bundleFile, new File(destination));
                }
              } catch (Exception e) {
                _log.error("copyDirToDir(" + bundleFile + ", " + destination + ") failed:", e);
              }
            }
          }
            
          bundleCount++;
        }
      } catch (Exception e) {
        _log.error("Exception moving bundle=" +bundleName, e);
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


  @Override
  /**
   * Download bundles from s3://{bucketname}/activebundles/{environment} and stage
   * for downloading and loading on the TDM.
   */
  public void deploy(BundleDeployStatus status, String s3Path) {
    try {
      status.setStatus(BundleDeployStatus.STATUS_STARTED);
      stageBundleForServing(status, s3Path);
      status.setStatus(BundleDeployStatus.STATUS_STAGING_COMPLETE);
      stageBundleForUse(status, s3Path);
      status.setStatus(BundleDeployStatus.STATUS_COMPLETE);
    } catch (Exception e) {
      status.setStatus(BundleDeployStatus.STATUS_ERROR);
    }
  }


}
