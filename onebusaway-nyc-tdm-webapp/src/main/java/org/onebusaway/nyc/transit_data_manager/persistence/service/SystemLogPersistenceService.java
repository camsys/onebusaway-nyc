package org.onebusaway.nyc.transit_data_manager.persistence.service;

import org.onebusaway.nyc.transit_data_manager.logging.SystemLogRecord;

public interface SystemLogPersistenceService {
	void saveLogRecord(SystemLogRecord logRecord);
}
