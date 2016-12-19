package org.onebusaway.nyc.admin.service.bundle;

public interface GtfsValidationService {
	void downloadFeedValidator();
	int validateGtfs(String gtfsZipFileName, String outputFile);
	String getOutputExtension();

}
