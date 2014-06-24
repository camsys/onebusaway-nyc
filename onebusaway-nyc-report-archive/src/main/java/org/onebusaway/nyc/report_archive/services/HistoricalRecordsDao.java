package org.onebusaway.nyc.report_archive.services;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.report.impl.CcAndInferredLocationFilter;
import org.onebusaway.nyc.report.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.result.HistoricalRecord;

/**
 * Retrieve historical records from {@link CcLocationReportRecord} and {@link ArchivedInferredLocationRecord}
 * @author abelsare
 *
 */
public interface HistoricalRecordsDao {
	
	/**
	 * Returns historical vehicle records matching the filter values
	 * @param filter filter that needs to be applied on results
	 * @return historical vehicle records matching filters
	 */
	List<HistoricalRecord> getHistoricalRecords(Map<CcAndInferredLocationFilter, Object> filter);

}
