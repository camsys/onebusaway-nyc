package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.service.BundleBuildingService;
import org.onebusaway.nyc.admin.service.FileService;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BundleBuildingServiceImplTest {
  private static Logger _log = LoggerFactory.getLogger(BundleBuildingServiceImplTest.class);
  private BundleBuildingServiceImpl _service;

  @Before
  public void setup() {
    _service = new BundleBuildingServiceImpl();
    FileService fileService;
    
    fileService = new FileServiceImpl();
    fileService.setBucketName("obanyc-bundle-data");
    fileService.setGtfsPath("gtfs_latest");
    fileService.setStifPath("stif_latest");
    fileService.setBuildPath("builds");    
    fileService.setup();
    _service.setFileService(fileService);
    _service.setup();

  }

  @Test
  public void testBuild() {
    String bundleDir = "test";
    String tmpDir = new FileUtils().createTmpDirectory();
    
    BundleBuildRequest request = new BundleBuildRequest();
    request.setBundleDirectory(bundleDir);
    request.setTmpDirectory(tmpDir);
    assertNotNull(request.getTmpDirectory());
    assertNotNull(request.getBundleDirectory());
    BundleBuildResponse response = new BundleBuildResponse("" + System.currentTimeMillis());
    assertEquals(0, response.getStatusList().size());
    
    _service.download(request, response);
    assertNotNull(response.getGtfsList());
    assertEquals(1, response.getGtfsList().size());
    
    assertNotNull(response.getStifZipList());
    assertEquals(1, response.getStifZipList().size());
    
    assertNotNull(response.getStatusList());
    assertTrue(response.getStatusList().size() > 0);
    
    _service.prepare(request, response);

    assertFalse(response.isComplete());
    int rc = _service.build(request, response);
    if (response.getException() != null) {
      _log.error("Failed with exception=" + response.getException());
    }
    assertNull(response.getException());
    assertFalse(response.isComplete());
    assertEquals(0, rc);
    
    _service.upload(request, response);
    assertFalse(response.isComplete()); // set by BundleRequestService
    
  }
  


}
