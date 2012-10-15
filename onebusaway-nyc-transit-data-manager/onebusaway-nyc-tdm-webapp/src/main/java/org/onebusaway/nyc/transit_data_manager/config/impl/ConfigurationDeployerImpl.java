package org.onebusaway.nyc.transit_data_manager.config.impl;

import org.onebusaway.nyc.transit_data_manager.bundle.model.ConfigDeployStatus;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDeployer;
import org.onebusaway.nyc.transit_data_manager.util.BaseDeployer;
import org.onebusaway.nyc.util.impl.FileUtility;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteConnectFailureException;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

/**
 * Implementation of deploying configuration files from S3 to TDM.  Currently supports
 * destination sign code spreadsheet and depot id map. 
 *
 */
public class ConfigurationDeployerImpl extends BaseDeployer implements ConfigurationDeployer {

  private static Logger _log = LoggerFactory.getLogger(ConfigurationDeployerImpl.class);
  private static final int MAX_RESULTS = -1;
  private static final String DEFAULT_DSC_DIRECTORY = "destination_sign_codes";
  private static final String DEFAULT_DEPOT_ID_DIRECTORY = "depot_id_map";

  private String localDepotIdMapDir;
  private String localDscFileDir;
  
  @Override
  public void setLocalDepotIdMapDir(String dir) {
    this.localDepotIdMapDir = dir;
  }
  
  @Override
  public void setLocalDscFileDir(String dir) {
    this.localDscFileDir = dir;
  }
  
  @Override
  public void deploy(ConfigDeployStatus status, String s3Path) {
    try {
      status.setStatus(ConfigDeployStatus.STATUS_STARTED);
      String depotIdMapS3Path = s3Path + getDepotIdMapDir() + "/";
      List<String> depotIdMaps = listFiles(depotIdMapS3Path, MAX_RESULTS);
      // download depot_id_map
      for (String depotIdMap : depotIdMaps) {
        String depotIdMapName = parseFileName(depotIdMap);
        _log.info("deploying " + depotIdMapName + " to " + getDepotIdMapDir());
        get(depotIdMap, localDepotIdMapDir);
        status.addDepotIdMapNames(depotIdMapName);
      }
  
  
      String dscPath = s3Path + getDscDir();
      List<String> dscs = listFiles(dscPath, MAX_RESULTS);
      // download destination sign codes
      for (String dsc : dscs) {
        String dscName = parseFileName(dsc);
        _log.info("deploying " + dscName + " to " + getDscDir());
        get(dsc, localDscFileDir);
        status.addDscFilename(dscName);
      }
      status.setStatus(ConfigDeployStatus.STATUS_COMPLETE);
    } catch (Exception e) {
      _log.error("exception deploying config:", e);
      status.setStatus(ConfigDeployStatus.STATUS_ERROR);
    }
  }

  private String getDscDir() {
    if (configurationService != null) {
      try {
        return configurationService.getConfigurationValueAsString("admin.dsc_directory", DEFAULT_DSC_DIRECTORY);
      } catch (RemoteConnectFailureException e){
        _log.error("default dsc dir lookup failed:", e);
        return DEFAULT_DSC_DIRECTORY;
      }
    }
    return DEFAULT_DSC_DIRECTORY;
  }

  private String getDepotIdMapDir() {
    if (configurationService != null) {
      try {
        return configurationService.getConfigurationValueAsString("admin.depot_id_directory", DEFAULT_DEPOT_ID_DIRECTORY);
      } catch (RemoteConnectFailureException e){
        _log.error("default depot_id dir lookup failed:", e);
        return DEFAULT_DEPOT_ID_DIRECTORY;
      }
    }
    return DEFAULT_DEPOT_ID_DIRECTORY;
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
      String bucketName = servletContext.getInitParameter("s3.config.bucketName");
      if (bucketName != null) {
        _log.info("servlet context provided config bucketName=" + bucketName);
        setBucketName(bucketName);
      } else {
        _log.info("servlet context missing bucketName, using "
            + getBucketName());
      }
    }
  }

  @Override
  public List<String> listFiles(String s3Path) {
    
    List<String> paths = listFiles(s3Path, MAX_RESULTS);
    List<String> files = new ArrayList<String>(paths.size());
    for (String path : paths) {
      files.add(this.parseFileName(path));
    }
    return files;
  }

}
