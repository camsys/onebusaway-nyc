/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.tests;

public class SampleTripUpdateTest extends TripUpdateTest {

  //  public TripUpdateTest(String gtfsFile, String defaultAgencyId, String blockTripMapFile, String inferenceFile, String pbFile) {

  public SampleTripUpdateTest() {
    super("google_transit_manhattan.zip", "MTA", "6700_M104_2017-04-06/btmap.tsv", "6700_M104_2017-04-06/vlrb.tsv", "6700_M104_2017-04-06/tripupdate.pb");
  }
}
