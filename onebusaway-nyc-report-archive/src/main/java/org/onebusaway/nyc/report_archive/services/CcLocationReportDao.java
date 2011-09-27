package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

public interface CcLocationReportDao {

  void saveOrUpdateReport(CcLocationReportRecord report);

  void saveOrUpdateReports(CcLocationReportRecord... reports);

}
