package org.onebusaway.nyc.admin.service.bundle;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;

public interface BundleBuildingService {
  void setup();

  void download(BundleBuildRequest request, BundleBuildResponse response);

  void prepare(BundleBuildRequest request, BundleBuildResponse response);

  int build(BundleBuildRequest request, BundleBuildResponse response);

  void assemble(BundleBuildRequest request, BundleBuildResponse response);
  
  void upload(BundleBuildRequest request, BundleBuildResponse response);

  void doBuild(BundleBuildRequest request, BundleBuildResponse response);

  String getDefaultAgencyId();


}
