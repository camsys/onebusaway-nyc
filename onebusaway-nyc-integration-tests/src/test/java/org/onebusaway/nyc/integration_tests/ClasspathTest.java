package org.onebusaway.nyc.integration_tests;

import java.io.File;

import org.junit.Test;

public class ClasspathTest {

  @Test
  public void test() {
    System.out.println("=== CLASSPATH BEGIN ===");
    for (String path : System.getProperty("java.class.path").split(
        File.pathSeparator))
      System.out.println(path);
    System.out.println("=== CLASSPATH END ===");
  }
}
