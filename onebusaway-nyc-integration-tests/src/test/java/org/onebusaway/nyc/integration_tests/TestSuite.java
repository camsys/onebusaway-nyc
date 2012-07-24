/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.integration_tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.onebusaway.nyc.integration_tests.nyc_webapp.SiriBlockInference_IntegrationTest;
import org.onebusaway.nyc.integration_tests.nyc_webapp.SiriBlockLayoverInference_IntegrationTest;
import org.onebusaway.nyc.integration_tests.nyc_webapp.SiriTripInference_IntegrationTest;
import org.onebusaway.nyc.integration_tests.nyc_webapp.SiriTripLayoverInference_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_0927_20101209T124742_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_1325_20101215T014845_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_1379_20101211T010025_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_1404_20101210T034249_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_2423_20120111_091352_092348_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_2433_20120723_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_2711_20111208_054046_102329_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_2782_20111202_211038_222038_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_3649_20101125T121801_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4111_20120717_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4138_20111207_150000_220000_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4255_20111215_183433_183932_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_5318_20101202T172138_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_6154_20111214_123000_143500_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_6154_20111214_123000_143500_no_dsc_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_6333_20120717_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101122T084226_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101122T221007_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101123T031734_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101127T003153_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7564_20101201T010042_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7564_20101202T034055_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7564_20101202T114909_IntegrationTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ 

	/*
     * Inference tests
     */
    Trace_0927_20101209T124742_IntegrationTest.class,
    Trace_1325_20101215T014845_IntegrationTest.class,
    Trace_1379_20101211T010025_IntegrationTest.class,
    Trace_1404_20101210T034249_IntegrationTest.class,
    Trace_2423_20120111_091352_092348_IntegrationTest.class,    
    Trace_2433_20120723_IntegrationTest.class,
    Trace_2711_20111208_054046_102329_IntegrationTest.class,
	Trace_2782_20111202_211038_222038_IntegrationTest.class,
    Trace_3649_20101125T121801_IntegrationTest.class,
    Trace_4111_20120717_IntegrationTest.class,
    Trace_4138_20111207_150000_220000_IntegrationTest.class,
    Trace_4255_20111215_183433_183932_IntegrationTest.class,
    Trace_5318_20101202T172138_IntegrationTest.class,
    Trace_6154_20111214_123000_143500_IntegrationTest.class,
    Trace_6154_20111214_123000_143500_no_dsc_IntegrationTest.class,
    Trace_6333_20120717_IntegrationTest.class,
    Trace_7560_20101122T084226_IntegrationTest.class,
    Trace_7560_20101122T221007_IntegrationTest.class,
    Trace_7560_20101123T031734_IntegrationTest.class,
    Trace_7560_20101127T003153_IntegrationTest.class,
    Trace_7564_20101201T010042_IntegrationTest.class,
    Trace_7564_20101202T034055_IntegrationTest.class,
    Trace_7564_20101202T114909_IntegrationTest.class,

	/*
	 * Wrapping logic tests
	 */
	SiriBlockInference_IntegrationTest.class,
	SiriBlockLayoverInference_IntegrationTest.class,

	SiriTripInference_IntegrationTest.class,
	SiriTripLayoverInference_IntegrationTest.class,
})
public class TestSuite {}