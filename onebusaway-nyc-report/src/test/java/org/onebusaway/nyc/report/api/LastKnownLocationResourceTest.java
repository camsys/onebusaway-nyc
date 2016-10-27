package org.onebusaway.nyc.report.api;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonPartEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.report.api.json.LowerCaseWDashesGsonJsonTool;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report.services.CcAndInferredLocationDao;

@RunWith(MockitoJUnitRunner.class)
public class LastKnownLocationResourceTest {

  @Test
  public void testGetlastLocationRecordForVehicle() throws Exception {
    LastKnownLocationResource r = new LastKnownLocationResource();
    CcAndInferredLocationDao dao = mock(CcAndInferredLocationDao.class);
    CcAndInferredLocationRecord ccAndInf = new CcAndInferredLocationRecord();
    ccAndInf.setAssignedBlockId("assigned block id");
    
    when(dao.getLastKnownRecordForVehicle(any(Integer.class))).thenReturn(ccAndInf);

    r.set_locationDao(dao);
    r.set_jsonTool(new LowerCaseWDashesGsonJsonTool(true));

    Response response = r.getlastLocationRecordForVehicle(123);
    assertEquals(200, response.getStatus());

    Object entity = response.getEntity();
    
    assertJsonPartEquals("assigned block id", entity.toString(),"records[0].assigned-block-id");  

  }

}
