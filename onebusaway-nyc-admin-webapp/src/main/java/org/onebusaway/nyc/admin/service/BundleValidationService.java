package org.onebusaway.nyc.admin.service;

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
  
}
