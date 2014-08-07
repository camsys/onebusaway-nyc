package org.onebusaway.nyc.transit_data_manager.bundle;

import java.io.File;
import java.io.FileNotFoundException;

public interface StagingBundleSource {
  boolean checkIsValidBundleFile(String bundleId, String relativeFilePath);

  File getBundleFile(String bundleId,
      String relativeFilePath) throws FileNotFoundException;
  //void stage(String env, String bundleDir, String bundleName) throws Exception;

}
