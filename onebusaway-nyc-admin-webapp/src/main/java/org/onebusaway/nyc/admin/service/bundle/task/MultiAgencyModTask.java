package org.onebusaway.nyc.admin.service.bundle.task;

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
      logger.changelogHeader();
      GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
      for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {
        String agencyId = parseAgencyDir(gtfsBundle.getPath().getPath());
        //_log.info("no modUrl found for agency " + agencyId + " and bundle " + gtfsBundle.getPath());
        String oldFilename = gtfsBundle.getPath().getPath();
        String newFilename = runModifications(gtfsBundle, agencyId, getEmptyModUrl(), null);
        logger.changelog("Transformed " + oldFilename + " to " + newFilename + " to add multi-agency support");
      }
    } catch (Throwable ex) {
      _log.error("error modifying gtfs:", ex);
    } finally {
      _log.info("GtfsModTask Exiting");
    }
  }

}
