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

package org.onebusaway.nyc.gtfsrt.integration_tests;

/**
 * First pass at a GTFS-RT integration test.
 */
public class SampleIntegrationTest extends AbstractInputRunner {
    public SampleIntegrationTest() throws Exception {
        super("20170412_7198", "2017April_Prod_rt_test", "2017-04-12T11:05:00EDT");
    }
}
