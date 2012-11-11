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
