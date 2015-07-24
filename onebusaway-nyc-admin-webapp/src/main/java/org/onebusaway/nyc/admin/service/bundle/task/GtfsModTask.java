package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsModTask extends BaseModTask implements Runnable {

  private static Logger _log = LoggerFactory.getLogger(GtfsModTask.class);
  

  
  @Override
  public void run() {
    try {
      _log.info("GtfsModTask Starting");
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
            String transform = getTransform(agencyId, gtfsBundle.getPath().toString());
            String newFilename = runModifications(gtfsBundle, agencyId, modUrl, transform);
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


}
