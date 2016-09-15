package org.onebusaway.nyc.transit_data_federation.impl.vtw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.impl.vtw.VehiclePulloutServiceImpl;
import org.onebusaway.nyc.util.impl.vtw.PullOutApiLibrary;

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
  
  @Before
  public void prepare(){
	  MockitoAnnotations.initMocks(this);
	  this.service.setup();
  }

  @Test
  public void testVehiclePulloutEmptyList() throws Exception {
    ObaSchPullOutList o = new ObaSchPullOutList();
    o.setErrorCode("1");
    o.setErrorDescription("No description here");

    String xml = service.getAsXml(o);
    
    when(mockApiLibrary.getContentsOfUrlAsString("uts","active","tcip")).thenReturn(xml);
        
    service.refreshData();

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
    
    SCHBlockIden block = new SCHBlockIden();
    pullinoutinfo.setBlock(block);
    block.setId("test block id");
    
    String xml = service.getAsXml(o);

    when(mockApiLibrary.getContentsOfUrlAsString("uts","active","tcip")).thenReturn(xml);
    
    service.refreshData();
    
    AgencyAndId lookupVehicle = new AgencyAndId("MTA", "7788");
    SCHPullInOutInfo resultPullouts = service.getVehiclePullout(lookupVehicle);
    assertNotNull(resultPullouts);
    assertEquals("MTA", resultPullouts.getVehicle().getAgdesig());
    assertEquals("7788", resultPullouts.getVehicle().getId());
    assertEquals("test block id", resultPullouts.getBlock().getId());
  }

}
