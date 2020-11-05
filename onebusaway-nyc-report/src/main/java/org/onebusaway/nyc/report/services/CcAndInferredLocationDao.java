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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;

public interface CcAndInferredLocationDao extends LocationDao {

  void saveOrUpdateRecord(ArchivedInferredLocationRecord record);

  void saveOrUpdateRecords(ArchivedInferredLocationRecord... records);
  
  void saveOrUpdateRecord(CcAndInferredLocationRecord record);
  
  void saveOrUpdateRecords(Collection<CcAndInferredLocationRecord> records);

  /**
   * Returns last known location records applying filters if available in the URL
   * @param filter filter parameters from request URL
   * @return matching last known location records
   */
  List<CcAndInferredLocationRecord> getAllLastKnownRecords(Map<CcAndInferredLocationFilter, String> filter);
  
  CcAndInferredLocationRecord getLastKnownRecordForVehicle(Integer vehicleId) throws Exception;

  Integer getArchiveInferredLocationCount();

  Integer getCcLocationReportRecordCount();

  Integer getCcAndInferredLocationCount();
  
}
