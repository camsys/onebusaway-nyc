package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import java.io.File;

public interface MostRecentFilePicker {

  /**
   * This method simply returns the most recent source file from the entire set
   * of source file data.
   * 
   * @return A File object pointing to the most recent source file.
   */
  File getMostRecentSourceFile();
}
