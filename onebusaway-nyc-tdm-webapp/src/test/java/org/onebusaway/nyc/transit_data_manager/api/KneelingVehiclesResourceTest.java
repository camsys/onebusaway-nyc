package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;
import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionService;
import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionServiceImpl;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.transit_data_manager.json.LowerCaseWDashesGsonJsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.*;

public class KneelingVehiclesResourceTest {

    /**
     * unit testing for KneelingVehiclesResource
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */
    private static Logger log = LoggerFactory.getLogger(KneelingVehiclesResourceTest.class);

    @Test
    public void testKneelingVehiclesResouceOutputList() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode correctListOfKneelingVehicles = mapper.readTree(getClass().getResourceAsStream(
                "kneelingVehicleSet_2023_01_31_2238.json"));

        KneelingVehiclesDataExtractionServiceImpl extractionService = new KneelingVehiclesDataExtractionServiceImpl();
        extractionService.setMappingTool(new TcipMappingTool());
        extractionService.setInputOverride(Paths.get(getClass().getResource("depot_assignments_20230131_2238.xml").toURI()).toFile());
        KneelingVehiclesResource kneelingVehiclesResource = new KneelingVehiclesResource();
        kneelingVehiclesResource.setKneelingVehiclesDataExtractionService(extractionService);
        kneelingVehiclesResource.setJsonTool(new LowerCaseWDashesGsonJsonTool());

        JsonNode generatedSetOfKneelingVehicles = mapper.readTree((String)
                kneelingVehiclesResource.getKneelingVehiclesSet().getEntity());
        HashMap<JsonNode,JsonNode> generatedHash = convertJSONListToHashMap(generatedSetOfKneelingVehicles);

        int count = 0;
        for(JsonNode node : correctListOfKneelingVehicles){
            JsonNode match = generatedHash.get(node);
            log.info(String.valueOf(match));
            assertNotNull(match);
            count++;
        }
        assertTrue(generatedHash.size()<=count);

    }


    private HashMap<JsonNode,JsonNode> convertJSONListToHashMap(JsonNode list){
        HashMap<JsonNode,JsonNode> out = new HashMap<>();
        for (JsonNode node : list){
            out.put(node,node);
        }
        return out;
    }

}
