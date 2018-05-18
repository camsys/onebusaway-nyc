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

public class FutureTripTripUpdateTest extends TripUpdateTest {
  public FutureTripTripUpdateTest() {
    super("google_transit_bronx_2018April.zip", "MTA", "7738_BX35_2018-05-18/btmap.tsv", "7738_BX35_2018-05-18/vlrb.tsv", "7738_BX35_2018-05-18/tripupdate.pb");
  }

  @Override
  int getExpectedNumberOfTrips() {
    return 2;
  }

}
