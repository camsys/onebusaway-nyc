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

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;

/**
 * Retrieve historical records from {@link CcLocationReportRecord} and {@link ArchivedInferredLocationRecord}
 * @author abelsare
 *
 */
public interface HistoricalRecordsDao {
	
	/**
	 * Returns historical vehicle records matching the filter values
	 * @param filter filter that needs to be applied on results
	 * @return historical vehicle records matching filters
	 */
	List<HistoricalRecord> getHistoricalRecords(Map<CcAndInferredLocationFilter, Object> filter);

}
