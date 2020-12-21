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

package org.onebusaway.nyc.transit_data_federation.impl.vtw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.impl.util.TcipUtilImpl;
import org.onebusaway.nyc.util.impl.vtw.PullOutApiLibrary;

import tcip_final_4_0_0.CPTTransitFacilityIden;
import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHBlockIden;
import tcip_final_4_0_0.SCHPullInOutInfo;
import tcip_final_4_0_0.SchPullOutList.PullOuts;


@RunWith(MockitoJUnitRunner.class)
public class VehiclePulloutServiceImplTest {

  @Mock
  private PullOutApiLibrary mockApiLibrary;

  @InjectMocks
  private VehiclePulloutServiceImpl service;

  @InjectMocks
  private TcipUtilImpl tcipUtil;

  @Before
  public void prepare(){
    MockitoAnnotations.initMocks(this);
    tcipUtil.setup();
    service.setTcipUtil(tcipUtil);
    service.setEnabled(true);
  }

  @Test
  public void testVehiclePulloutEmptyList() throws Exception {
    ObaSchPullOutList o = new ObaSchPullOutList();
    o.setErrorCode("1");
    o.setErrorDescription("No description here");

    String xml = tcipUtil.getAsXml(o);
    ObaSchPullOutList o2 = tcipUtil.getFromXml(xml);
    service.refreshData(o2);

    AgencyAndId vehicle = new AgencyAndId("MTA", "7788");
    SCHPullInOutInfo pullouts = service.getVehiclePullout(vehicle);
    assertNull(pullouts);
  }

  @Test
  public void testVehiclePulloutNonEmptyList() throws Exception {

    ObaSchPullOutList o = new ObaSchPullOutList();
    
    PullOuts pullouts = new PullOuts();
    
    o.setPullOuts(pullouts);
    
    List<SCHPullInOutInfo> list = pullouts.getPullOut();
    
    SCHPullInOutInfo pullinoutinfo = new SCHPullInOutInfo();
    
    list.add(pullinoutinfo);
    
    CPTVehicleIden vehicle = new CPTVehicleIden();
    pullinoutinfo.setVehicle(vehicle);
    vehicle.setAgdesig("MTA");
    vehicle.setId("7788");

    CPTTransitFacilityIden garage = new CPTTransitFacilityIden();
    pullinoutinfo.setGarage(garage);
    garage.setAgdesig("MTA");
    
    SCHBlockIden block = new SCHBlockIden();
    pullinoutinfo.setBlock(block);
    block.setId("test block id");
    
    String xml = tcipUtil.getAsXml(o);
    ObaSchPullOutList o2 = tcipUtil.getFromXml(xml);
    service.refreshData(o2);
    
    AgencyAndId lookupVehicle = new AgencyAndId("MTA", "7788");
    SCHPullInOutInfo resultPullouts = service.getVehiclePullout(lookupVehicle);
    assertNotNull(resultPullouts);
    assertEquals("MTA", resultPullouts.getVehicle().getAgdesig());
    assertEquals("7788", resultPullouts.getVehicle().getId());
    assertEquals("test block id", resultPullouts.getBlock().getId());
  }

  @Test
  public void testVehiclePulloutFromXml() throws Exception {
    Path path = Paths.get(getClass().getResource("vehicle_pipo.xml").getFile());
    String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    ObaSchPullOutList pullOutList = tcipUtil.getFromXml(xml);

    service.processVehiclePipoList(pullOutList);
    assertEquals(1,service.getVehicleIdToPullouts().size());

    List<SCHPullInOutInfo> pullInOutInfoList =  new ArrayList<>(service.getVehicleIdToPullouts().values());
    SCHPullInOutInfo pipoInfo = pullInOutInfoList.get(0);
    assertEquals(pipoInfo.getBlock().getId(), "MTABC_BPPC0-BP_C0-Weekday-10_5440668");
    assertEquals(pipoInfo.getGarage().getAgdesig(), "MTABC");
    assertEquals(pipoInfo.getVehicle().getId(), "3773");
    assertEquals(pipoInfo.getRun().getId(), "417");

    AgencyAndId vehicleId = new AgencyAndId("MTABC","3773");
    assertEquals("MTABC_BPPC0-BP_C0-Weekday-10_5440668",service.getAssignedBlockId(vehicleId));
  }

  @Test
  public void testVehiclePulloutFromEmptyXml() throws Exception {
    Path path = Paths.get(getClass().getResource("vehicle_pipo_empty.xml").getFile());
    String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    ObaSchPullOutList pullOutList = tcipUtil.getFromXml(xml);
    assertEquals("0", pullOutList.getErrorCode());
    assertEquals("Yard Trek", pullOutList.getSourceapp());
    assertEquals("TCIP 4.0", pullOutList.getSchVersion());

    service.processVehiclePipoList(pullOutList);
    assertEquals(0, service.getVehicleIdToPullouts().size());
  }

}
