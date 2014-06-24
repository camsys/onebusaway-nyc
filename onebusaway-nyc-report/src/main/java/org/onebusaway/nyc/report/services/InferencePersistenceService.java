package org.onebusaway.nyc.report.services;

import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;

public interface InferencePersistenceService {

  public void persist(ArchivedInferredLocationRecord record, String contents);
}
