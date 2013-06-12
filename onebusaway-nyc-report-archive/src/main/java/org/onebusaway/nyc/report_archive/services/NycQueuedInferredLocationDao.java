package org.onebusaway.nyc.report_archive.services;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.report_archive.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;

public interface NycQueuedInferredLocationDao extends LocationDao {

  void saveOrUpdateRecord(ArchivedInferredLocationRecord record);

  void saveOrUpdateRecords(ArchivedInferredLocationRecord... records);

  /**
   * Returns last known location records applying filters if available in the URL
   * @param filter filter parameters from request URL
   * @return matching last known location records
   */
  List<CcAndInferredLocationRecord> getAllLastKnownRecords(Map<CcAndInferredLocationFilter, String> filter);
  
  CcAndInferredLocationRecord getLastKnownRecordForVehicle(Integer vehicleId) throws Exception;
  
}
