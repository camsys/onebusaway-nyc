package org.onebusaway.nyc.transit_data_manager.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutService;
import org.onebusaway.nyc.transit_data_manager.api.vehiclepipo.service.VehiclePullInOutServiceImpl;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.SCHPullInOutInfo;

/**
 * Tests {@link VehiclePullInOutServiceImpl}
 * @author abelsare
 *
 */
public class VehiclePullInOutServiceImplTest {

	private VehiclePullInOutService service;
	
	@Before
	public void setUp() throws Exception {
		service = new VehiclePullInOutServiceImpl();
	}
	
	@Test
	public void testGetActivePullouts() {
		List<VehiclePullInOutInfo> pulloutInfo = new ArrayList<VehiclePullInOutInfo>();
		
		VehiclePullInOutInfo activePullout1 = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo activePullout2 = mock(VehiclePullInOutInfo.class);
		
		SCHPullInOutInfo activePullout1Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullout2Info = mock(SCHPullInOutInfo.class);
		
		CPTVehicleIden vehicle1 = mock(CPTVehicleIden.class);
		CPTVehicleIden vehicle2 = mock(CPTVehicleIden.class);
		
		when(activePullout1.getPullOutInfo()).thenReturn(activePullout1Info);
		when(activePullout2.getPullOutInfo()).thenReturn(activePullout2Info);
		
		when(activePullout1Info.getTime()).thenReturn(getPullInOutTime(-1));
		when(activePullout2Info.getTime()).thenReturn(getPullInOutTime(-1));
		
		when(activePullout1Info.getVehicle()).thenReturn(vehicle1);
		when(activePullout2Info.getVehicle()).thenReturn(vehicle2);
		when(vehicle1.getVehicleId()).thenReturn(1L);
		when(vehicle2.getVehicleId()).thenReturn(2L);
		
		pulloutInfo.add(activePullout1);
		pulloutInfo.add(activePullout2);
		
		List<VehiclePullInOutInfo> activePullouts = service.getActivePullOuts(pulloutInfo, false);
		
		assertEquals("Expecting 2 active pull outs", 2, activePullouts.size());
	}
	
	@Test
	public void testMultipleActivePullouts() {
		List<VehiclePullInOutInfo> pulloutInfo = new ArrayList<VehiclePullInOutInfo>();
		
		VehiclePullInOutInfo activePullout1 = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo activePullout2 = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo activePullout3 = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo inactivePullout = mock(VehiclePullInOutInfo.class);
		
		SCHPullInOutInfo activePullout1Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullout2Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullout3Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo inactivePulloutInfo = mock(SCHPullInOutInfo.class);
		
		CPTVehicleIden vehicle1 = mock(CPTVehicleIden.class);
		CPTVehicleIden vehicle2 = mock(CPTVehicleIden.class);
		
		when(activePullout1.getPullOutInfo()).thenReturn(activePullout1Info);
		when(activePullout2.getPullOutInfo()).thenReturn(activePullout2Info);
		when(activePullout3.getPullOutInfo()).thenReturn(activePullout3Info);
		when(inactivePullout.getPullOutInfo()).thenReturn(inactivePulloutInfo);
		
		when(activePullout1Info.getTime()).thenReturn(getPullInOutTime(-1));
		when(inactivePulloutInfo.getTime()).thenReturn(getPullInOutTime(-5));
		when(activePullout2Info.getTime()).thenReturn(getPullInOutTime(-2));
		when(activePullout3Info.getTime()).thenReturn(getPullInOutTime(-3));
		
		when(activePullout1Info.getVehicle()).thenReturn(vehicle1);
		when(inactivePulloutInfo.getVehicle()).thenReturn(vehicle1);
		when(activePullout2Info.getVehicle()).thenReturn(vehicle2);
		when(activePullout3Info.getVehicle()).thenReturn(vehicle2);
		
		when(vehicle1.getVehicleId()).thenReturn(1L);
		when(vehicle2.getVehicleId()).thenReturn(2L);
		
		pulloutInfo.add(activePullout1);
		pulloutInfo.add(activePullout2);
		pulloutInfo.add(inactivePullout);
		
		List<VehiclePullInOutInfo> activePullouts = service.getActivePullOuts(pulloutInfo, false);
		
		assertEquals("Expecting 2 active pull outs", 2, activePullouts.size());
	}
	
