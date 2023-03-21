package org.onebusaway.nyc.transit_data_manager.api.service;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.MostRecentFilePicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StrollerVehiclesDataExtractionServiceImpl implements StrollerVehiclesDataExtractionService {

    /**
     * service impl to support tdm api resource to expose Stroller bus data extracted from SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    private static Logger log = LoggerFactory.getLogger(StrollerVehiclesDataExtractionServiceImpl.class);
    private MostRecentFilePicker mostRecentFilePicker;
    private TcipMappingTool mappingTool;
    private File inputOverride;

    @Override
    public void setInputOverride(File resource) {
        inputOverride=resource;
    }

    @Override
    public Map getStrollerVehiclesAsMap(DepotIdTranslator depotIdTranslator) {
        return processVehiclesToMap(getVehiclesData(depotIdTranslator));
    }

    @Override
    public Set<AgencyAndId> getStrollerVehiclesAsSet(DepotIdTranslator depotIdTranslator) {
        return processVehiclesToSet(getVehiclesData(depotIdTranslator));
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

    private List<AgencyAndId> processVehiclesToList(List<MtaBusDepotAssignment> data){
        return data.stream().filter(d -> d.isStroller()).map(d-> createAgencyAndIdFromMTABusAssig(d)).collect(Collectors.toList());
    }

    private Set<AgencyAndId> processVehiclesToSet(List<MtaBusDepotAssignment> data){
        return data.stream().filter(d -> d.isStroller()).map(d-> createAgencyAndIdFromMTABusAssig(d)).collect(Collectors.toSet());
    }

    private Map<AgencyAndId,Boolean> processVehiclesToMap(List<MtaBusDepotAssignment> data){
        return data.stream().collect(Collectors.toMap(d-> createAgencyAndIdFromMTABusAssig(d), d->d.isStroller()));
    }

    private AgencyAndId createAgencyAndIdFromMTABusAssig(MtaBusDepotAssignment assignment) {
        AgencyAndId bus = new AgencyAndId();
        bus.setAgencyId(mappingTool.getJsonModelAgencyIdByTcipId(assignment.getAgencyId()));
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

