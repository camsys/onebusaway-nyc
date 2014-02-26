package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.File;
import java.util.List;

import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.transit_data_federation.services.FederatedTransitDataBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.conveyal.gtfs.model.InvalidValue;
import com.conveyal.gtfs.model.ValidationResult;
import com.conveyal.gtfs.service.GtfsValidationService;

public class GtfsValidationTask implements Runnable {
  private static final String FILENAME = "gtfs_validation.csv";
  private Logger _log = LoggerFactory.getLogger(GtfsStatisticsTask.class);
  private GtfsMutableRelationalDao _dao;
  private FederatedTransitDataBundle _bundle;
  @Autowired
  private MultiCSVLogger logger;

  
  public void setLogger(MultiCSVLogger logger) {
    this.logger = logger;
  }

  
  @Autowired
  public void setGtfsDao(GtfsMutableRelationalDao dao) {
    _dao = dao;
  }

  @Autowired
  public void setBundle(FederatedTransitDataBundle bundle) {
    _bundle = bundle;
  }
  @Override
  public void run() {
    File basePath = _bundle.getPath();
    _log.info("Starting GTFS valdiation to basePath=" + basePath);
    logger.header(FILENAME, InvalidValueHelper.getCsvHeader());
    GtfsValidationService service = new GtfsValidationService(_dao);
    ValidationResult vr = service.validateRoutes();
    log(vr, FILENAME);
    vr = service.validateTrips();
    log(vr, FILENAME);
    vr = service.duplicateStops();
    log(vr, FILENAME);
    vr = service.listReversedTripShapes();
    log(vr, FILENAME);
    _log.info("Exiting");
  }


  private void log(ValidationResult vr, String file) {
    List<InvalidValue> invalidValues = vr.invalidValues;
    for (InvalidValue iv : invalidValues) {
      logger.logCSV(file, InvalidValueHelper.getCsv(iv));
    }

    
  }

}
