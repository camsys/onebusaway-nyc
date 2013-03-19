package org.onebusaway.nyc.transit_data_federation.bundle.model;

import java.io.File;

/**
 * Captures the file structure of various file artifacts of an NYC federated transit
 * data bundle. All artifact file paths are relative to a base path.
 */
public class NycFederatedTransitDataBundle {

  private File _path;

  public NycFederatedTransitDataBundle(File path) {
	  _path = path;
  }

  public NycFederatedTransitDataBundle() {
  }

  public void setPath(File path) {
    _path = path;
  }

  public File getPath() {
    return _path;
  }

  public File getNonRevenueMoveLocationsPath() {
	return new File(_path, "NonRevenueMoveLocations.obj");
  }

  public File getNonRevenueMovePath() {
	return new File(_path, "NonRevenueMoves.obj");
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
