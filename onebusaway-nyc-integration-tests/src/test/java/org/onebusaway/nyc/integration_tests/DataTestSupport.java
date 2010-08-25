package org.onebusaway.nyc.integration_tests;

import java.io.InputStream;

public class DataTestSupport {
  public static InputStream getTestDataAsInputStream() {
    return DataTestSupport.class.getResourceAsStream("ivn-dsc.csv");
  }
}
