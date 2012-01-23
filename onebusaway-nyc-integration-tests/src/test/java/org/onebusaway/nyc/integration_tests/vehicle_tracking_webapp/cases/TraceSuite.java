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
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * All the trace tests in one suite so they can quickly be run from within
 * Eclipse for testing.
 * 
 * @author bdferris
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ 
    /*
     * current bundle traces
     */
    Trace_2782_20111202_211038_222038_IntegrationTest.class,
    Trace_4138_20111207_150000_220000_IntegrationTest.class,
    Trace_4255_20111215_183433_183932_IntegrationTest.class,
    Trace_6154_20111214_123000_143500_IntegrationTest.class,
    Trace_2711_20111208_054046_102329_IntegrationTest.class,
    Trace_7564_20101203T004633_with_runs_reported_IntegrationTest.class,
    
    /*
     * old bundle traces
     */
    Trace_7564_20101203T004633_IntegrationTest.class,
    Trace_0927_20101209T124742_IntegrationTest.class,
    Trace_1325_20101215T014845_IntegrationTest.class,
    Trace_1379_20101211T010025_IntegrationTest.class,
    Trace_1404_20101210T034249_IntegrationTest.class,
    Trace_3649_20101125T121801_IntegrationTest.class,
    Trace_5318_20101202T172138_IntegrationTest.class,
    Trace_7560_20101122T084226_IntegrationTest.class,
    Trace_7560_20101122T221007_IntegrationTest.class,
    Trace_7560_20101123T031734_IntegrationTest.class,
    Trace_7560_20101123T234515_IntegrationTest.class,
    Trace_7560_20101127T003153_IntegrationTest.class,
    Trace_7560_20101127T232847_IntegrationTest.class,
    Trace_7560_20101129T011744_IntegrationTest.class,
    Trace_7560_20101129T233021_IntegrationTest.class,
    Trace_7561_20101209T124755_IntegrationTest.class,
    Trace_7563_20101209T124732_IntegrationTest.class,
    Trace_7564_20101129T041335_IntegrationTest.class,
    Trace_7564_20101201T010042_IntegrationTest.class,
    Trace_7564_20101202T034055_IntegrationTest.class,
    Trace_7564_20101202T114909_IntegrationTest.class,
    Trace_7564_20101206T005551_IntegrationTest.class })
public class TraceSuite {
}
