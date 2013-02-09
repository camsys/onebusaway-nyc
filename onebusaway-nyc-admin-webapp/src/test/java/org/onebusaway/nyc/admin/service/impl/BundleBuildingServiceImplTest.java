package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.BundleBuildingService;
import org.onebusaway.nyc.admin.service.bundle.impl.BundleBuildingServiceImpl;
import org.onebusaway.nyc.admin.util.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BundleBuildingServiceImplTest {
  private static Logger _log = LoggerFactory.getLogger(BundleBuildingServiceImplTest.class);
  private BundleBuildingServiceImpl _service;

  @Before
  public void setup() {
    _service = new BundleBuildingServiceImpl() {
      @Override
      public String getDefaultAgencyId() {
        return null;
      }
    };
    
    FileService fileService;
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
        _log.error("list called with " + directory);
        ArrayList<String> list = new ArrayList<String>();
        if (directory.equals("test/gtfs_latest")) {
          list.add("gtfs-m34.zip");
        } else if (directory.equals("test/stif_latest")) {
          list.add("stif-m34.zip");
        } else if (directory.equals("test/config")) {
          // do nothing
        } else {
          list.add("empty");
        }
        return list;
      }

      @Override
      public String get(String key, String tmpDir) {
        _log.error("get called with " + key);
        InputStream source = null;
        if (key.equals("gtfs-m34.zip")) {
          source = this.getClass().getResourceAsStream(
              "gtfs-m34.zip");
        } else if (key.equals("stif-m34.zip")) {
          source = this.getClass().getResourceAsStream("stif-m34.zip");
        }
        String filename = tmpDir + File.separator + key;
        new FileUtils().copy(source, filename);
        return filename;
      }

      @Override
      public String put(String key, String file) {
        // do nothing
        return null;
      }
    };
    fileService.setBucketName("obanyc-bundle-data");
    fileService.setGtfsPath("gtfs_latest");
    fileService.setStifPath("stif_latest");
    fileService.setBuildPath("builds");
    fileService.setConfigPath("config");
    fileService.setup();

    // uncomment for s3
    // fileService = new FileServiceImpl();
    // fileService.setBucketName("obanyc-bundle-data");
    // fileService.setGtfsPath("gtfs_latest");
    // fileService.setStifPath("stif_latest");
    // fileService.setBuildPath("builds");
    // fileService.setConfigPath("config");
    // fileService.setup();
    _service.setFileService(fileService);
    _service.setup();

  }

  @Test
  public void testnoop() {
    // do  nothing
  }
  //@Test
  public void testBuild() {
    String bundleDir = "test";
    String tmpDir = new FileUtils().createTmpDirectory();

    BundleBuildRequest request = new BundleBuildRequest();
    request.setBundleDirectory(bundleDir);
    request.setBundleName("testname");
    request.setTmpDirectory(tmpDir);
    request.setBundleStartDate("2012-04-08");
    request.setBundleEndDate("2012-07-07");
    assertNotNull(request.getTmpDirectory());
    assertNotNull(request.getBundleDirectory());
    BundleBuildResponse response = new BundleBuildResponse(""
        + System.currentTimeMillis());
    assertEquals(0, response.getStatusList().size());

    // step 1
    _service.download(request, response);
    assertNotNull(response.getGtfsList());
    assertEquals(1, response.getGtfsList().size());

    assertNotNull(response.getStifZipList());
    assertEquals(1, response.getStifZipList().size());
     
    assertNotNull(response.getStatusList());
    assertTrue(response.getStatusList().size() > 0);

    assertNotNull(response.getConfigList());
    assertEquals(0, response.getConfigList().size());
    
    // step 2
    _service.prepare(request, response);

    
    assertFalse(response.isComplete());
    
    // step 3
    int rc = _service.build(request, response);
    if (response.getException() != null) {
      _log.error("Failed with exception=" + response.getException());
    }
    assertNull(response.getException());
    assertFalse(response.isComplete());
    assertEquals(0, rc);
    
    // step 4
    // OBANYC-1451 -- fails on OSX TODO
    //_service.assemble(request, response);

    // step 5
    _service.upload(request, response);
    assertFalse(response.isComplete()); // set by BundleRequestService

  }

}
