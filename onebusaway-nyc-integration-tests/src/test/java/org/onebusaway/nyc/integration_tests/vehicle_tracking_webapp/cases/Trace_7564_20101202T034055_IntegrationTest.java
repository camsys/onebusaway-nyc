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

import org.onebusaway.realtime.api.EVehiclePhase;

/**
 * Trace: 7564-2010-12-02T03-40-55
 * 
 * blockA = 2008_12888414
 * 
 * duration = 4:55-8:55am
 * 
 * layovers = 07:08 AM - 08:25 AM
 * 
 * @author bdferris
 */
public class Trace_7564_20101202T034055_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101202T034055_IntegrationTest() throws Exception {
    super("7564-2010-12-02T03-40-55.csv.gz");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
    
    /**
     * FIXME traces think it should be in progress, new model
     * believes it should be deadhead; i think it should be
     * layover.
     */
    setMinAccuracyRatioForPhase(EVehiclePhase.IN_PROGRESS, 0.8);
  }
}
