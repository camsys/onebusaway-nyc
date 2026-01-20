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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data_manager.api.dao.DataFetcherDao;
import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherConnectionData;
import org.onebusaway.nyc.transit_data_manager.api.datafetcher.DataFetcherFactory;
import org.onebusaway.nyc.transit_data_manager.api.service.CapiRetrievalServiceImpl;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class CancelledTripsResourceTest {

    private PersonalConfigurationService configurationService;

    @Mock
    private DataFetcherFactory dataFetcherFactory;

    @Mock
    private DataFetcherDao dataFetcherDao;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private ServletContext servletContext;

    private CapiRetrievalServiceImpl capiService;

    private ObjectMapper mapper;

    private String sampleCapiJson;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Initialize configuration service
        configurationService = new PersonalConfigurationService();
        configurationService.setConfigurationValue("tdm", "tdm.enableCapi", "true");
        configurationService.setConfigurationValue("tdm", "tdm.capiUrl", "file://sample_capi_input.json");
        configurationService.setConfigurationValue("tdm", "tdm.capiConnectionTimeout", "5000");
        configurationService.setConfigurationValue("tdm", "tdm.capiRefreshInterval", "60000");
        configurationService.setConfigurationValue("tdm", "tdm.capiCacheTimeout", "120000");

        // Initialize mapper
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                .setTimeZone(Calendar.getInstance().getTimeZone());

        // Load sample CAPI JSON
        sampleCapiJson = loadSampleCapiJson();

        // Mock servlet context
        when(servletContext.getInitParameter("capi.user")).thenReturn("testuser");
        when(servletContext.getInitParameter("capi.password")).thenReturn("testpass");
        when(servletContext.getInitParameter("capi.authHeader")).thenReturn("Authorization");

        // Mock data fetcher to return sample JSON
        when(dataFetcherDao.fetchData()).thenReturn(
                new ByteArrayInputStream(sampleCapiJson.getBytes(StandardCharsets.UTF_8))
        );

        // Mock factory to return the mocked data fetcher
        when(dataFetcherFactory.getDataFetcher(any(DataFetcherConnectionData.class)))
                .thenReturn(dataFetcherDao);

        // Create the service with mocked dependencies
        capiService = new CapiRetrievalServiceImpl(
                configurationService,
                dataFetcherFactory,
                taskScheduler
        );

        // Set servlet context to initialize credentials
        capiService.setServletContext(servletContext);

        // Manually trigger setup since @PostConstruct won't run in test
        capiService.refreshConfig();
    }

    @Test
    public void testCapiOutToNYCCancelledTripBeans() throws Exception {
        // Update cancelled trip beans from the mocked data source
        capiService.updateCancelledTripBeans();

        // Create the resource and set the service
        CancelledTripsResource resource = new CancelledTripsResource();
        resource.setCapiService(capiService);
        resource.setupObjectMapper();

        // Get the response
        Response r = resource.getCancelledTripsList();
        String outputJson = (String) r.getEntity();

        JsonNode tdmCapiOutput = mapper.readTree(outputJson);
        JsonNode expectedCapiOutput = mapper.readTree(
                getClass().getResourceAsStream("expected_capi_output.json")
        );

        // Check that json string output matches expected value
        assertEquals(expectedCapiOutput, tdmCapiOutput);

        // Check that json can be deserialized
        List<CancelledTripBean> beans = readOutput((String) r.getEntity());
        assertTrue(beans.size() == 3);

        // Check that deserialized values match expected values
        CancelledTripBean bean = beans.get(1);
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207", bean.getBlock());
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207", bean.getTrip());
        assertEquals("canceled", bean.getStatus());
        assertEquals("2022-01-21", bean.getServiceDate());
        assertEquals("B41", bean.getRoute());
        assertEquals("MTA NYCT_B41", bean.getRouteId());
        assertEquals("MTA_303215", bean.getFirstStopId());
        assertEquals("07:29:00", bean.getFirstStopDepartureTime());
        assertEquals("07:49:00", bean.getLastStopArrivalTime());
        assertTrue(bean.getTimestamp() == Long.valueOf("1642734418000"));
        assertEquals("2022-01-21T07:23:00", bean.getScheduledPullOut());
        assertEquals("2022-01-20T22:06:58", bean.getHumanReadableTimestamp());
    }

    private List<CancelledTripBean> readOutput(String str) throws JsonProcessingException {
        return mapper.readValue(str, new TypeReference<List<CancelledTripBean>>() {});
    }

    @Test
    public void testConfig() throws Exception {
        // Set configuration value
        String testUrl = "http://test.example.com/capi";
        configurationService.setConfigurationValue("tdm", "tdm.capiUrl", testUrl);

        // Refresh configuration
        capiService.refreshConfig();

        // Verify the location was set from configuration
        assertEquals(testUrl, capiService.getLocation());
    }

    @Test
    public void testServiceEnabled() {
        assertTrue(capiService.isEnabled());
        assertEquals("file://sample_capi_input.json", capiService.getFeedUrl());
    }

    /**
     * Loads sample CAPI JSON for testing.
     * In a real scenario, this would load from the actual test resource file.
     */
    private String loadSampleCapiJson() throws IOException {
        JsonNode sample_capi_input = mapper.readTree(
                getClass().getResourceAsStream("sample_capi_input.json")
        );
        return sample_capi_input.toString();
    }

    /**
     * Simple in-memory configuration service for testing.
     */
    private class PersonalConfigurationService implements ConfigurationService {
        private Map<String, String> configMap = new HashMap<>();

        @Override
        public String getConfigurationValueAsString(String configurationItemKey, String defaultValue) {
            String value = configMap.get(configurationItemKey);
            return value != null ? value : defaultValue;
        }

        @Override
        public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
            String str = configMap.get(configurationItemKey);
            if (str == null) return defaultValue;
            try {
                return Float.valueOf(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
            String str = configMap.get(configurationItemKey);
            if (str == null) return defaultValue;
            try {
                return Integer.valueOf(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public Boolean getConfigurationValueAsBoolean(String configurationItemKey, Boolean defaultValue) {
            String str = configMap.get(configurationItemKey);
            if (str == null) return defaultValue;
            return Boolean.valueOf(str);
        }

        @Override
        public void setConfigurationValue(String component, String configurationItemKey, String value) throws Exception {
            configMap.put(configurationItemKey, value);
        }

        @Override
        public Map<String, String> getConfiguration() {
            return new HashMap<>(configMap);
        }
    }
}