	@Test
	public void testMostRecentActivePullout() {
		List<VehiclePullInOutInfo> pulloutInfo = new ArrayList<VehiclePullInOutInfo>();
		
		VehiclePullInOutInfo activePullout1 = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo activePullout2 = mock(VehiclePullInOutInfo.class);
		SCHPullInOutInfo activePullout1Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullin1Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullout2Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullin2Info = mock(SCHPullInOutInfo.class);
		
		when(activePullout1.getPullOutInfo()).thenReturn(activePullout1Info);
		when(activePullout1.getPullInInfo()).thenReturn(activePullin1Info);
		when(activePullout2.getPullOutInfo()).thenReturn(activePullout2Info);
		when(activePullout2.getPullInInfo()).thenReturn(activePullin2Info);
		
		pulloutInfo.add(activePullout1);
		pulloutInfo.add(activePullout2);
		
		when(activePullout1Info.getTime()).thenReturn(getPullInOutTime(-1));
		when(activePullin1Info.getTime()).thenReturn(getPullInOutTime(8));
		when(activePullout2Info.getTime()).thenReturn(getPullInOutTime(-2));
		when(activePullin2Info.getTime()).thenReturn(getPullInOutTime(8));
		
		VehiclePullInOutInfo activePullout = service.getMostRecentActivePullout(pulloutInfo);
		assertTrue("Expecting first active pull out to be most recent",activePullout.equals(activePullout1));
		assertFalse("Not expecting second active pull out to be most recent",activePullout.equals(activePullout2));
	}
	
	@Test
	public void testMostRecentNoPullouts() {
		List<VehiclePullInOutInfo> pulloutInfo = new ArrayList<VehiclePullInOutInfo>();
		VehiclePullInOutInfo activePullout = service.getMostRecentActivePullout(pulloutInfo);
		assertNull("Expecting null result", activePullout);
	}
	
	@Test
	public void testGetAllActivePullouts() {
		List<VehiclePullInOutInfo> pulloutInfo = new ArrayList<VehiclePullInOutInfo>();
		
		VehiclePullInOutInfo activePullout1 = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo activePullout2 = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo activePullout3 = mock(VehiclePullInOutInfo.class);
		
		SCHPullInOutInfo activePullout1Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullout2Info = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullout3Info = mock(SCHPullInOutInfo.class);
		
		CPTVehicleIden vehicle1 = mock(CPTVehicleIden.class);
		CPTVehicleIden vehicle2 = mock(CPTVehicleIden.class);
		
		when(activePullout1.getPullOutInfo()).thenReturn(activePullout1Info);
		when(activePullout2.getPullOutInfo()).thenReturn(activePullout2Info);
		when(activePullout3.getPullOutInfo()).thenReturn(activePullout3Info);
		
		when(activePullout1Info.getTime()).thenReturn(getPullInOutTime(-1));
		when(activePullout3Info.getTime()).thenReturn(getPullInOutTime(-5));
		when(activePullout2Info.getTime()).thenReturn(getPullInOutTime(-2));
		
		when(activePullout1Info.getVehicle()).thenReturn(vehicle1);
		when(activePullout3Info.getVehicle()).thenReturn(vehicle1);
		when(activePullout2Info.getVehicle()).thenReturn(vehicle2);
		
		when(vehicle1.getVehicleId()).thenReturn(1L);
		when(vehicle2.getVehicleId()).thenReturn(2L);
		
		pulloutInfo.add(activePullout1);
		pulloutInfo.add(activePullout2);
		pulloutInfo.add(activePullout3);
		
		List<VehiclePullInOutInfo> activePullouts = service.getActivePullOuts(pulloutInfo, true);
		
		assertEquals("Expecting 3 active pull outs", 3, activePullouts.size());
	}
	
	private String getPullInOutTime(int offset) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, offset);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");
		return formatter.format(calendar.getTime());
	}

}
