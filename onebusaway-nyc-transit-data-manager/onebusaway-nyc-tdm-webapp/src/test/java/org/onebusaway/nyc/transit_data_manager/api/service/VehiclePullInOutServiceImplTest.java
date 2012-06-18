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
		
		VehiclePullInOutInfo activePullout = mock(VehiclePullInOutInfo.class);
		VehiclePullInOutInfo inactivePullout = mock(VehiclePullInOutInfo.class);
		SCHPullInOutInfo activePulloutInfo = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo activePullinInfo = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo inactivePulloutInfo = mock(SCHPullInOutInfo.class);
		SCHPullInOutInfo inactivePullinInfo = mock(SCHPullInOutInfo.class);
		
		when(activePullout.getPullOutInfo()).thenReturn(activePulloutInfo);
		when(activePullout.getPullInInfo()).thenReturn(activePullinInfo);
		when(inactivePullout.getPullOutInfo()).thenReturn(inactivePulloutInfo);
		when(inactivePullout.getPullInInfo()).thenReturn(inactivePullinInfo);
		
		pulloutInfo.add(activePullout);
		pulloutInfo.add(inactivePullout);
		
		when(activePulloutInfo.getTime()).thenReturn(getPullInOutTime(-1));
		when(activePullinInfo.getTime()).thenReturn(getPullInOutTime(8));
		when(inactivePulloutInfo.getTime()).thenReturn(getPullInOutTime(1));
		when(inactivePullinInfo.getTime()).thenReturn(getPullInOutTime(8));
		
		List<VehiclePullInOutInfo> activePullouts = service.getActivePullOuts(pulloutInfo);
		
		assertEquals("Expecting 1 active pull out", 1, activePullouts.size());
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
	
	private String getPullInOutTime(int offset) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, offset);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");
		return formatter.format(calendar.getTime());
	}

}
