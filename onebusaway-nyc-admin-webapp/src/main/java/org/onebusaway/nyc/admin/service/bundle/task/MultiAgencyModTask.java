package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiAgencyModTask extends BaseModTask implements Runnable {

  private static Logger _log = LoggerFactory.getLogger(GtfsModTask.class);
  

  @Override
  public void run() {
    try {
      _log.info("GtfsModTask Starting");
      logger.changelogHeader(requestResponse.getRequest().getBundleComment());
      
      makeOutputDirectory(requestResponse.getResponse());
      
      GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
      for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {
        String agencyId = parseAgencyDir(gtfsBundle.getPath().getPath());
        //_log.info("no modUrl found for agency " + agencyId + " and bundle " + gtfsBundle.getPath());
        String oldFilename = gtfsBundle.getPath().getPath();
        String newFilename = runModifications(gtfsBundle, agencyId, getEmptyModUrl(), null);
        copyToOutputGtfsDirectory(requestResponse.getResponse(), newFilename);
        logger.changelog("Transformed " + oldFilename + " to " + newFilename + " to add multi-agency support");
      }
    } catch (Throwable ex) {
      _log.error("error modifying gtfs:", ex);
    } finally {
      _log.info("GtfsModTask Exiting");
    }
  }


  private void copyToOutputGtfsDirectory(BundleBuildResponse response,
      String srcFilename) throws Exception {
    if (response.getBundleOutputGtfsDirectory() == null) {
      _log.error("bundle output gtfs dir not configured, not copying " + srcFilename);
      return;
    }
    
    File srcFile = new File(srcFilename);
    File destFile = new File(requestResponse.getResponse().getBundleOutputGtfsDirectory() + File.separator + srcFile.getName());
    FileUtils.copyFile(srcFile, destFile, true);
    logger.changelog("applied output gtfs tag to " + destFile);
    response.addOutputGtfsFile(destFile.toString()); 
  }


  private void makeOutputDirectory(BundleBuildResponse response) {
    if (response.getBundleOutputGtfsDirectory() == null) {
      _log.error("bundle output gtfs dir not configured");
      return;
    }
    File outputDirectory = new File(response.getBundleOutputGtfsDirectory());
    if (outputDirectory.exists() && outputDirectory.isDirectory()) return;
    outputDirectory.mkdirs();
  }

}
