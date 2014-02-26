package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs_transformer.GtfsTransformer;
import org.onebusaway.gtfs_transformer.GtfsTransformerLibrary;
import org.onebusaway.gtfs_transformer.factory.TransformFactory;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class GtfsModTask implements Runnable {

  private static Logger _log = LoggerFactory.getLogger(GtfsModTask.class);
  private ApplicationContext _applicationContext;

  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    _applicationContext = applicationContext;
  }

  @Autowired
  private MultiCSVLogger logger;

  
  public void setLogger(MultiCSVLogger logger) {
    this.logger = logger;
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
            runModifications(gtfsBundle, agencyId);
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

  

  private void runModifications(GtfsBundle gtfsBundle, String agencyId) throws Exception {
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
      _log.info("running...");
      mod.run();
      _log.info("done!");
      logger.changelog("Transformed " + gtfsBundle.getPath() + " according to url " + getModUrl(agencyId));
      // cleanup
      _log.info("gtfsBundle.getPath=" + gtfsBundle.getPath());
      
      // TODO delete the zip file
      gtfsBundle.setPath(new File(outputDirectory));
      _log.info("gtfsBundle.getPath(mod)=" + gtfsBundle.getPath());
  }

  private String getModUrl(String agencyId) {
    // TODO pull this from configuration
    if ("1".equals(agencyId))
      return "https://raw.github.com/wiki/camsys/onebusaway-application-modules/KingCountyMetroModifications.mediawiki";
    if ("3".equals(agencyId))
      return "https://raw.github.com/wiki/camsys/onebusaway-application-modules/PierceTransitModifications.mediawiki";
    if ("19".equals(agencyId))
      return "https://raw.github.com/wiki/camsys/onebusaway-application-modules/IntercityTransitModifications.mediawiki";
    if ("40".equals(agencyId))
      return "https://raw.github.com/wiki/camsys/onebusaway-application-modules/SoundTransitModifications.mediawiki";
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
