package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import java.io.IOException;

public class UtsCrewUploadsFilePicker extends DateUbarTimeTimestampFilePicker
    implements MostRecentFilePicker {

  private static String FILE_PREFIX = "CIS_";
  private static String FILE_SUFFIX = ".txt";

  public UtsCrewUploadsFilePicker(String timestampedUploadsDir)
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
