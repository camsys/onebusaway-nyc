package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.List;

import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SCHPullInOutInfo;

public class ImporterVehiclePulloutData implements PulloutData {

  private List<SCHPullInOutInfo> pulloutData = null;

  public ImporterVehiclePulloutData(List<SCHPullInOutInfo> pulloutData) {
    this.pulloutData = pulloutData;
  }
  
  public List<SCHPullInOutInfo> getAllPullouts() {
    return pulloutData;
  }
}
