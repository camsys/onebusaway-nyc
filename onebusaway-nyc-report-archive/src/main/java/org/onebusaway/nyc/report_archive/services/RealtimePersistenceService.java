package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;

public interface RealtimePersistenceService {

  void persist(CcLocationReportRecord record);

}
