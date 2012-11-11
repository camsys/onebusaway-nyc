package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class DscManualUploadDateTimestampFilePicker implements MostRecentFilePicker {

  private static String FILE_PREFIX = "dsc_";
  private static String FILE_SUFFIX = ".csv";
  
  private File uploadsDir;

  public DscManualUploadDateTimestampFilePicker(String timestampedUploadsDir)
      throws IOException {

    File dir = new File(timestampedUploadsDir);

    if (!dir.exists() || !dir.isDirectory()) {
      throw new IOException(timestampedUploadsDir
          + " is not a directory or does not exist.");
    }

    uploadsDir = dir;
  }
  
  @Override
  public File getMostRecentSourceFile() {
    String[] uploadFilenames = uploadsDir.list(getFilenameFilter());

    if (uploadFilenames.length == 0)
      return null;

    Arrays.sort(uploadFilenames, new timestampAfterPrefixFilenameComparator());

    String latestFilename = uploadFilenames[(uploadFilenames.length - 1)];

    return new File(uploadsDir, latestFilename);
  }

  protected FilenameFilter getFilenameFilter() {
    return new FilenameFilter() {

      String matchFilenameRegex = getFilePrefix() + "\\d{8}" + getFileSuffix();
      
      @Override
      public boolean accept(File dir, String name) {
        return name.matches(matchFilenameRegex);
      }
    };
  }

  protected String getFilePrefix() {
    return FILE_PREFIX;
  }

  protected String getFileSuffix() {
    return FILE_SUFFIX;
  }

  private class timestampAfterPrefixFilenameComparator implements
      Comparator<String> {

    /*
     * (non-Javadoc)
     * 
     * In this method we should basically be comparing strings like
     * CIS_20111216_1704.txt . The goal is to return the most recent of course.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */

    @Override
    public int compare(String firstStr, String secondStr) {

      if (firstStr.equalsIgnoreCase(secondStr))
        return 0;

      String firstWoPreSuffix = firstStr.substring(getFilePrefix().length(),
          firstStr.indexOf(getFileSuffix()));
      
      int firstStrDate = Integer.parseInt(firstWoPreSuffix);

      String secondWoPreSuffix = secondStr.substring(getFilePrefix().length(),
          secondStr.indexOf(getFileSuffix()));
      
      int secondStrDate = Integer.parseInt(secondWoPreSuffix);
      
      if (firstStrDate > secondStrDate)
        return 1;
      else if (secondStrDate > firstStrDate)
        return -1;
      else 
        return 0;
    }

  }

}
