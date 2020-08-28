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

package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleLocationInferenceServiceImpl.ProcessingTask;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

@RunWith(MockitoJUnitRunner.class)
public class ProcessingTaskTest {

  private static final String DEPOT = "GH";

  @Mock
  ApplicationContext appContext;
  @Mock
  VehicleAssignmentService _vehicleAssignmentService;
  @Mock
  OutputQueueSenderService _outputQueueSenderService;

  @InjectMocks
  VehicleLocationInferenceServiceImpl service = new VehicleLocationInferenceServiceImpl();

  @Test
  public void testRun() throws Exception {

    final VehicleInferenceInstance vehInfInst = mock(VehicleInferenceInstance.class);
    when(
        vehInfInst.handleBypassUpdate(any(NycTestInferredLocationRecord.class))).thenReturn(
        true);
    final NycQueuedInferredLocationBean nqlb = mock(NycQueuedInferredLocationBean.class);
    when(vehInfInst.getCurrentStateAsNycQueuedInferredLocationBean()).thenReturn(
        nqlb);

    final NycVehicleManagementStatusBean mgmtStatus = mock(NycVehicleManagementStatusBean.class);

    when(vehInfInst.getCurrentManagementState()).thenReturn(mgmtStatus);
    when(appContext.getBean(VehicleInferenceInstance.class)).thenReturn(
        vehInfInst);

    final NycTestInferredLocationRecord infLocationRec = mock(NycTestInferredLocationRecord.class);
    final AgencyAndId vehicleId = mock(AgencyAndId.class);
    when(infLocationRec.getVehicleId()).thenReturn(vehicleId);

    when(_vehicleAssignmentService.getAssignedDepotForVehicleId(vehicleId)).thenReturn(
        DEPOT);

    service.setApplicationContext(appContext);

    final ProcessingTask task = service.new ProcessingTask(vehInfInst, infLocationRec, false, false);
    task.run();

    // verify(mgmtStatus).setDepotId(DEPOT);

  }

}
