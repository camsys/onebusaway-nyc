package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionService;
import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class KneelingVehiclesResourceTest {

    /**
     * unit testing for KneelingVehiclesResource
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    @Test
    public void testKneelingVehiclesResouceOutputMap() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode correctListOfKneelingVehicles = mapper.readTree(getClass().getResourceAsStream(
                "kneelingVehicleMap_2022_01_27.json"));

        KneelingVehiclesDataExtractionService extractionService = new KneelingVehiclesDataExtractionServiceImpl();
        extractionService.setInputSource(getClass().getResourceAsStream("fleet_2022_01_27.xml"));
        KneelingVehiclesResouce kneelingVehiclesResouce = new KneelingVehiclesResouce();
        kneelingVehiclesResouce.setKneelingVehiclesDataExtractionService(extractionService);

        JsonNode generatedListOfKneelingVehicles = mapper.readTree((String)
                kneelingVehiclesResouce.getKneelingVehiclesMap().getEntity());

        // Check that generatedList matches correctList
        assertEquals(correctListOfKneelingVehicles, generatedListOfKneelingVehicles);

    }


    @Test
    public void testKneelingVehiclesResouceOutputList() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode correctListOfKneelingVehicles = mapper.readTree(getClass().getResourceAsStream(
                "kneelingVehicleList_2022_01_27.json"));

        KneelingVehiclesDataExtractionService extractionService = new KneelingVehiclesDataExtractionServiceImpl();
        extractionService.setInputSource(getClass().getResourceAsStream("fleet_2022_01_27.xml"));
        KneelingVehiclesResouce kneelingVehiclesResouce = new KneelingVehiclesResouce();
        kneelingVehiclesResouce.setKneelingVehiclesDataExtractionService(extractionService);

        JsonNode generatedListOfKneelingVehicles = mapper.readTree((String)
                kneelingVehiclesResouce.getKneelingVehiclesList().getEntity());

        // Check that generatedList matches correctList
        assertEquals(correctListOfKneelingVehicles, generatedListOfKneelingVehicles);

    }

}
