package org.onebusaway.nyc.transit_data_manager.api;

import static org.junit.Assert.*;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UTSUtil;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.OperatorAssignmentFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;


import org.joda.time.DateMidnight;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class CrewResourceTest extends ResourceTest {

  private static Logger _log = LoggerFactory.getLogger(CrewResourceTest.class);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testOperatorAssignmentComparator() {
    OperatorAssignment oa1 = new OperatorAssignment();
    oa1.setUpdated("2012-07-29T20:51:24-00:00");
    OperatorAssignment oa2 = new OperatorAssignment();
    oa2.setUpdated("2012-07-29T20:51:24-00:00");
    assertEquals(0, oa1.compareTo(oa2));
    assertEquals(0, oa2.compareTo(oa1));
    OperatorAssignment oa3 = new OperatorAssignment();
    oa3.setUpdated("2012-07-29T20:51:25-00:00");
    assertEquals(1, oa3.compareTo(oa2));
    
  }
  
  @Test
  public void testPaddedRunIds() throws Exception {
    File tmpInFile = getCISFile("CIS_20120727.txt");
    
    UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(tmpInFile);
    // no need to set depotIdTranslater
    OperatorAssignmentData data = process.generateDataObject();
    assertNotNull(data);
    DateMidnight serviceDate = new DateMidnight("2012-07-27");
    List<SCHOperatorAssignment> list = data.getOperatorAssignmentsByServiceDate(serviceDate);
    assertNotNull(list);
    int i = 0;
    for (SCHOperatorAssignment assignment: list) {
      i++;
      String runId = assignment.getRun().getDesignator();
      if (runId.contains("-0")) {
        _log.error("found runId=" + runId + " for assignment=" + assignment.toString());
      } else {
          //_log.info("found runId=" + runId + " for assignment=" + assignment.toString());
      }
      assertFalse(runId.contains("-0"));
    }
    assertTrue(i>0);
    _log.info("processed " + i + " assignment records");
  }

  @Test
  public void testDuplicateAssignments() throws Exception {
    HashMap<String, OperatorAssignment> passMap = testDuplicateAssignments("CIS_20120727.txt", "2012-07-27");
    
    assertNotNull(passMap);
    assertTrue(!passMap.isEmpty());
    OperatorAssignment test1 = passMap.get("706005");
    assertNotNull(test1);
    assertEquals("455", test1.getRunNumber());

    passMap = testDuplicateAssignments("CIS_20120730_1602.txt", "2012-07-30");
    OperatorAssignment test2 = passMap.get("387009");
    assertNotNull(test2);
    assertEquals("Q4420", test2.getRunRoute()); // duplicate CAST should be excluded
  }
  
  
  @Test
  public void testNumericPassNumbers() throws Exception {

    File tmpInFile = getCISFile("CIS_20120816_1202.txt");
    UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(tmpInFile);
    // no need to set depotIdTranslater
    OperatorAssignmentData data = process.generateDataObject();
    assertNotNull(data);
    DateMidnight serviceDate = new DateMidnight("2012-08-16");
    List<SCHOperatorAssignment> list = data.getOperatorAssignmentsByServiceDate(serviceDate);
    assertNotNull(list);
    ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> tcipToJsonConverter = new OperatorAssignmentFromTcip();    
    List<OperatorAssignment> jsonOpAssigns = new UTSUtil().listConvertOpAssignTcipToJson(tcipToJsonConverter,
        data.getOperatorAssignmentsByServiceDate(serviceDate));
    assertNotNull(jsonOpAssigns);
    
    boolean found = false;
    for (OperatorAssignment oa : jsonOpAssigns) {
      String passNumber = oa.getPassId();
      
      if (passNumber.matches("^[A-Z].*")) {
        _log.error("found non-numeric passNumber=" + passNumber);
        found = true;
      }
    }
    assertFalse(found);

    for (OperatorAssignment oa : jsonOpAssigns) {
      String route = oa.getRunRoute();
      
      if (route.matches("^[A-Z][a-z].*")) {
        _log.error("found lowercase route=" + route);
        found = true;
      }
    }
    assertFalse(found);
  }
  
  public HashMap<String, OperatorAssignment> testDuplicateAssignments(String filename, String serviceDateStr) throws Exception {
    File tmpInFile = getCISFile(filename);
    UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(tmpInFile);
    // no need to set depotIdTranslater
    OperatorAssignmentData data = process.generateDataObject();
    assertNotNull(data);
    DateMidnight serviceDate = new DateMidnight(serviceDateStr);
    List<SCHOperatorAssignment> list = data.getOperatorAssignmentsByServiceDate(serviceDate);
    assertNotNull(list);

    ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> tcipToJsonConverter = new OperatorAssignmentFromTcip();
    
    List<OperatorAssignment> jsonOpAssigns = new UTSUtil().listConvertOpAssignTcipToJson(tcipToJsonConverter,
        data.getOperatorAssignmentsByServiceDate(serviceDate));
    HashMap<String, Integer> passCounts = new HashMap<String, Integer>();
    HashMap<String, OperatorAssignment> passMap = new HashMap<String, OperatorAssignment>();
    int i = 0;
    for (OperatorAssignment oa : jsonOpAssigns) {
      i++;
      String key = oa.getPassId();
      if (passCounts.containsKey(key)) {
        Integer val = passCounts.get(key);
        passCounts.put(key, val + 1);
        _log.error("found dup for pass=" + key + ", val=" + (val + 1));
        passMap.put(key, oa);
      } else {
        passCounts.put(key, 1);
        passMap.put(key, oa);
      }
    }
    // make sure we found some data!
    assertTrue(i>0);
    
    boolean foundDuplicate = false;
    for (String key : passCounts.keySet()) {
      if (passCounts.get(key) != 1) {
        _log.error("key=" + key + " has count= " + passCounts.get(key));
        foundDuplicate = true;
      }

    }
    assertFalse(foundDuplicate);
    return passMap;
  }

  private File getCISFile(String filename) throws Exception {
    File tmpInFile = File.createTempFile("tmp", ".tmp");
    tmpInFile.deleteOnExit();
    
    InputStream resource = this.getClass().getResourceAsStream(filename);
    assertNotNull(resource);
    copy(resource, tmpInFile.getCanonicalPath());
    return tmpInFile;
  }
  
}
