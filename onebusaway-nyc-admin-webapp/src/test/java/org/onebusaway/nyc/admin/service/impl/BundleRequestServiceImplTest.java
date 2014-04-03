package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.service.bundle.impl.BundleValidationServiceImpl;
import org.onebusaway.nyc.admin.service.server.impl.BundleServerServiceImpl;
import org.onebusaway.nyc.admin.util.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BundleRequestServiceImplTest {
  private static Logger _log = LoggerFactory.getLogger(BundleRequestServiceImplTest.class);
  private BundleRequestServiceImpl service;
  
  @Before
  public void setup() {
    BundleValidationServiceImpl validationService = new BundleValidationServiceImpl();
    service = new BundleRequestServiceImpl();
    service.setInstanceId("localhost");
    service.setup();

    FileService fileService = new S3FileServiceImpl() {
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
      public <T> T makeRequest(String instanceId, String apiCall, Object payload, Class<T> returnType, int waitTimeInSeconds, Map params) {
        _log.debug("makeRequest called with apiCall=" + apiCall + " and payload=" + payload);
        if (apiCall.equals("/validate/remote/2012Jan/test_0/1/create")) {
          BundleResponse br = new BundleResponse("1");
          return (T) br;
        } else if (apiCall.equals("/build/remote/2012Jan/test_0/null/1/2012-04-08/2012-07-07/create") || apiCall.equals("/build/remote/create")) {
          BundleBuildResponse br = new BundleBuildResponse("1");
          return (T) br;
        } else if (apiCall.equals("/ping/remote")) {
          return (T)"{1}";
        } else if (apiCall.equals("/validate/remote/1/list")) {
          BundleResponse br = new BundleResponse("1");
          br.addValidationFile("file1");
          br.addValidationFile("file2");
          br.setComplete(true);
          return (T) br;
        } else if (apiCall.equals("/build/remote/1/list")) {
          BundleBuildResponse br = new BundleBuildResponse("1");
          br.addGtfsFile("file1");
          br.addGtfsFile("file2");
          br.setComplete(true);
          return (T) br;
        } else {
          _log.error("unmatched apiCall=|" + apiCall + "|");
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
    //String key = "m34"; // use for faster testing
    req.setBundleDirectory(key);
    req.setBundleBuildName("test_0");
    _log.debug("calling validate for dir=" + req.getBundleDirectory() + " name=" + req.getBundleBuildName());
    BundleResponse res = service.validate(req);
    assertFalse(res.isComplete());
    
    int count = 0;
    while (count < 300 && !res.isComplete() && res.getException() == null) {
      //_log.info("sleeping[" + count + "]...");
      Thread.sleep(10 * 1000);
      count++;
      // NOTE: this is optional to demonstrate retrieval service
      _log.debug("calling lookup(local) for id=" + res.getId());
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
  
  @Test
  public void testBuild() throws Exception {
    BundleBuildRequest req = new BundleBuildRequest();
    String key= "2012Jan";
    //String key = "m34"; // use for faster testing
    req.setBundleDirectory(key);
    req.setBundleName("test_0");
    req.setTmpDirectory(new FileUtils().createTmpDirectory());
    req.setBundleStartDate("2012-04-08");
    req.setBundleEndDate("2012-07-07");
    _log.debug("calling build for dir=" + req.getBundleDirectory() + " name=" + req.getBundleName());
    BundleBuildResponse res = service.build(req);
    assertFalse(res.isComplete());
    
    int count = 0;
    while (count < 300 && !res.isComplete() && res.getException() == null) {
      //_log.info("sleeping[" + count + "]...");
      Thread.sleep(10 * 1000);
      count++;
      // NOTE: this is optional to demonstrate retrieval service
      _log.info("calling lookup(local) for id=" + res.getId());
      res = service.lookupBuildRequest(res.getId());
      assertNotNull(res);
    }

    if (res.getException() != null) {
      _log.error("Failed with exception=" + res.getException());
    }
    assertNull(res.getException());
    assertTrue(res.isComplete());
    assertNotNull(res.getGtfsList());
    assertEquals(2, res.getGtfsList().size());
  }
}
