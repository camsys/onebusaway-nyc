/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;

/**
 * Characterization test for how {@link FileInputTask} decodes non-ASCII text
 * in a replayed realtime message file.
 *
 * The task's insertion thread opens the file with {@code new FileReader(String)},
 * which uses the JVM default charset. Up to Java 17 that default comes from the
 * OS locale; from Java 18 on (JEP 400) it is always UTF-8. This test pins the
 * Java 11 baseline on a UTF-8 host: a UTF-8 message decodes to the correct
 * accented text by the time it reaches the inference service.
 */
@RunWith(MockitoJUnitRunner.class)
public class FileInputTaskEncodingTest {

  // Unicode escapes keep the expected value independent of the source file encoding.
  private static final String MANUFACTURER_DATA = "VFTP Caf\u00e9 \u00d1and\u00fa";

  private static final String MESSAGE = "{\"RealtimeEnvelope\": {\"UUID\": \"foo\",\"timeReceived\": 1234567,\"CcLocationReport\": {\"request-id\" : 528271,\"vehicle\": {\"vehicle-id\": 8140,\"agency-id\": 2008,\"agencydesignator\": \"MTA NYCT\"},\"status-info\": 0,\"time-reported\": \"2011-10-15T03:26:19.000-00:00\",\"latitude\": 40612060,\"longitude\": -74035771,\"direction\": {\"deg\": 128.77},\"speed\": 0,\"manufacturer-data\": \"" + MANUFACTURER_DATA + "\",\"operatorID\": {\"operator-id\": 0,\"designator\": \"\"},\"runID\": {\"run-id\": 0,\"designator\": \"\"},\"destSignCode\": 4631,\"routeID\": {\"route-id\": 0,\"route-designator\": \"\"},\"localCcLocationReport\": {\"NMEA\": {\"sentence\": [\"\",\"\"]}}}}}";

  @Mock
  private VehicleAssignmentService vehicleAssignmentService;

  @Mock
  private VehicleLocationInferenceService vehicleLocationInferenceService;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private FileInputTask task;

  @After
  public void tearDown() {
    if (task != null) {
      task.destroy();
    }
  }

  /**
   * Runs the real insertion thread over a one-record file and captures the
   * envelope it hands to the inference service. The thread swallows and logs
   * its own exceptions, so a read or parse failure shows up here as the
   * verify timing out.
   */
  @Test
  public void testUtf8MessageTextDecodesUnchanged() throws Exception {
    ArrayList<AgencyAndId> assigned = new ArrayList<AgencyAndId>();
    assigned.add(new AgencyAndId("MTA NYCT", "8140"));
    when(vehicleAssignmentService.getAssignedVehicleIdsForDepot("JG")).thenReturn(assigned);

    File dump = tmp.newFile("dump_raw.sql");
    Files.write(dump.toPath(), (MESSAGE + "\n").getBytes(StandardCharsets.UTF_8));

    FileInputServiceImpl inputService = new FileInputServiceImpl();
    inputService.setVehicleAssignmentService(vehicleAssignmentService);
    inputService.setup();

    task = new FileInputTask();
    task.setInputService(inputService);
    task.setVehicleLocationService(vehicleLocationInferenceService);
    task.setFilename(dump.getAbsolutePath());
    // execute() copies this key onto the input service, which filters by depot.
    task.setDepotPartitionKey("JG");
    task.execute();

    ArgumentCaptor<RealtimeEnvelope> captor = ArgumentCaptor.forClass(RealtimeEnvelope.class);
    verify(vehicleLocationInferenceService, timeout(10000)).handleRealtimeEnvelopeRecord(captor.capture());

    assertEquals("default charset=" + Charset.defaultCharset(), MANUFACTURER_DATA,
        captor.getValue().getCcLocationReport().getManufacturerData());
  }
}
