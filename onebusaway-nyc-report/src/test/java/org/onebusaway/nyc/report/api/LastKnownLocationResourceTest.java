/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
