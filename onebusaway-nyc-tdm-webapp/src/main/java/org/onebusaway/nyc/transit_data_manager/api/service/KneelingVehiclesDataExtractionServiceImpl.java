package org.onebusaway.nyc.transit_data_manager.api.service;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.MostRecentFilePicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CPTVehicleIden;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KneelingVehiclesDataExtractionServiceImpl implements KneelingVehiclesDataExtractionService{

    /**
     * service impl to support tdm api resource to expose kneeling bus data extracted from SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    private static Logger log = LoggerFactory.getLogger(KneelingVehiclesDataExtractionServiceImpl.class);
    private MostRecentFilePicker mostRecentFilePicker;
    private TcipMappingTool mappingTool;
    private File inputOverride;

    @Override
    public void setInputOverride(File resource) {
        inputOverride=resource;
    }

    @Override
    public Map getKneelingVehiclesAsMap(DepotIdTranslator depotIdTranslator) {
        return processVehiclesToMap(getVehiclesData(depotIdTranslator));
    }

    @Override
    public List getKneelingVehiclesAsList(DepotIdTranslator depotIdTranslator) {
        return processVehiclesToList(getVehiclesData(depotIdTranslator));
    }

    public List<MtaBusDepotAssignment> getVehiclesData(DepotIdTranslator depotIdTranslator) {
        File inputFile = inputOverride!=null? inputOverride : mostRecentFilePicker.getMostRecentSourceFile();

        log.debug("Getting VehicleDepotData object in getVehicleDepotData from " + inputFile.getPath());

        List<MtaBusDepotAssignment> resultData = null;

        MtaBusDepotFileToDataCreator process;
        try {
            process = new MtaBusDepotFileToDataCreator(inputFile);

            if (depotIdTranslator == null) {
                log.info("Depot ID translation has not been enabled properly. Depot ids will not be translated.");
            } else {
                log.info("Using depot ID translation.");
            }
//            process.setDepotIdTranslator(depotIdTranslator);

            resultData = process.loadDepotAssignments();
        } catch (IOException e) {
            log.info("Could not create data object from " + inputFile.getPath());
            log.info(e.getMessage());
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        log.debug("Returning VehicleDepotData object in getVehicleDepotData.");
        return resultData;
    }



    public List getKneelingVehiclesList(List<CPTFleetSubsetGroup> depotGroups) {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();

        //Loop through all depot groups and create vehicles with the required info
        for(CPTFleetSubsetGroup depotGroup : depotGroups) {
            List<CPTVehicleIden> tcipVehciles = depotGroup.getGroupMembers().getGroupMember();
            for(CPTVehicleIden tcipVehicle : tcipVehciles) {
                Vehicle vehicle = new Vehicle();

                vehicle.setDepotId(depotGroup.getGroupGarage().getFacilityName());
                vehicle.setVehicleId(String.valueOf(tcipVehicle.getVehicleId()));
                vehicle.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(tcipVehicle.getAgencyId()));

                vehicles.add(vehicle);
            }
        }

        return vehicles;
    }


    private List processVehiclesToList(List<MtaBusDepotAssignment> data){
        return data.stream().filter(d -> d.isKneeling()).map(d-> createAgencyAndIdFromMTABusAssig(d)).collect(Collectors.toList());
    }

    private Map processVehiclesToMap(List<MtaBusDepotAssignment> data){
        return data.stream().collect(Collectors.toMap(d-> createAgencyAndIdFromMTABusAssig(d), d->d));
    }

    private AgencyAndId createAgencyAndIdFromMTABusAssig(MtaBusDepotAssignment assignment) {
        AgencyAndId bus = new AgencyAndId();
        bus.setAgencyId(String.valueOf(assignment.getAgencyId()));
        bus.setId(String.valueOf(assignment.getBusNumber()));
        return bus;
    }

    /**
     * Injects most recent file picker
     * @param mostRecentFilePicker the mostRecentFilePicker to set
     */
    @Autowired
    @Qualifier("depotFilePicker")
    public void setMostRecentFilePicker(MostRecentFilePicker mostRecentFilePicker) {
        this.mostRecentFilePicker = mostRecentFilePicker;
    }

    /**
     * Injects {@link TcipMappingTool}
     * @param mappingTool the mappingTool to set
     */
    @Autowired
    @Qualifier("tcipMappingTool")
    public void setMappingTool(TcipMappingTool mappingTool) {
        this.mappingTool = mappingTool;
    }
}

