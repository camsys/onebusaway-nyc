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

package org.onebusaway.nyc.report.services;

import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;

/**
 * Validates both real time and inference records for the required fields and other database constraints
 * such as values out of range. The records are discarded if validation fails.
 * @author abelsare
 *
 */
public interface RecordValidationService {
	
	/**
	 * Validates that given inferred record satisfies the database constraints set
	 * @param inferredRecord inferred record to validate
	 * @return true if the record satisfies the required constraints, false otherwise
	 */
	boolean validateInferenceRecord(NycQueuedInferredLocationBean inferredRecord);
	
	/**
	 * Validates that given real time record satisfies the database constraints set
	 * @param realTimeRecord real time record to validate
	 * @return true if the record satisfies the required constraints, false otherwise
	 */
	boolean validateRealTimeRecord(RealtimeEnvelope realTimeRecord);
	
	/**
	 * Validates the last known (operational API) record satisfies the database 
	 * constratints.
	 * @param record lastKnown record to validate
	 * @return true if the record satisfies the require constraints, false otherwise
	 */
	boolean validateLastKnownRecord(CcAndInferredLocationRecord record);

	/**
   * Validates that given post processed inferred record satisfies the database constraints set
   * @param inferredRecord inferred record to validate
   * @return true if the record satisfies the required constraints, false otherwise
   */
	boolean validateArchiveInferenceRecord(ArchivedInferredLocationRecord record);
	
	/**
	 * Checks if the given value is within allowed range of numbers
	 * @param value value to check
	 * @param lowerBound lower bound of the acceptable range
	 * @param upperBound upper bound of the acceptable range
	 * @return true if the value falls within the range, false otherwise
	 */
	boolean isValueWithinRange(Double value, double lowerBound, double upperBound);

  

}
