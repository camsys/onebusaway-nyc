package org.onebusaway.nyc.transit_data_manager.api.barcode.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipFileTesterUtil {
  public ZipFileTesterUtil() {
  }
  
  public int getNumEntriesInZipFile(File file) throws ZipException, IOException {
    ZipFile zipFile = new ZipFile(file);
    int numActualEntries = zipFile.size();

    zipFile.close();
    
    return numActualEntries;
  }
  
  public int getNumEntriesInZipInputStream(InputStream is) throws IOException {
    ZipInputStream zis = new ZipInputStream(is);

    int numFilesInZip = 0;
    while (zis.getNextEntry() != null) {
      numFilesInZip++;
    }

    zis.close();
    
    return numFilesInZip;
  }
}
