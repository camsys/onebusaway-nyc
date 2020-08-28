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

package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.List;

import org.joda.time.DateMidnight;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

/**
 * Designed to work with the entire set of Operator Assignment Data.
 * 
 * @author sclark
 * 
 */
public interface OperatorAssignmentData {

  /**
   * Get the entire set of service dates from the data.
   * 
   * @return A list of DateTimes each a different service date in the system.
   */
  List<DateMidnight> getAllServiceDates();

  /**
   * Fetch all the operator assignments for the input service date.
   * 
   * @param serviceDate a DateTime for which operator assignments should be
   *          fetched.
   * @return A list of SCHOperatorAssignments, one for each operator assignment
   *         on that service date.
   */
  List<SCHOperatorAssignment> getOperatorAssignmentsByServiceDate(
      DateMidnight serviceDate);
}
