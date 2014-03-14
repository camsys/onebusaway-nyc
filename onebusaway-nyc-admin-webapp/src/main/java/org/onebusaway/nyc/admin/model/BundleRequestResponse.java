package org.onebusaway.nyc.admin.model;

/**
 * Typed request/response objects
 *
 */
public class BundleRequestResponse {

  private BundleBuildRequest request;
  private BundleBuildResponse response;
  
  public BundleBuildRequest getRequest() {
    return request;
  }
  public void setRequest(BundleBuildRequest request) {
    this.request = request;
  }
  public BundleBuildResponse getResponse() {
    return response;
  }
  public void setResponse(BundleBuildResponse response) {
    this.response = response;
  }
}
