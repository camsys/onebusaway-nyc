package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.TcipMappingTool;
import org.onebusaway.nyc.transit_data_manager.api.service.StrollerVehiclesDataExtractionServiceImpl;
import org.onebusaway.nyc.transit_data_manager.json.LowerCaseWDashesGsonJsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.*;

public class StrollerVehiclesResourceTest {

    /**
     * unit testing for StrollerVehiclesResource
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */
    private static Logger log = LoggerFactory.getLogger(StrollerVehiclesResourceTest.class);

    @Test
    public void testStrollerVehiclesResouceOutputList() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode correctListOfStrollerVehicles = mapper.readTree(getClass().getResourceAsStream(
                "strollerVehicleSet_2023_01_31_2238.json"));

        StrollerVehiclesDataExtractionServiceImpl extractionService = new StrollerVehiclesDataExtractionServiceImpl();
        extractionService.setMappingTool(new TcipMappingTool());
        extractionService.setInputOverride(Paths.get(getClass().getResource("depot_assignments_20230131_2238.xml").toURI()).toFile());
        StrollerVehiclesResource strollerVehiclesResource = new StrollerVehiclesResource();
        strollerVehiclesResource.setStrollerVehiclesDataExtractionService(extractionService);
        strollerVehiclesResource.setJsonTool(new LowerCaseWDashesGsonJsonTool());

        JsonNode generatedSetOfStrollerVehicles = mapper.readTree((String)
                strollerVehiclesResource.getStrollerVehiclesSet().getEntity());
        HashMap<JsonNode,JsonNode> generatedHash = convertJSONListToHashMap(generatedSetOfStrollerVehicles);

        int count = 0;
        for(JsonNode node : correctListOfStrollerVehicles){
            JsonNode match = generatedHash.get(node);
            System.out.println(String.valueOf(match));
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
