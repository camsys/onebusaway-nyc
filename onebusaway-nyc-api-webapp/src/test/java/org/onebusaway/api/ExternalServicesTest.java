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
package org.onebusaway.api;

import org.junit.Test;
import org.onebusaway.cloud.api.ExternalResult;
import org.onebusaway.cloud.api.ExternalServices;
import org.onebusaway.cloud.api.ExternalServicesBridgeFactory;

public class ExternalServicesTest {

    @Test
    public void testPublishMetric(){
        System.setProperty("oba.cloud.aws", "true");
        // Note, you can change cloudwatch destination with System properties "aws.accessKeyId" and "aws.secretKey"
        ExternalServices es = new ExternalServicesBridgeFactory().getExternalServices();
        String namespace = "Obanyc:test";
        ExternalResult result = es.publishMetric(namespace,"CloudwatchPluginTest",null,null,1);
    }
}
