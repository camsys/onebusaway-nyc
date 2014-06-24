package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report.services.LocationDao;

public interface CcLocationReportDao extends LocationDao {

  void saveOrUpdateReport(CcLocationReportRecord report);

  void saveOrUpdateReports(CcLocationReportRecord... reports);

}
