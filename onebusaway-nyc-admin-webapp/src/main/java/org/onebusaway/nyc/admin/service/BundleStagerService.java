package org.onebusaway.nyc.admin.service;

import javax.ws.rs.core.Response;

public interface BundleStagerService {

  public Response stage(String environment, String bundleDir, String bundleName);

  public Response stageStatus(String id);

  public Response getBundleList();

  public Response getBundleFile(String bundleId, String relativeFilename);

}
