/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.onebusaway.nyc.transit_data_manager.api.dao.CapiDaoFileImpl;
import org.onebusaway.nyc.transit_data_manager.api.dao.CapiDaoHttpImpl;
import org.onebusaway.nyc.transit_data_manager.api.service.CapiRetrievalServiceImpl;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;

import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CancelledTripsResourceTest {

    /**
     * unit testing for canceled trips integrator
     *
     * @author caylasavitzky
     *
     */

    PersonalConfigurationService datastoreInterface = new PersonalConfigurationService();

    @Spy
    CapiDaoFileImpl capiFileDao;

    @Spy
    CapiDaoHttpImpl capiHttpDao;

    @Spy
    CapiRetrievalServiceImpl capiService;

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                            .setTimeZone(Calendar.getInstance().getTimeZone());

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testCapiOutToNYCCancelledTripBeans() throws Exception {
        capiFileDao.setLocation("sample_capi_input.json");
        capiService.setCapiDao(capiFileDao);
        capiService.updateCancelledTripBeans();

        CancelledTripsResource resource = new CancelledTripsResource();
        resource.setCapiService(capiService);
        resource.setupObjectMapper();

        Response r = resource.getCancelledTripsList();
        String outputJson = (String) r.getEntity();

        JsonNode tdmCapiOutput = mapper.readTree(outputJson);
        JsonNode expectedCapiOutput = mapper.readTree(getClass().getResourceAsStream("expected_capi_output.json"));

        // Check that json string output matches expected value
        assertEquals(tdmCapiOutput,expectedCapiOutput);

        // Check that json can be deserialized
        List<CancelledTripBean> beans = readOutput((String) r.getEntity());
        assertTrue(beans.size()==3);

        // Check that deserialized values match expected values
        CancelledTripBean bean = beans.get(1);
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207",bean.getBlock());
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207",bean.getTrip());
        assertEquals("canceled",bean.getStatus());
        assertEquals("2022-01-21",bean.getServiceDate());
        assertEquals("B41",bean.getRoute());
        assertEquals("MTA NYCT_B41", bean.getRouteId());
        assertEquals("MTA_303215",bean.getFirstStopId());
        assertEquals("07:29:00",bean.getFirstStopDepartureTime());
        assertEquals("07:49:00",bean.getLastStopArrivalTime());
        assertTrue(bean.getTimestamp()==Long.valueOf("1642734418000"));
        assertEquals("2022-01-21T07:23:00",bean.getScheduledPullOut());
        assertEquals("2022-01-20T22:06:58",bean.getHumanReadableTimestamp());
    }

    private List<CancelledTripBean> readOutput(String str) throws JsonProcessingException {
        return mapper.readValue(str, new TypeReference<List<CancelledTripBean>>(){});
    }


    @Test
    public void testConfig() throws Exception {
        datastoreInterface.setConfigurationValue("tdm", "cancelledTrips.CAPIUrl",null);
        capiHttpDao.setConfig(datastoreInterface);
        capiHttpDao.refreshConfig();
        assertEquals(capiHttpDao.getLocation(),datastoreInterface.getConfigurationValueAsString( "cancelledTrips.CAPIUrl", null));
    }

    private class PersonalConfigurationService implements ConfigurationService {
        Map<String, String> configMap = new HashMap<>();


        @Override
        public String getConfigurationValueAsString(String configurationItemKey, String defaultValue) {
            return configMap.get(configurationItemKey);
        }

        @Override
        public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
            return null;
        }

        @Override
        public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
            String str = configMap.get(configurationItemKey);
            if (str == null) return defaultValue;
            return Integer.valueOf(str);
        }

        @Override
        public Boolean getConfigurationValueAsBoolean(String configurationItemKey, Boolean defaultValue) {
            return null;
        }

        @Override
        public void setConfigurationValue(String component, String configurationItemKey, String value) throws Exception {
            configMap.put(configurationItemKey, value);
        }

        @Override
        public Map<String, String> getConfiguration() {
            return null;
        }

        public void setConfigItemByComponentKey(String component, String key, String configString) {

        }
    };

}
