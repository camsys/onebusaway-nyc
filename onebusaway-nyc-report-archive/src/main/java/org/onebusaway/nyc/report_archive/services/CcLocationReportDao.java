package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.CcLocationReport;

public interface CcLocationReportDao {

  void saveOrUpdateReport(CcLocationReport report);

  void saveOrUpdateReports(CcLocationReport... reports);

}
