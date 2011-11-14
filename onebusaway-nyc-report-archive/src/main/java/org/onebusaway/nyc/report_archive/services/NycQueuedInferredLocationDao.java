package org.onebusaway.nyc.report_archive.services;

// import org.onebusaway.nyc.report_archive.model.NycQueuedInferredLocationRecord;

// public interface NycQueuedInferredLocationDao {

//   void saveOrUpdateRecord(NycQueuedInferredLocationRecord record);

//   void saveOrUpdateRecords(NycQueuedInferredLocationRecord... records);

// }

import java.util.List;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;

public interface NycQueuedInferredLocationDao {

  void saveOrUpdateRecord(ArchivedInferredLocationRecord record);

  void saveOrUpdateRecords(ArchivedInferredLocationRecord... records);

  List<ArchivedInferredLocationRecord> getAllLastKnownRecords();
}
