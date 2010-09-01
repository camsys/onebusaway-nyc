package org.onebusaway.nyc.vehicle_tracking.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.nyc.stif.StifTripLoader;
import org.onebusaway.nyc.vehicle_tracking.model.DestinationSignCodeRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Load STIF data, including the mapping between destination sign codes and trip
 * ids, into the database
 * 
 * @author bdferris
 * 
 */
public class StifTask implements Runnable {

  private GtfsRelationalDao _gtfsRelationalDao;

  private VehicleTrackingDao _vehicleTrackingDao;

  private File _stifPath;

  @Autowired
  public void setGtfsRelationalDao(GtfsRelationalDao gtfsRelationalDao) {
    _gtfsRelationalDao = gtfsRelationalDao;
  }

  @Autowired
  public void setVehicleTrackingDao(VehicleTrackingDao vehicleTrackingDao) {
    _vehicleTrackingDao = vehicleTrackingDao;
  }

  /**
   * The path of the directory containing STIF files to process
   */
  public void setStifPath(File path) {
    this._stifPath = path;
  }

  public File getStifPath() {
    return _stifPath;
  }

  public void run() {

    StifTripLoader loader = new StifTripLoader();
    loader.setGtfsDao(_gtfsRelationalDao);

    loadStif(_stifPath, loader);

    Map<String, List<AgencyAndId>> tripMapping = loader.getTripMapping();

    for (Map.Entry<String, List<AgencyAndId>> entry : tripMapping.entrySet()) {
      String destinationSignCode = entry.getKey();
      List<AgencyAndId> tripIds = entry.getValue();
      for (AgencyAndId tripId : tripIds) {
        DestinationSignCodeRecord record = new DestinationSignCodeRecord();
        record.setDestinationSignCode(destinationSignCode);
        record.setTripId(tripId);
        _vehicleTrackingDao.saveOrUpdateDestinationSignCodeRecord(record);
      }
    }
  }

  public void loadStif(File path, StifTripLoader loader) {
    
    // Exclude files and directories like .svn
    if( path.getName().startsWith("."))
      return;
    
    if (path.isDirectory()) {
      for (String filename : path.list()) {
        File contained = new File(path, filename);
        loadStif(contained, loader);
      }
    } else {
      loader.run(path);
    }
  }
}
