package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

public interface CcLocationReportDao extends LocationDao {

  void saveOrUpdateReport(CcLocationReportRecord report);

  void saveOrUpdateReports(CcLocationReportRecord... reports);

}
