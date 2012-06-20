package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FileServiceImplTest {

  private static Logger _log = LoggerFactory.getLogger(FileServiceImplTest.class);
  private FileServiceImpl fileService;

  @Before
  public void setup() {
    fileService = new FileServiceImpl() {
      @Override
      public void setup() {
      };

      @Override
      public boolean bundleDirectoryExists(String filename) {
        return !"noSuchDirectory".equals(filename);
      }

      @Override
      public boolean createBundleDirectory(String filename) {
        return true;
      };

      @Override
      public List<String[]> listBundleDirectories(int maxResults) {
        ArrayList<String[]> list = new ArrayList<String[]>();
        String[] columns0 = {"2012April", "", "" + System.currentTimeMillis()};
        list.add(columns0);
        String[] columns1 = {"2012Jan", "", "" + System.currentTimeMillis()};
        list.add(columns1);
        String[] columns2 = {"2011April", "", "" + System.currentTimeMillis()};
        list.add(columns2);
        String[] columns3 = {"2011Jan", "", "" + System.currentTimeMillis()};
        list.add(columns3);
        String[] columns4 = {"2010April", "", "" + System.currentTimeMillis()};
        list.add(columns4);
        String[] columns5 = {"2010Jan", "", "" + System.currentTimeMillis()};
        list.add(columns5);
        return list;
      }

      @Override
      public List<String> list(String directory, int maxResults) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("google_transit_brooklyn.zip");
        list.add("google_transit_staten_island.zip");
        return list;
      }
    };
    fileService.setBucketName("obanyc-bundle-data");
    fileService.setGtfsPath("gtfs_latest");
    fileService.setup();

    // un-comment to run against S3
    // fileService = new FileServiceImpl();
    // fileService.setBucketName("obanyc-bundle-data");
    // fileService.setGtfsPath("gtfs_latest");
    // fileService.setStifPath("stif_latest");
    // fileService.setBuildPath("builds");
    // fileService.setup();
  }

  @Test
  public void testBundleDirectoryExists() {
    assertFalse(fileService.bundleDirectoryExists("noSuchDirectory"));
    assertTrue(fileService.bundleDirectoryExists("2012April"));
  }

  @Test
  public void testCreateBundleDirectory() {
    String filename = "testDir" + System.currentTimeMillis();
    assertTrue(fileService.createBundleDirectory(filename));

  }

  @Test
  public void testListBundleDirectories() {
    List<String[]> rows = fileService.listBundleDirectories(1000000000);
    assertNotNull(rows);
    assertTrue(rows.size() > 5);

    String[] row0 = rows.get(0);
    assertEquals("2012April", row0[0]);

  }

  @Test
  public void testList() {
    String gtfsDir = "2012Jan/gtfs_latest";
    List<String> rows = fileService.list(gtfsDir, -1);
    assertNotNull(rows);
    assertEquals(2, rows.size());

  }
}
