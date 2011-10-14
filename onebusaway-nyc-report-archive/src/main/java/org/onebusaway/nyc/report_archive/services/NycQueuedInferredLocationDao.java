package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.NycQueuedInferredLocationRecord;

public interface NycQueuedInferredLocationDao {

  void saveOrUpdateRecord(NycQueuedInferredLocationRecord record);

  void saveOrUpdateRecords(NycQueuedInferredLocationRecord... records);

}
