package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;

import tcip_final_4_0_0.CPTVehicleIden;
import tcip_final_4_0_0.ObaSchPullOutList;
import tcip_final_4_0_0.SCHBlockIden;
import tcip_final_4_0_0.SCHPullInOutInfo;
import tcip_final_4_0_0.SchPullOutList.PullOuts;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(MockitoJUnitRunner.class)
public class VehiclePulloutServiceImplTest {

  @Mock
  private TransitDataManagerApiLibrary mockApiLibrary;

  @InjectMocks
  private VehiclePulloutServiceImpl service;

  @Test
  public void testVehiclePulloutEmptyList() throws Exception {
    List<JsonObject> value = new ArrayList<JsonObject>();

    ObaSchPullOutList o = new ObaSchPullOutList();
    o.setErrorCode("1");
    o.setErrorDescription("No description here");

    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper m = new ObjectMapper();
    m.registerModule(module);
    m.setSerializationInclusion(Include.NON_NULL);
    
    String s = m.writeValueAsString(o);

    JsonElement json = new JsonParser().parse(s);
    value.add(json.getAsJsonObject());

    when(mockApiLibrary.getItemsForRequest("pullouts", "realtime", "list")).thenReturn(value);
    
    service.refreshData();

    AgencyAndId vehicle = new AgencyAndId("MTA", "7788");
    SCHPullInOutInfo pullouts = service.getVehiclePullout(vehicle);
    assertNull(pullouts);
  }

  @Test
  public void testVehiclePulloutNonEmptyList() throws Exception {
    List<JsonObject> value = new ArrayList<JsonObject>();

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
    SCHBlockIden block = new SCHBlockIden();
    pullinoutinfo.setBlock(block);
    block.setId("test block id");
    
    JaxbAnnotationModule module = new JaxbAnnotationModule();
    ObjectMapper m = new ObjectMapper();
    m.registerModule(module);
    m.setSerializationInclusion(Include.NON_NULL);
    
    String s = m.writeValueAsString(o);

    JsonElement json = new JsonParser().parse(s);
    value.add(json.getAsJsonObject());

    when(mockApiLibrary.getItemsForRequestNoCheck("pullouts", "realtime", "list")).thenReturn(value);
    
    service.refreshData();
    
    AgencyAndId lookupVehicle = new AgencyAndId("MTA", "7788");
    SCHPullInOutInfo resultPullouts = service.getVehiclePullout(lookupVehicle);
    assertNotNull(resultPullouts);
    assertEquals("MTA", resultPullouts.getVehicle().getAgdesig());
    assertEquals("7788", resultPullouts.getVehicle().getId());
    assertEquals("test block id", resultPullouts.getBlock().getId());
  }

}
