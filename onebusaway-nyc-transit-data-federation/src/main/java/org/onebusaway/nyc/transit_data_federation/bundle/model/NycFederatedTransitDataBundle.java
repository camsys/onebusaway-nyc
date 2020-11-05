/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  
  public File getNonRevenueStopsPath() {
    return new File(_path, "NonRevenueStops.obj");
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

public File getSupplimentalTrioInfo() {
	return new File(_path, "SupplimentalTripInfo.obj");
}
}
