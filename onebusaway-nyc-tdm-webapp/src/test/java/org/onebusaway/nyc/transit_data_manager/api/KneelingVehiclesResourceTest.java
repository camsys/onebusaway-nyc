package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionService;
import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionServiceImpl;
import org.onebusaway.nyc.transit_data_manager.json.LowerCaseWDashesGsonJsonTool;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KneelingVehiclesResourceTest {

    /**
     * unit testing for KneelingVehiclesResource
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    @Test
    public void testKneelingVehiclesResouceOutputList() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode correctListOfKneelingVehicles = mapper.readTree(getClass().getResourceAsStream(
                "kneelingVehicleList_2023_01_31_2238.json"));

        KneelingVehiclesDataExtractionService extractionService = new KneelingVehiclesDataExtractionServiceImpl();
        extractionService.setInputOverride(Paths.get(getClass().getResource("depot_assignments_20230131_2238.xml").toURI()).toFile());
        KneelingVehiclesResource kneelingVehiclesResource = new KneelingVehiclesResource();
        kneelingVehiclesResource.setKneelingVehiclesDataExtractionService(extractionService);
        kneelingVehiclesResource.setJsonTool(new LowerCaseWDashesGsonJsonTool());

        JsonNode generatedListOfKneelingVehicles = mapper.readTree((String)
                kneelingVehiclesResource.getKneelingVehiclesList().getEntity());

        assertEquals(correctListOfKneelingVehicles, generatedListOfKneelingVehicles);

    }

}
