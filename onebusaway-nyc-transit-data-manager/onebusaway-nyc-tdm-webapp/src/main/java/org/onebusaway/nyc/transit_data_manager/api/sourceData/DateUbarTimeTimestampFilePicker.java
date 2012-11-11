package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * This class is designed to return a most recent file from a directory
 * containing a set of files with filenames of the form
 * ~file_prefix~20111015_1015~file_suffix~ where 20111015 is the date, 1015 is
 * the time and file prefix and suffix are strings appended to show what the
 * contens of the file are.
 * 
 * @author sclark
 * 
 */
public abstract class DateUbarTimeTimestampFilePicker implements
    MostRecentFilePicker {

  private File uploadsDir;

  public DateUbarTimeTimestampFilePicker(String timestampedUploadsDir)
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

      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith(getFilePrefix());
      }
    };
  }

  protected abstract String getFilePrefix();

  protected abstract String getFileSuffix();

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
      int firstStrDate = Integer.parseInt(firstWoPreSuffix.substring(0,
          firstWoPreSuffix.indexOf('_')));
      int firstStrTime = Integer.parseInt(firstWoPreSuffix.substring(firstWoPreSuffix.indexOf('_') + 1));

      String secondWoPreSuffix = secondStr.substring(getFilePrefix().length(),
          secondStr.indexOf(getFileSuffix()));
      int secondStrDate = Integer.parseInt(secondWoPreSuffix.substring(0,
          secondWoPreSuffix.indexOf('_')));
      int secondStrTime = Integer.parseInt(secondWoPreSuffix.substring(secondWoPreSuffix.indexOf('_') + 1));

      if (firstStrDate > secondStrDate)
        return 1;
      else if (secondStrDate > firstStrDate)
        return -1;
      else {
        if (firstStrTime > secondStrTime)
          return 1;
        else if (secondStrTime > firstStrTime)
          return -1;
        else
          return 0;
      }
    }

  }

}
