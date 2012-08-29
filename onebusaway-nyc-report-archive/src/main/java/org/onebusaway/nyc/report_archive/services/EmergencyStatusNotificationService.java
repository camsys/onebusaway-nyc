package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;

/**
 * Processes incoming bus records from the real time queue and sends notification to amazon sns service
 * if a bus is reporting emergency.
 * @author abelsare
 *
 */
public interface EmergencyStatusNotificationService {
	
	/**
	 * Process incoming record and check if it is reporting emergency. Dispatches an sns notification event
	 * if emergency status is reported.
	 * @param record incoming bus record
	 */
	void process(CcLocationReportRecord record);

}
