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
package org.onebusaway.nyc.transit_data_manager.api.canceledTripsApi;

import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data.model.NycCancledTripBean;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CanceledTripsIntegratorTest {

    /**
     * unit testing for canceled trips integrator
     *
     * @author caylasavitzky
     *
     */

    @Mock
    ConfigurationDatastoreInterface datastoreInterface;

    @Mock
    ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Test
    public void testCapiOutToNYCCanceledTripBeans(){

        MockitoAnnotations.initMocks(this);

        String fakeCAPIOutput = "{\"Impacted\": [{\"Block\": \"MTA NYCT 1\", \"Trip\": \"MTA NYCT 1_1\", \"Status\": \"Cancelled\", \"actor\": \"Jim Jones\", \"timestamp\": \"20210114 12:45:00\"},{\"Block\": \"MTA NYCT 2\", \"Trip\": \"MTA NYCT 2_1\", \"Status\": \"Cancelled\", \"actor\": \"Jim Johns\", \"timestamp\": \"20210114 12:46:00\"}],\"timestamp\" : \"20210114 12:46:00\"}";
        StringBuffer buffer = new StringBuffer();
        buffer.append(fakeCAPIOutput);
        CanceledTripsIntegrator integrator = new CanceledTripsIntegrator();

        List<NycCancledTripBean> beans = integrator.makeCancelledTripBeansFromCapiOutput(buffer);

        NycCancledTripBean bean1 = beans.get(0);
        assertEquals(bean1.getBlock(),"MTA NYCT 1");
        assertEquals(bean1.getActor(),"Jim Jones");
        assertEquals(bean1.getStatus(),"Cancelled");
        assertEquals(bean1.getTrip(),"MTA NYCT 1_1");
        assertEquals(bean1.getTimestamp(),1610646300);

        NycCancledTripBean bean2 = beans.get(0);
        assertEquals(bean2.getBlock(),"MTA NYCT 2");
        assertEquals(bean2.getActor(),"Jim Johns");
        assertEquals(bean2.getStatus(),"Cancelled");
        assertEquals(bean2.getTrip(),"MTA NYCT 2_1");
        assertEquals(bean2.getTimestamp(),1610646360);
    }
}
