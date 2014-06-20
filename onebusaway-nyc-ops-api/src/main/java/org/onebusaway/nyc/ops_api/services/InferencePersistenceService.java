package org.onebusaway.nyc.ops_api.services;

import org.onebusaway.nyc.ops_api.model.ArchivedInferredLocationRecord;

public interface InferencePersistenceService {

  public void persist(ArchivedInferredLocationRecord record, String contents);
}
