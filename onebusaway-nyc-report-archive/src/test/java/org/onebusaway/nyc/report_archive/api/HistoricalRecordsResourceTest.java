package org.onebusaway.nyc.report_archive.api;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonPartEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.report.api.json.LowerCaseWDashesGsonJsonTool;
import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;
import org.onebusaway.nyc.report_archive.services.HistoricalRecordsDao;

@RunWith(MockitoJUnitRunner.class)
public class HistoricalRecordsResourceTest {

  @Test
  public void testGetHistoricalRecords() {
    HistoricalRecordsResource r = new HistoricalRecordsResource();
    // this is normally autowired.  See application-context.xml.
    r.setJsonTool(new LowerCaseWDashesGsonJsonTool(true));
    
    HistoricalRecordsDao dao = mock(HistoricalRecordsDao.class);
    List<HistoricalRecord> results = new ArrayList<HistoricalRecord>();
    HistoricalRecord historicalRecord = new HistoricalRecord();
    historicalRecord.setAssignedBlockId("assigned block id");
    results.add(historicalRecord);
    when(dao.getHistoricalRecords(Matchers.<Map<CcAndInferredLocationFilter, Object>>any())).thenReturn(results);

    r.setHistoricalRecordsDao(dao);

    String depotId = null;
    String inferredRouteId = null;
    String inferredPhase = null;
    Integer vehicleId = null;
    String vehicleAgencyId = null;
    String boundingBox = null;
    String startDate = null;
    String endDate = null;
    Integer records = null;
    Integer timeout = null;
    Response response = r.getHistoricalRecords(depotId, inferredRouteId, inferredPhase, vehicleId, vehicleAgencyId, boundingBox, startDate, endDate, records, timeout);

    assertEquals(200, response.getStatus());
    
    Object entity = response.getEntity();
    
    assertJsonPartEquals("assigned block id", entity.toString(),"records[0].assigned-block-id");  
  }

}
