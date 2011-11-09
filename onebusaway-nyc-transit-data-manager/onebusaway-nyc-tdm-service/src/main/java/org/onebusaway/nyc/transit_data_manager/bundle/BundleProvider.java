package org.onebusaway.nyc.transit_data_manager.bundle;

import java.io.File;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;

public class BundleProvider {

  private BundleSource bundleSource;
  
  public BundleProvider(BundleSource bundleSource) {
    super();
    
    this.bundleSource = bundleSource;
  }
  
  public List<Bundle> getBundles () {
    return bundleSource.getBundles();
  }
  
  public File getBundleFile (String bundleId, String relativeFilePath) {
    return bundleSource.getBundleFile(bundleId, relativeFilePath);
  }
}
