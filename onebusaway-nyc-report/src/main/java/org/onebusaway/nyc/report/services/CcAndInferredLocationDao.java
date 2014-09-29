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
