package org.onebusaway.nyc.transit_data_federation.bundle.model;

import java.io.File;

import org.onebusaway.transit_data_federation.bundle.FederatedTransitDataBundleCreator;

/**
 * Captures the file structure of various file artifacts of a federated transit
 * data bundle. All artifact file paths are relative to a base path.
 * 
 * @author bdferris
 * @see FederatedTransitDataBundleCreator
 */
public class NycFederatedTransitDataBundle {

  private File _path;

  public NycFederatedTransitDataBundle(File path) {
	  _path = path;
  }

  public NycFederatedTransitDataBundle() {}

  public void setPath(File path) {
    _path = path;
  }

  public File getPath() {
    return _path;
  }

  public File getNotInServiceDSCs() {
	return new File(_path, "NotInServiceDSCs.obj");
  }

  public File getTripsForDSCIndex() {
	return new File(_path, "TripsForDSCIndices.obj");
  }

  public File getDSCForTripIndex() {
	return new File(_path, "DSCForTripIndices.obj");
  }

  public File getBaseLocationsPath() {
    return new File(_path, "BaseLocations.txt");
  }
  
  public File getTerminalLocationsPath() {
    return new File(_path, "TerminalLocations.txt");
  }

  public File getTripRunDataPath() {
	return new File(_path, "TripRunData.obj");
  }
}
