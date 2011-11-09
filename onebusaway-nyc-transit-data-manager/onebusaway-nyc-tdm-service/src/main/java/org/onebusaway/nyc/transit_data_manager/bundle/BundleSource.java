package org.onebusaway.nyc.transit_data_manager.bundle;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;

public interface BundleSource {
  /**
   * Get all the bundles that seem initially correct.
   * @return A list of the available bundle objects.
   */
  List<Bundle> getBundles();
  
  /**
   * Get the full path of a bundle file.
   * @param bundleId
   * @param relativeFilePath
   * @return
   */
  File getBundleFile(String bundleId, String relativeFilePath);
}
