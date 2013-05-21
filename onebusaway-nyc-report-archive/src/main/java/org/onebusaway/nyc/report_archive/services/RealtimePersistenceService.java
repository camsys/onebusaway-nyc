package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

public interface RealtimePersistenceService {

  void persist(CcLocationReportRecord record);

}
