package org.onebusaway.nyc.transit_data_manager.bundle;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;
import org.onebusaway.nyc.transit_data_manager.json.model.JsonMessage;

public class BundlesListMessage extends JsonMessage {
  List<Bundle> bundles;

  public List<Bundle> getBundles() {
    return bundles;
  }

  public void setBundles(List<Bundle> bundles) {
    this.bundles = bundles;
  }
}
