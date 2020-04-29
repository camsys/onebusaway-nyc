package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.junit.Ignore;
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

  private String oddInternalDelimeter = "_&_&_";

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
        } else if (directory.equals("extensiveTest/gtfs_latest")) {
          list.add("GTFS_MTABC.zip");
          list.add("google_transit_mta_agency.zip");
          list.add("google_transit_queens.zip");
          list.add("google_transit_staten_island.zip");
          list.add("google_transit_bronx.zip");
          list.add("google_transit_brooklyn.zip");
          list.add("google_transit_manhattan.zip");
        } else if (directory.equals("extensiveTest/stif_latest")) {
          list.add("STIF_MTABC_A0_AsAssigned_Rte_Included_Hol_Included-v1.zip");
          list.add("STIF_SURFACE_BX_2020-01-19_REV2019-11-25_1032003.zip");
          list.add("STIF_SURFACE_B_2020-01-19_REV2019-12-17_0832430.zip");
          list.add("STIF_SURFACE_M_2020-01-31_REV2020-02-05_1050273.zip");
          list.add("STIF_SURFACE_Q_2020-01-19_REV2020-01-14_1130185-v1.zip");
          list.add("STIF_SURFACE_S_2020-01-19_REV2020-02-03_0943449.zip");
        } else if (directory.equals("extensiveTest/config")) {
          // do nothing
        } else if (directory.equals("semiExtensiveTest/gtfs_latest")) {
          list.add("GTFS_MTABC.zip");
          list.add("google_transit_mta_agency.zip");
          list.add("google_transit_bronx.zip");
        } else if (directory.equals("semiExtensiveTest/stif_latest")) {
          list.add("STIF_MTABC_A0_AsAssigned_Rte_Included_Hol_Included-v1.zip");
          list.add("STIF_SURFACE_BX_2020-01-19_REV2019-11-25_1032003.zip");
        } else if (directory.equals("semiExtensiveTest/config")) {
          // do nothing
        } else if (directory.split(oddInternalDelimeter).length > 1){
          String[] directoryParts = directory.split(oddInternalDelimeter);
          String gtfsFile = directoryParts[1];
          String stifFile = directoryParts[2];
          String requestType = directoryParts[3].split("/")[1];
          if (requestType.equals("gtfs_latest")){
            list.add(gtfsFile);
          } else if (requestType.equals("stif_latest")){
            list.add(stifFile);
          }
        }
        else{
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
        } else {
          source = this.getClass().getResourceAsStream(key);
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
  public void testBuild(){
    testBuild(1);
  }

  @Ignore
  @Test
  public void extensiveTestBuild(){
    testBuild(6);
  }

  @Ignore
  @Test
  public void semiExtensiveTestBuild(){
    testBuild(2);
  }

  @Ignore
  @Test
  public void transformationTestBuild(){

  }

  public void testBuild(int mode) {
    String bundleDir;
    switch(mode)
    {
      case 1:
        bundleDir = "test";
        break;
      case 6:
        bundleDir = "extensiveTest";
        break;
      default:
        bundleDir = "semiExtensiveTest";
    }

    String tmpDir = new FileUtils().createTmpDirectory();

    BundleBuildRequest request = new BundleBuildRequest();
    request.setBundleDirectory(bundleDir);
    request.setBundleName("testname");
    request.setTmpDirectory(tmpDir);
    String startDate = (mode == 1) ? "2012-04-08" : "2020-01-08";
    request.setBundleStartDate(startDate);
    String endDate = (mode == 1) ? "2012-07-07" : "2020-03-07";
    request.setBundleEndDate(endDate);
    assertNotNull(request.getTmpDirectory());
    assertNotNull(request.getBundleDirectory());
    BundleBuildResponse response = new BundleBuildResponse(""
        + System.currentTimeMillis());
    assertEquals(0, response.getStatusList().size());


    // step 1
    _service.download(request, response);
    assertNotNull(response.getGtfsList());
    int expected = (mode == 1) ? mode:mode+1;
    assertEquals(expected, response.getGtfsList().size());
    expected = mode;
    assertNotNull(response.getStifZipList());
    assertEquals(expected, response.getStifZipList().size());

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

  @Ignore
  @Test
  public void configureMeTest(){
    testingGtfsLocally("2020-01-19", "2020-05-02","GTFS_MTABC_tmp.zip","STIF_MTABC_04202020_ECCPFRJKSC-v1.zip");
  }

  public void testingGtfsLocally(String startDate, String endDate, String gtfsFile, String stifFile){

    String bundleDir;

    bundleDir = "testingGtfsLocally" + oddInternalDelimeter + gtfsFile + oddInternalDelimeter + stifFile + oddInternalDelimeter + "testingGtfsLocally";


    String tmpDir = new FileUtils().createTmpDirectory();

    BundleBuildRequest request = new BundleBuildRequest();
    request.setBundleDirectory(bundleDir);
    request.setBundleName("testname");
    request.setTmpDirectory(tmpDir);
    request.setBundleStartDate(startDate);
    request.setBundleEndDate(endDate);
    BundleBuildResponse response = new BundleBuildResponse(""
            + System.currentTimeMillis());


    // step 1
    _service.download(request, response);

    // step 2
    _service.prepare(request, response);



    // step 3
    int rc = _service.build(request, response);
    if (response.getException() != null) {
      _log.error("Failed with exception=" + response.getException());
    }

    // step 4
    // OBANYC-1451 -- fails on OSX TODO
    //_service.assemble(request, response);

    // step 5
    _service.upload(request, response);

  }

}
