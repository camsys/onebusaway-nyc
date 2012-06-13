package org.onebusaway.nyc.admin.service.bundle;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.model.ServiceDateRange;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface BundleValidationService {
  List<ServiceDateRange> getServiceDateRanges(InputStream gtfsZipFile);
  Map<String, List<ServiceDateRange>> getServiceDateRangesByAgencyId(List<ServiceDateRange> ranges);
  Map<String, List<ServiceDateRange>> getServiceDateRangesAcrossAllGtfs(List<InputStream> gtfsZipFiles);
  int validateGtfs(String gtfsZipFileName, String outputFile);
  int installAndValidateGtfs(String gtfsZipFileName, String outputFile);
  void upload(BundleRequest request, BundleResponse response);
  void downloadAndValidate(BundleRequest request, BundleResponse response);
}
