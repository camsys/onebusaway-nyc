package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.*;

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
    
    VehicleInferenceInstance vehInfInst = mock(VehicleInferenceInstance.class);
    when(vehInfInst.handleBypassUpdate(any(NycTestInferredLocationRecord.class))).thenReturn(true);
    NycQueuedInferredLocationBean nqlb = mock(NycQueuedInferredLocationBean.class);
    when(vehInfInst.getCurrentStateAsNycQueuedInferredLocationBean()).thenReturn(nqlb);

    NycVehicleManagementStatusBean mgmtStatus = mock(NycVehicleManagementStatusBean.class);

    when(vehInfInst.getCurrentManagementState()).thenReturn(mgmtStatus);
    when(appContext.getBean(VehicleInferenceInstance.class)).thenReturn(vehInfInst);

    NycTestInferredLocationRecord infLocationRec = mock(NycTestInferredLocationRecord.class);
    AgencyAndId vehicleId = mock(AgencyAndId.class);
    when(infLocationRec.getVehicleId()).thenReturn(vehicleId);

    when(_vehicleAssignmentService.getAssignedDepotForVehicle(vehicleId)).thenReturn(DEPOT);

    service.setApplicationContext(appContext);

    ProcessingTask task = service.new ProcessingTask(infLocationRec);
    task.run();

    verify(mgmtStatus).setDepotId(DEPOT);

  }

}
