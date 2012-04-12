package org.onebusaway.nyc.report_archive.services;

import java.util.List;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;

public interface NycQueuedInferredLocationDao {

  void queueForUpdate(ArchivedInferredLocationRecord record);
  
  void saveOrUpdateRecord(ArchivedInferredLocationRecord record);

  void saveOrUpdateRecords(ArchivedInferredLocationRecord... records);

  List<CcAndInferredLocationRecord> getAllLastKnownRecords();
  
  CcAndInferredLocationRecord getLastKnownRecordForVehicle(Integer vehicleId) throws Exception;
}
