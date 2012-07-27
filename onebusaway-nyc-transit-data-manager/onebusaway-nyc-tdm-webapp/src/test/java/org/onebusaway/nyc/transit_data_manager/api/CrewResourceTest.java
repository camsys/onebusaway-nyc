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
  public void testPaddedRunIds() throws Exception {
    File tmpInFile = getCISFile();
    
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
    _log.error("processed " + i + " assignment records");
  }


  @Test
  public void testDuplicateAssignments() throws Exception {
    File tmpInFile = getCISFile();
    UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(tmpInFile);
    // no need to set depotIdTranslater
    OperatorAssignmentData data = process.generateDataObject();
    assertNotNull(data);
    DateMidnight serviceDate = new DateMidnight("2012-07-27");
    List<SCHOperatorAssignment> list = data.getOperatorAssignmentsByServiceDate(serviceDate);
    assertNotNull(list);

    ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> tcipToJsonConverter = new OperatorAssignmentFromTcip();
    
    List<OperatorAssignment> jsonOpAssigns = new UTSUtil().listConvertOpAssignTcipToJson(tcipToJsonConverter,
        data.getOperatorAssignmentsByServiceDate(serviceDate));
    HashMap<String, Integer> passCounts = new HashMap<String, Integer>();
    HashMap<String, OperatorAssignment> passMap = new HashMap<String, OperatorAssignment>();
    for (OperatorAssignment oa : jsonOpAssigns) {

      String key = oa.getPassId();
      if (passCounts.containsKey(key)) {

        Integer val = passCounts.get(key);
        passCounts.put(key, val + 1);
        passMap.remove(key);
        passMap.put(key, oa);
      } else {
        passCounts.put(key, 1);
        passMap.put(key, oa);
      }
    }
    
    boolean foundDuplicate = false;
    for (String key : passCounts.keySet()) {
      if (passCounts.get(key) != 1) {
        _log.error("key=" + key + " has count= " + passCounts.get(key));
        foundDuplicate = true;
      }

    }
    assertFalse(foundDuplicate);
    OperatorAssignment test1 = passMap.get("706005");
    assertNotNull(test1);
    assertEquals("455", test1.getRunNumber());
  }

  private File getCISFile() throws Exception {
    File tmpInFile = File.createTempFile("tmp", ".tmp");
    tmpInFile.deleteOnExit();
    
    InputStream resource = this.getClass().getResourceAsStream("CIS.txt");
    assertNotNull(resource);
    copy(resource, tmpInFile.getCanonicalPath());
    return tmpInFile;
  }

}
