package org.onebusaway.nyc.ops_api.services;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.ops_api.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.ops_api.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.ops_api.model.CcAndInferredLocationRecord;

public interface CcAndInferredLocationDao extends LocationDao {

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
