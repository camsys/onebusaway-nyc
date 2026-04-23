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
package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.mockito.Mockito;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;

public class ApcIntegrationServiceImplTest {

    String apcJson = "{\"9507\":{\"vehicle\":9507,\"timestamp\":1592243923000,\"agencyId\":\"MTA NYCT\",\"estCapacity\":null,\"estLoad\":\"6\",\"loadDescription\":\"6\",\"vehicleDescription\":\"2018 NEW FLYER HYBRID  XDE40\"},\"9509\":{\"vehicle\":9509,\"timestamp\":1592243917000,\"agencyId\":\"MTA NYCT\",\"estCapacity\":null,\"estLoad\":\"17\",\"loadDescription\":\"17\",\"vehicleDescription\":\"2018 NEW FLYER HYBRID  XDE40\"}}";

    @Test
    public void testGetFeed() throws Exception {
        //feed is object map key:vehicleId, value: ApcData
        String url = "http://example.com/feed"; // we mock out the results, this isn't used
        Map<AgencyAndId, VehicleOccupancyRecord> map = new HashMap<AgencyAndId, VehicleOccupancyRecord>();
        ApcIntegrationServiceImpl impl = new ApcIntegrationServiceImpl();
        ApcLoadLevelCalculator calculator = new ApcLoadLevelCalculator();
        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        Mockito.when(entity.getContent()).thenReturn(getAsStream(apcJson));
        Mockito.when(response.getEntity()).thenReturn(entity);
        Mockito.when(httpClient.execute((HttpUriRequest) any())).thenReturn(response);
        ConfigurationService service = Mockito.mock(ConfigurationService.class);
        ApcIntegrationServiceImpl.RawCountWebServicePollerThread thread
                = new ApcIntegrationServiceImpl.RawCountWebServicePollerThread(null, null, service,null,
                calculator,
                map,
                url) {
            @Override
            protected HttpClient getHttpClient() {
                return httpClient;
            }
        };
        Map<AgencyAndId, ApcLoadData> feed = thread.getFeed();
        assertNotNull(feed);
        assertEquals(2, feed.size());
        assertNotNull(feed.get(new AgencyAndId("MTA NYCT","9507")));
        assertNotNull(feed.get(new AgencyAndId("MTA NYCT","9509")));

        ApcLoadData a9507 = feed.get(new AgencyAndId("MTA NYCT","9507"));
        assertEquals("9507", a9507.getVehicle());
        assertEquals("MTA NYCT_9507", a9507.getVehicleAsId().toString());
        assertEquals(1592243923000l, a9507.getTimestamp());
        assertEquals(null, a9507.getEstCapacityAsInt());
        assertEquals(new Integer(6), a9507.getEstLoadAsInt());
        assertEquals("6", a9507.getLoadDescription());
        assertNull(a9507.getEstCapacityAsInt());

    }

    private InputStream getAsStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }
}
