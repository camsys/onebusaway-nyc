package org.onebusaway.nyc.transit_data_manager.bundle;

import java.io.File;
import java.io.FileNotFoundException;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.transit_data_manager.bundle.model.BundleMetadata;
import org.onebusaway.transit_data_federation.model.bundle.BundleItem;

public class StagingBundleProvider {
  private static final String META_DATA_LOCATION = "metadata.json";
  private static final String ENV = "prod";
  
  private StagingBundleSource bundleSource;
  
  public StagingBundleProvider(StagingBundleSource bundleSource) {
    this.bundleSource = bundleSource;
  }
  
  public File getBundleFile(String bundleDirectory, String relativeFilePath) throws FileNotFoundException {
    return bundleSource.getBundleFile(ENV, relativeFilePath);
  }
  
  public boolean checkIsValidStagedBundleFile (String bundleId, String relativeFilePath) {
    return bundleSource.checkIsValidBundleFile(bundleId, relativeFilePath);
  }
  
  public BundleMetadata getMetadata(String stagingDirectory) throws Exception {
    File file = bundleSource.getBundleFile(stagingDirectory, 
        AbstractBundleSource.BUNDLE_DATA_DIRNAME + File.separator + META_DATA_LOCATION);
    ObjectMapper mapper = new ObjectMapper();
    BundleMetadata meta = mapper.readValue(file, BundleMetadata.class);
    return meta;
  }
}
