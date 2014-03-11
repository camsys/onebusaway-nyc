package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs_transformer.GtfsTransformer;
import org.onebusaway.gtfs_transformer.GtfsTransformerLibrary;
import org.onebusaway.gtfs_transformer.factory.TransformFactory;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class GtfsModTask implements Runnable {

  private static Logger _log = LoggerFactory.getLogger(GtfsModTask.class);
  private ApplicationContext _applicationContext;
  private String _outputDirectory;

  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    _applicationContext = applicationContext;
  }

  @Autowired
  private MultiCSVLogger logger;

  public void setLogger(MultiCSVLogger logger) {
	this.logger = logger;
  }
	
  @Autowired
  private ConfigurationServiceClient configurationServiceClient;

  public ConfigurationServiceClient getConfigurationServiceClient() {
	return configurationServiceClient;
	}
	
  public void setConfigurationServiceClient(ConfigurationServiceClient configurationServiceClient) {
    this.configurationServiceClient = configurationServiceClient;
  }
  
  public void setOutputDirectory(String outputDirectory) {
    _outputDirectory = outputDirectory;
  }
  
  @Override
  public void run() {
    try {
      _log.info("GtfsModTask Starting");
      logger.changelogHeader();
      GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
      for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {
        String agencyId = parseAgencyDir(gtfsBundle.getPath().getPath());
        if (agencyId != null) {
          // lookup meta info for agency
          String modUrl = getModUrl(agencyId);
          _log.info("using modUrl=" + modUrl + " for agency " + agencyId + " and bundle " + gtfsBundle.getPath());
          if (modUrl != null) {
            // run the mod script on this gtfsBundle
            String oldFilename = gtfsBundle.getPath().getPath();
            String newFilename = runModifications(gtfsBundle, agencyId);
            logger.changelog("Transformed " + oldFilename + " to " + newFilename + " according to url " + getModUrl(agencyId));
          } else {
            _log.info("no modUrl found for agency " + agencyId + " and bundle " + gtfsBundle.getPath());
          }
        }
      }
    } catch (Throwable ex) {
      _log.error("error modifying gtfs:", ex);
    } finally {
      _log.info("GtfsModTask Exiting");
    }
  }

  private String runModifications(GtfsBundle gtfsBundle, String agencyId) throws Exception {
      GtfsTransformer mod = new GtfsTransformer();
      TransformFactory factory = mod.getTransformFactory();
      // add models outside the default namespace
      factory.addEntityPackage("org.onebusaway.king_county_metro_gtfs.model");
      
      String outputDirectory = parseDirectory(gtfsBundle.getPath().getPath());

      List<File> paths = new ArrayList<File>();
      paths.add(gtfsBundle.getPath());
      _log.info("transformer path=" + gtfsBundle.getPath() +"; output=" + outputDirectory);
      mod.setGtfsInputDirectories(paths);
      mod.setOutputDirectory(new File(outputDirectory));
      GtfsTransformerLibrary.configureTransformation(mod, getModUrl(agencyId));
      String path = gtfsBundle.getPath().getPath();
      if (getTransform(agencyId, path) != null) {
        _log.info("using transform=" + getTransform(agencyId, path));
        factory.addModificationsFromString(getTransform(agencyId, path));
      }
      
      _log.info("running...");
      mod.run();
      _log.info("done!");
      // cleanup
      return cleanup(gtfsBundle);
  }

  private String cleanup(GtfsBundle gtfsBundle) throws Exception {
    File gtfsFile = gtfsBundle.getPath();
    FileUtility fu = new FileUtility();
    FileUtils fs = new FileUtils();
    
    _log.info("gtfsBundle.getPath=" + gtfsFile.getPath());
    String oldGtfsName = gtfsFile.getPath().toString();
    // delete the old zip file
    _log.info("deleting " + gtfsFile.getPath());
    gtfsFile.delete();
    // create a new zip file
    
    String newGtfsName = fs.parseDirectory(oldGtfsName) + File.separator + fs.parseFileNameMinusExtension(oldGtfsName) + "_mod.zip";
    
    String basePath = fs.parseDirectory(oldGtfsName);
    String includeExpression = ".*\\.txt";
    fu.zip(newGtfsName, basePath, includeExpression);
    int deletedFiles = fu.deleteFilesInFolder(basePath, includeExpression);
    if (deletedFiles < 1) {
      throw new IllegalStateException("Missing expected modded gtfs files in directory " + basePath);
    }

    gtfsBundle.setPath(new File(newGtfsName));
    _log.info("gtfsBundle.getPath(mod)=" + gtfsBundle.getPath());
    
    if (_outputDirectory != null) {
      String outputLocation = _outputDirectory + File.separator + fs.parseFileName(newGtfsName);
      // copy to outputs for downstream systems
      FileUtils.copyFile(new File(newGtfsName), new File(outputLocation));
    }
    return newGtfsName;
    
  }

  private String getTransform(String agencyId, String path) {
    try {
		return configurationServiceClient.getItem("admin", agencyId+"_transform").replaceAll(":path", path);
	} catch (Exception e) {}
    return null;
  }

  private String getModUrl(String agencyId) {
    try {
		return configurationServiceClient.getItem("admin", agencyId+"_modurl");
	} catch (Exception e) {}
    return null;
  }

  private String parseAgencyDir(String path) {
    String agency = null;
    int lastSlash = path.lastIndexOf(File.separatorChar);
    if (lastSlash < 0) return agency;
    int firstBar = path.indexOf('_', lastSlash);
    if (firstBar < 0) return agency;
    
    return path.substring(lastSlash+1, firstBar);
  }
  
  private String parseDirectory(String path) {
    int lastSlash = path.lastIndexOf(File.separatorChar);
    if (lastSlash < 0) return null;
    
    return path.substring(0, lastSlash);
  }


  private GtfsBundles getGtfsBundles(ApplicationContext context) {

    GtfsBundles bundles = (GtfsBundles) context.getBean("gtfs-bundles");
    if (bundles != null)
      return bundles;

    GtfsBundle bundle = (GtfsBundle) context.getBean("gtfs-bundle");
    if (bundle != null) {
      bundles = new GtfsBundles();
      bundles.getBundles().add(bundle);
      return bundles;
    }

    throw new IllegalStateException(
        "must define either \"gtfs-bundles\" or \"gtfs-bundle\" in config");
  }

}
