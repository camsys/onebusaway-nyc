package org.onebusaway.nyc.transit_data_manager.importers;

import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.PulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;
import tcip_final_3_0_5_1.CPTTransitFacilityIden;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.SCHPullInOutInfo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class ImporterVehiclePulloutDataTest {

    UtsMappingTool mappingTool;


    @Before
    public void setup() {
        mappingTool = new UtsMappingTool();
    }

    @Test
    public void validAuthIdTest(){
        List<VehiclePullInOutInfo> tcipPullouts = getTestPulloutFromAuthId("TA");
        PulloutData data = new ImporterVehiclePulloutData(tcipPullouts);

        List<VehiclePullInOutInfo> emptyPullouts = data.getPulloutsByAgency("MTABC");
        assertTrue(emptyPullouts.isEmpty());

        List<VehiclePullInOutInfo> pullouts = data.getPulloutsByAgency("MTA NYCT");
        assertEquals(1, pullouts.size());

        VehiclePullInOutInfo temp = pullouts.get(0);
        assertEquals(1, pullouts.size());
    }

    @Test
    public void invalidAuthIdTest(){
        List<VehiclePullInOutInfo> tcipPullouts = getTestPulloutFromAuthId("Unknown");
        PulloutData data = new ImporterVehiclePulloutData(tcipPullouts);

        List<VehiclePullInOutInfo> emptyPullouts = data.getPulloutsByAgency("MTABC");
        assertTrue(emptyPullouts.isEmpty());

        List<VehiclePullInOutInfo> pullouts = data.getPulloutsByAgency("MTA NYCT");
        assertTrue(emptyPullouts.isEmpty());
    }

    private List<VehiclePullInOutInfo> getTestPulloutFromAuthId(String authId){
        List<VehiclePullInOutInfo> tcipPullouts = new ArrayList<VehiclePullInOutInfo> ();

        SCHPullInOutInfo outputAssignment = new SCHPullInOutInfo();

        Long agencyId = mappingTool.getAgencyIdFromUtsAuthId(authId);
        String agencyDesignator = mappingTool.getAgencyDesignatorFromAgencyId(agencyId);

        // Set vehicle to new CPTVehicleIden
        CPTVehicleIden bus = new CPTVehicleIden();
        bus.setAgencyId(agencyId);
        bus.setVehicleId(0001);
        outputAssignment.setVehicle(bus);

        // Set the value of pullIn equal to isPullIn
        outputAssignment.setPullIn(false);

        // Set the garage to a new CPTTransitFacilityIden representing a depot.
        CPTTransitFacilityIden depot = new CPTTransitFacilityIden();
        depot.setFacilityName("TEST");
        depot.setFacilityId(new Long(0));
        depot.setAgencydesignator(agencyDesignator);
        outputAssignment.setGarage(depot);

        VehiclePullInOutInfo vehiclePipoInfo = new VehiclePullInOutInfo();
        vehiclePipoInfo.setPullOutInfo(outputAssignment);

        tcipPullouts.add(vehiclePipoInfo);

        return tcipPullouts;
    }

}
