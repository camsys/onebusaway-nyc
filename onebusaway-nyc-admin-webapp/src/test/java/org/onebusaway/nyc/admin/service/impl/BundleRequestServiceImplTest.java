package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.impl.BundleValidationServiceImpl;
import org.onebusaway.nyc.admin.service.server.impl.BundleServerServiceImpl;

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
    fileService.setBucketName("obanyc-bundle-data-test");
    
    
    BundleServerServiceImpl bundleServer = new BundleServerServiceImpl() {
      @Override
      public void setup() {
        // no op
      }
      @Override
      public String start(String instanceId) {
        return instanceId;
      }
      @Override
      public String pollPublicDns(String instanceId, int maxWaitSeconds) {
        return "localhost";
      }
      @Override
      public String findPublicDns(String instanceId) {
        return "localhost";
      }
      @Override
      public String findPublicIp(String instanceId) {
        return "127.0.0.1";
      }
      @Override
      public String stop(String instanceId) {
        return instanceId;
      }
      @Override
      public boolean ping(String instanceId) {
        return true;
      }
      @SuppressWarnings("unchecked")
      @Override
      public <T> T makeRequest(String instanceId, String apiCall, Object payload, Class<T> returnType, int waitTimeInSeconds) {
        _log.error("makeRequest called with apiCall=" + apiCall + " and payload=" + payload);
        if (apiCall.equals("/validate/2012Jan/test_0/1/create")) {
          BundleResponse br = new BundleResponse("1");
          return (T) br;
        } else if (apiCall.equals("/ping")) {
          return (T)"{1}";
        } else if (apiCall.equals("/validate/1/list")) {
          BundleResponse br = new BundleResponse("1");
          br.addValidationFile("file1");
          br.addValidationFile("file2");
          br.setComplete(true);
          return (T) br;
        }
        return null;
      }
    };
    bundleServer.setEc2User("user");
    bundleServer.setEc2Password("password");
    bundleServer.setup();
    service.setBundleServerService(bundleServer);
    validationService.setFileService(fileService);
  }
  
  @Test
  public void testValidate() throws Exception {
    BundleRequest req = new BundleRequest();
    String key= "2012Jan";
    // String key = "m34"; // use for faster testing
    req.setBundleDirectory(key);
    req.setBundleBuildName("test_0");
    _log.info("calling validate for dir=" + req.getBundleDirectory() + " name=" + req.getBundleBuildName());
    BundleResponse res = service.validate(req);
    assertFalse(res.isComplete());
    
    int count = 0;
    while (count < 300 && !res.isComplete() && res.getException() == null) {
      //_log.info("sleeping[" + count + "]...");
      Thread.sleep(10 * 1000);
      count++;
      // NOTE: this is optional to demonstrate retrieval service
      _log.info("calling lookup(local) for id=" + res.getId());
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
