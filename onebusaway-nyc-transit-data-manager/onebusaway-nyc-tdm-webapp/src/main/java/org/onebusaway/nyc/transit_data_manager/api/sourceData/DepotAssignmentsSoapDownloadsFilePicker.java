package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import java.io.IOException;

public class DepotAssignmentsSoapDownloadsFilePicker extends
    DateUbarTimeTimestampFilePicker implements MostRecentFilePicker {

  private static String FILE_PREFIX = "depot_assignments_";
  private static String FILE_SUFFIX = ".xml";

  public DepotAssignmentsSoapDownloadsFilePicker(String timestampedUploadsDir)
      throws IOException {
    super(timestampedUploadsDir);
  }

  @Override
  protected String getFilePrefix() {
    return FILE_PREFIX;
  }

  @Override
  protected String getFileSuffix() {
    return FILE_SUFFIX;
  }

}
