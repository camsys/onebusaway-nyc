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

package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

/**
 * OBANYC-3416
 *
 */
public class Trace_8396_dsc_from_34047_to_9429_IntegrationTest extends AbstractTraceRunner {

public Trace_8396_dsc_from_34047_to_9429_IntegrationTest() throws Exception {
        super("8396_dsc_from_34047_to_9429.csv");
        // Date format is 2013-07-15T19:40:00EDT
        setBundle("2023Sept_Prod_r02_b03_PREDATE_SHUTTLES", "2023-09-18T16:02:00EDT");
        }
        }