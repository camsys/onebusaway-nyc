package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.impl.BundleValidationServiceImpl;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BundleRequestServiceImplTest {
  private static Logger _log = LoggerFactory.getLogger(BundleRequestServiceImplTest.class);
  private BundleRequestServiceImpl service;
  
  @Before
  public void setup() {
    BundleValidationServiceImpl validationService = new BundleValidationServiceImpl();
    service = new BundleRequestServiceImpl();
    service.setup();
    service.setBundleValidationService(validationService);
    FileService fileService = new FileServiceImpl() {
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
      @Override
      public String get(String key, String tmpDir) {
        InputStream input = this.getClass().getResourceAsStream(
        "empty_feed.zip");
        String destination = "/tmp/empty_feed.zip.html";
        new FileUtils().copy(input, destination);
        return destination;
      }
      @Override
      public String put(String key, String tmpDir) {
        // no op
        return null;
      }
    };
    fileService.setup();
    fileService.setBucketName("obanyc-bundle-data");
    validationService.setFileService(fileService);
  }
  
  @Test
  public void testValidate() throws Exception {
    BundleRequest req = new BundleRequest();
    String key= "2012Jan";
    req.setBundleDirectory(key);
    req.setBundleBuildName("test_0");
    BundleResponse res = service.validate(req);
    assertFalse(res.isComplete());
    
    int count = 0;
    while (count < 30 && !res.isComplete()) {
      _log.info("sleeping[" + count + "]...");
      Thread.sleep(1000);
      count++;
      // NOTE: this is optional to demonstrate retrieval service
      res = service.lookupValidationRequest(res.getId());
      assertNotNull(res);
    }

    if (res.getException() != null) {
      _log.error("Failed with exception=" + res.getException());
    }
    assertNull(res.getException());
    assertTrue(res.isComplete());
    assertNotNull(res.getValidationFiles());
    assertEquals(2, res.getValidationFiles().size());
  }
}
