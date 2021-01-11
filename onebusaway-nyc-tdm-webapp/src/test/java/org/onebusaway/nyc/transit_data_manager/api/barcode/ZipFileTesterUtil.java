/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.api.barcode;

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
