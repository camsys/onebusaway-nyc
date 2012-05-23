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
      public void setup() {};
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
        String[] columns = {"2012April/", "", "" + System.currentTimeMillis()};
        list.add(columns);
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
    List<String[]> rows = fileService.listBundleDirectories(1000);
    assertNotNull(rows);
    assertTrue(rows.size() > 0);

    for (String[] columns : rows) {
      //_log.error("row=" + columns[0]);
    }
    
    String[] row0 = rows.get(0);
    assertEquals("2012April/", row0[0]);

  }
  
  @Test
  public void testList() {
    String gtfsDir = "2012Jan/gtfs_latest";
    List<String> rows = fileService.list(gtfsDir, 0);

    assertNotNull(rows);
    assertEquals(2, rows.size());
    
  }
}
