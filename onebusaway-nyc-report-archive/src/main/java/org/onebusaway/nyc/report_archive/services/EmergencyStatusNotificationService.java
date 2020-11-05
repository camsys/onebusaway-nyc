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

package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;

/**
 * Processes incoming bus records from the real time queue and sends notification to amazon sns service
 * if a bus is reporting emergency.
 * @author abelsare
 *
 */
public interface EmergencyStatusNotificationService {
	
	/**
	 * Process incoming record and check if it is reporting emergency. Dispatches an sns notification event
	 * if emergency status is reported.
	 * @param record incoming bus record
	 */
	void process(CcLocationReportRecord record);

}
