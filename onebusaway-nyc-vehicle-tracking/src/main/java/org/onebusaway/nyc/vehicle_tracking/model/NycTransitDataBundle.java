package org.onebusaway.nyc.vehicle_tracking.model;

import java.io.File;

import org.onebusaway.transit_data_federation.bundle.FederatedTransitDataBundleCreator;

/**
 * Captures the file structure of various file artifacts of a NYC transit data
 * bundle. All artifact file paths are relative to a base path.
 * 
 * @author bdferris
 * @see FederatedTransitDataBundleCreator
 */
public class NycTransitDataBundle {

  private File _path;

  public NycTransitDataBundle(File path) {
    _path = path;
  }

  public NycTransitDataBundle() {

  }

  public void setPath(File path) {
    _path = path;
  }

  public File getPath() {
    return _path;
  }

  public File getBaseLocationsPath() {
    return new File(_path, "BaseLocations.txt");
  }
}
