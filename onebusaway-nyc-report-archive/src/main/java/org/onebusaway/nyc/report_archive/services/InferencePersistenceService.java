package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;

public interface InferencePersistenceService {

  public void persist(ArchivedInferredLocationRecord record, String contents);
}
