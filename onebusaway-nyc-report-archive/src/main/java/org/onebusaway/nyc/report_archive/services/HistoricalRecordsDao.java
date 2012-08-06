package org.onebusaway.nyc.report_archive.services;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.report_archive.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;

/**
 * Retrieve historical records from {@link CcLocationReportRecord} and {@link ArchivedInferredLocationRecord}
 * @author abelsare
 *
 */
public interface HistoricalRecordsDao {
	
	List<HistoricalRecord> getHistoricalRecords(Map<CcAndInferredLocationFilter, Object> filter);

}
