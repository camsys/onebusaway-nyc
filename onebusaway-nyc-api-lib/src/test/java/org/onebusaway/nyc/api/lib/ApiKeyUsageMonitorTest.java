/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.api.lib;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.cloud.api.ExternalServices;
import org.onebusaway.cloud.api.ExternalServicesBridgeFactory;
import org.onebusaway.nyc.api.lib.impl.ApiKeyUsageMonitorImpl;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;

import static org.junit.Assert.*;
import static org.onebusaway.nyc.api.lib.impl.ApiKeyUsageMonitorImpl.DEFAULT_MAX_KEY_COUNT_METRICS;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyUsageMonitorTest {

    private ExternalServices _es;
    private ConfigurationServiceImpl _configurationService;

    @Before
    public void setup(){
        _es = new ExternalServicesBridgeFactory().getExternalServices();
        _configurationService = new ConfigurationServiceImpl();
    }

    @Test
    public void testSortedKeyCounts(){
        ApiKeyUsageMonitorImpl apiMonitor = new ApiKeyUsageMonitorImpl();
        apiMonitor.setExternalServices(_es);
        apiMonitor.increment("Key2");
        apiMonitor.increment("Key1");
        apiMonitor.increment("Key1");
        apiMonitor.increment("Key3");
        apiMonitor.increment("Key1");
        apiMonitor.increment("Key1");
        apiMonitor.increment("Key2");

        Map<String, Double> keysSortedByUsage = apiMonitor.getKeysSortedByUsage(apiMonitor.getApiKeyCountMap());
        Double[] actualKeyCounts = keysSortedByUsage.values().toArray(new Double[3]);
        Double[] expectedKeyCounts = new Double[]{4.0, 2.0, 1.0};

        assertArrayEquals(expectedKeyCounts, actualKeyCounts);
    }

    @Test
    public void testGetMapKeyCount(){
        ApiKeyUsageMonitorImpl apiMonitor = new ApiKeyUsageMonitorImpl();
        apiMonitor.setExternalServices(_es);
        apiMonitor.setConfigurationService(_configurationService);
        apiMonitor.updateConfig();

        assertEquals(apiMonitor.getMaxKeyCountMetrics() -1, apiMonitor.getMapKeyCount(apiMonitor.getMaxKeyCountMetrics() - 1));
        assertNotSame(apiMonitor.getMaxKeyCountMetrics() + 1, apiMonitor.getMapKeyCount(apiMonitor.getMaxKeyCountMetrics() + 1));
        assertEquals(apiMonitor.getMaxKeyCountMetrics(), apiMonitor.getMapKeyCount(apiMonitor.getMaxKeyCountMetrics() + 1));
    }

    @Test
    public void testGetSleepTime(){
        ApiKeyUsageMonitorImpl apiMonitor = new ApiKeyUsageMonitorImpl();
        apiMonitor.setExternalServices(_es);
        apiMonitor.setConfigurationService(_configurationService);
        apiMonitor.updateConfig();

        int keyCount = 200;
        int sleepTime = 2000;
        int batchSize = 10;
        int publishingFrequencySec = 60;
        int expectedSleepTime = sleepTime;

        assertEquals(expectedSleepTime, apiMonitor.getSleepTime(keyCount, sleepTime, batchSize, publishingFrequencySec));

        /* Tests for case where if the sleep time is too small (less than 10ms) to make
         * sure that it gets increased to 10ms
         */
        sleepTime = 1;
        expectedSleepTime = 10;
        assertEquals(expectedSleepTime, apiMonitor.getSleepTime(keyCount, sleepTime, batchSize, publishingFrequencySec));


        /* Tests for behavior where if the sleep time is too large (ie. cannot publish all the metrics in the
         * metrics in the allotted time) it cuts the sleep time to a reasonable max sleep time (currently 80%
         * of the absolute max time possible)
         */
        sleepTime = 3000;
        expectedSleepTime = 2400;
        assertEquals(expectedSleepTime, apiMonitor.getSleepTime(keyCount, sleepTime, batchSize, publishingFrequencySec));

    }
}
