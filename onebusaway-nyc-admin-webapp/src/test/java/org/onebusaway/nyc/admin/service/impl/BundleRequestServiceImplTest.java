package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.FileService;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BundleRequestServiceImplTest {
  private static Logger _log = LoggerFactory.getLogger(FileServiceImplTest.class);
  private BundleRequestServiceImpl service;
  
  @Before
  public void setup() {
    service = new BundleRequestServiceImpl();
    service.setup();
    service.setBundleValidationService(new BundleValidationServiceImpl());
    FileService fileService = new FileServiceImpl() {
      public void setup() {};
      public boolean bundleDirectoryExists(String filename) {
        return !"noSuchDirectory".equals(filename);
      }
      public boolean createBundleDirectory(String filename) {
        return true;
      };
      public List<String[]> listBundleDirectories(int maxResults) {
        ArrayList<String[]> list = new ArrayList<String[]>();
        String[] columns = {"2012April/", "", "" + System.currentTimeMillis()};
        list.add(columns);
        return list;
      }
      public String get(String key, String tmpDir) {
        InputStream input = this.getClass().getResourceAsStream(
        "empty_feed.zip");
        String destination = "/tmp/empty_feed.zip.html";
        new FileUtils().copy(input, destination);
        return destination;
      }
    };
    fileService.setup();
    fileService.setBucketName("obanyc-bundle-data");
    service.setFileService(fileService);
  }
  
  @Test
  public void testValidate() throws Exception {
    BundleRequest req = new BundleRequest();
    String key= "2012Jan/gtfs_latest/google_transit_brooklyn.zip";
    req.getGtfsList().add(key);
    BundleResponse res = service.validate(req);
    assertFalse(res.isComplete());
    
    int count = 0;
    while (count < 300 && !res.isComplete()) {
      _log.info("sleeping[" + count + "]...");
      Thread.sleep(10000);
      count++;
    }
    _log.error("exception=" + res.getException());
    assertNull(res.getException());
    assertTrue(res.isComplete());
    assertNotNull(res.getValidationFiles());
    assertEquals(1, res.getValidationFiles().size());
  }
}
