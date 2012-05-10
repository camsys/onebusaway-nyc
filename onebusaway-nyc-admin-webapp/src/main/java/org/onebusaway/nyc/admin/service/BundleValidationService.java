package org.onebusaway.nyc.admin.service;

import org.onebusaway.nyc.admin.model.ServiceDateRange;

import java.io.InputStream;
import java.util.List;

public interface BundleValidationService {
  List<ServiceDateRange> getServiceDateRanges(InputStream gtfsZipFile);
  ServiceDateRange getCommonServiceDateRange(List<ServiceDateRange> ranges);
  
}
