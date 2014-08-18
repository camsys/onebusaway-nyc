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
import org.onebusaway.nyc.integration_tests.nyc_webapp.SiriScheduleDeviation_IntegrationTest;
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
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_2782_deadhead_nyct_bus_as_mta_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_3088_in_progress_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_324_20121004_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_3649_20101125T121801_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_3819_20120829_220004_222001_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4111_20120717_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4138_20111207_150000_220000_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4255_20111215_183433_183932_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4257_20120907_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4264_20120907T162054_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4399_20131225_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4655_mtabus_as_nyct_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_4855_export_trips_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_5318_20101202T172138_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_5558_layover_in_motion_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_5799_20121011_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_5725_20120919T101417_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_6154_20111214_123000_143500_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_6154_20111214_123000_143500_no_dsc_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_6333_20120717_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_6844_deadhead_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7040_layover_oosdsc_formal_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7040_layover_oosdsc_informal_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101122T084226_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101122T221007_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101123T031734_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7560_20101127T003153_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7564_20101201T010042_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7564_20101202T034055_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7564_20101202T114909_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7659_20121010_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_7726_20121010_IntegrationTest;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.Trace_9527_Q_MISC_IntegrationTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ 

	PauseDuringIntegrationTest.class,

	/*
	 * Inference tests
	 */
	
	// 2014April_Prod_r12_b01
	Trace_4655_mtabus_as_nyct_IntegrationTest.class,
	
	// 2014April_Prod_r06_b02
	Trace_4855_export_trips_IntegrationTest.class,
	Trace_6844_deadhead_IntegrationTest.class,
	
	// 2014Jan_AllCity_r09_b3
	Trace_3088_in_progress_IntegrationTest.class,
	Trace_2782_deadhead_nyct_bus_as_mta_IntegrationTest.class,
  // 2013June_Prod_r04_b03
	Trace_5558_layover_in_motion_IntegrationTest.class,
	
  // 2013Sept_Prod_r08_b04
	Trace_4399_20131225_IntegrationTest.class,
	SiriScheduleDeviation_IntegrationTest.class,

	// 2013Sept_AllCity_r15_b02
	Trace_9527_Q_MISC_IntegrationTest.class,
	
	// September_Bronx_r10_b03
	Trace_324_20121004_IntegrationTest.class,
	Trace_5799_20121011_IntegrationTest.class,
	Trace_7659_20121010_IntegrationTest.class,
	Trace_7726_20121010_IntegrationTest.class,
	Trace_5725_20120919T101417_IntegrationTest.class,
	Trace_4257_20120907_IntegrationTest.class,
	Trace_4264_20120907T162054_IntegrationTest.class,

	// b63-winter 10
	Trace_0927_20101209T124742_IntegrationTest.class,
	Trace_1325_20101215T014845_IntegrationTest.class,
	Trace_1379_20101211T010025_IntegrationTest.class,
	Trace_1404_20101210T034249_IntegrationTest.class,
	Trace_3649_20101125T121801_IntegrationTest.class,
	Trace_5318_20101202T172138_IntegrationTest.class,
	Trace_7560_20101122T084226_IntegrationTest.class,
	Trace_7560_20101122T221007_IntegrationTest.class,
	Trace_7560_20101123T031734_IntegrationTest.class,
	Trace_7560_20101127T003153_IntegrationTest.class,
	Trace_7564_20101201T010042_IntegrationTest.class,
	Trace_7564_20101202T034055_IntegrationTest.class,
	Trace_7564_20101202T114909_IntegrationTest.class,

	// 2012Jan_SIB63M34_r20_b01
	Trace_2423_20120111_091352_092348_IntegrationTest.class,    

	// 2012July_r04_b02
	Trace_2433_20120723_IntegrationTest.class,
	Trace_4111_20120717_IntegrationTest.class,
	Trace_6333_20120717_IntegrationTest.class,
	Trace_3819_20120829_220004_222001_IntegrationTest.class,

	// si
	Trace_2711_20111208_054046_102329_IntegrationTest.class,
	Trace_2782_20111202_211038_222038_IntegrationTest.class,
	Trace_4138_20111207_150000_220000_IntegrationTest.class,
	Trace_4255_20111215_183433_183932_IntegrationTest.class,
	Trace_6154_20111214_123000_143500_IntegrationTest.class,
	Trace_6154_20111214_123000_143500_no_dsc_IntegrationTest.class,  	

	// paired down (SI) version of 2013June_Prod_r03_b02
	Trace_7040_layover_oosdsc_formal_IntegrationTest.class,
	Trace_7040_layover_oosdsc_informal_IntegrationTest.class,
	/*
	 * Wrapping logic tests
	 */
	SiriBlockInference_IntegrationTest.class,
	SiriBlockLayoverInference_IntegrationTest.class,

	SiriTripInference_IntegrationTest.class,
	SiriTripLayoverInference_IntegrationTest.class
})
public class TestSuite {}
