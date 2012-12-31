package org.onebusaway.nyc.transit_data_manager.adapters.data;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;

public class ImporterBusDepotDataTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetVehiclesExceptForDepotNameStr() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetGroupsExceptForDepots() {
		List<CPTFleetSubsetGroup> depotGroups = new ArrayList<CPTFleetSubsetGroup>();
		CPTFleetSubsetGroup group = new CPTFleetSubsetGroup();
		group.setGroupName("AB");
		depotGroups.add(group);
		ImporterBusDepotData data = new ImporterBusDepotData(depotGroups); 
		fail("Not yet finished");
	}

}
