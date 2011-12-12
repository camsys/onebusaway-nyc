package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import java.util.Date;

public interface CcLocationReportDao {

  void saveOrUpdateReport(CcLocationReportRecord report);

  void saveOrUpdateReports(CcLocationReportRecord... reports);

  void handleException(String content, Throwable error, Date timeReceived);

}
