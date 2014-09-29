package org.onebusaway.nyc.transit_data_manager.bundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;

public class BundleProvider {

  private BundleSource bundleSource;
  
  public BundleProvider(BundleSource bundleSource) {
    this.bundleSource = bundleSource;
  }
  
  public List<Bundle> getBundles () {
    return bundleSource.getBundles();
  }
  
  public File getBundleFile (String bundleId, String relativeFilePath) throws FileNotFoundException {
    return bundleSource.getBundleFile(bundleId, relativeFilePath);
  }
  
  public boolean checkIsValidBundleFile (String bundleId, String relativeFilePath) {
    return bundleSource.checkIsValidBundleFile(bundleId, relativeFilePath);
  }

}
