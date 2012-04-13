package org.onebusaway.nyc.report_archive.queue;

import org.codehaus.jackson.map.DeserializationConfig;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.CcLocationReportDao;
import org.onebusaway.nyc.vehicle_tracking.impl.queue.InputQueueListenerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ArchivingInputQueueListenerTask extends InputQueueListenerTask {

	public static final int BATCH_SIZE = 1000;
	public static final int COUNT_INTERVAL = 6000;
	protected static Logger _log = LoggerFactory
			.getLogger(ArchivingInputQueueListenerTask.class);

	@Autowired
	private CcLocationReportDao _dao;

	// offset of timezone (-04:00 or -05:00)
	private String _zoneOffset = null;
	private String _systemTimeZone = null;
	private int _batchCount = 0;
	private int count = 0;
	private long dbSum = 0;
	private long dbStart = System.currentTimeMillis();
	private List<CcLocationReportRecord> reports = Collections.synchronizedList(new ArrayList<CcLocationReportRecord>());
	@Override
	// this method can't throw exceptions or it will stop the queue
	// listening
	public boolean processMessage(String address, String contents) {
		RealtimeEnvelope envelope = null;
		try {
			envelope = deserializeMessage(contents);
			count++;

			if (envelope == null || envelope.getCcLocationReport() == null) {
				_log.error("Message discarded, probably corrupted, contents= "
						+ contents);
				Exception e = new Exception(
						"deserializeMessage failed, possible corrupted message.");
				_dao.handleException(contents, e, null);
				return false;
			}

			CcLocationReportRecord record = new CcLocationReportRecord(
					envelope, contents, getZoneOffset());
			if (record != null) {
				long dbStart = System.currentTimeMillis();
				_batchCount++;
				reports.add(record);
				if (_batchCount == BATCH_SIZE) {
					_dao.saveOrUpdateReports(reports.toArray(new CcLocationReportRecord[0]));
					reports.clear();
					_batchCount = 0;
				}

				dbSum += System.currentTimeMillis() - dbStart;
			}
			// re-calculate zoneOffset periodically
			if (count > COUNT_INTERVAL) {
				_log.warn("realtime queue processed " + count + " messages in "
						+ (System.currentTimeMillis() - dbStart)
						+ ", db time was " + dbSum);
				_zoneOffset = null;
				if (record != null) {
					long delta = System.currentTimeMillis()
							- record.getTimeReceived().getTime();
					if (delta > 2000) {
						_log.warn("realtime queue is " + delta
								+ " millis behind");
					}
				}
				count = 0;
				dbSum = 0;
				dbStart = System.currentTimeMillis();
			}
		} catch (Throwable t) {
			_log.error("Exception processing contents= " + contents, t);
			try {
				Date timeReceived = null;
				if (envelope != null)
					timeReceived = new Date(envelope.getTimeReceived());
				_dao.handleException(contents, t, timeReceived);
			} catch (Throwable tt) {
				// we tried
				_log.error("Exception handling exception= " + tt);
			}
		}

		return true;
	}

	@PostConstruct
	public void setup() {
		super.setup();
		// make parsing lenient
		_mapper.configure(
				DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// set a reasonable default
		_systemTimeZone = _configurationService.getConfigurationValueAsString(
				"archive.systemTimeZone", "America/New_York");
	}

	@PreDestroy
	public void destroy() {
		super.destroy();
	}

	/**
	 * Return the offset in an tz-offset string fragment ("-04:00" or "-5:00")
	 * based on the daylight savings rules in effect during the given date. This
	 * method assumes timezone is a standard hour boundary away from GMT.
	 * 
	 * Package private for unit tests.
	 * 
	 * @param date
	 *            when to consider the zoneoffset. Now makes the most sense, but
	 *            can be historical/future for unit testing
	 * @param systemTimeZone
	 *            the java string representing a timezone, such as
	 *            "America/New_York"
	 */
	String getZoneOffset(Date date, String systemTimeZone) {
		if (date == null)
			return null;
		// cache _zoneOffset
		if (_zoneOffset == null) {
			long millisecondOffset;
			// use systemTimeZone if available
			if (systemTimeZone != null) {
				millisecondOffset = TimeZone.getTimeZone(systemTimeZone)
						.getOffset(date.getTime());
			} else {
				// use JVM default otherwise
				millisecondOffset = TimeZone.getDefault().getOffset(
						date.getTime());
			}
			String plusOrMinus = (millisecondOffset <= 0 ? "-" : "+");
			if (millisecondOffset == 0) {
				_zoneOffset = plusOrMinus + "00:00";
			} else {
				// format 1st arg 0-padded to a width of 2
				_zoneOffset = plusOrMinus
						+ String.format("%1$02d",
								Math.abs(millisecondOffset / (1000 * 60 * 60)))
						+ ":00";
			}
		}
		return _zoneOffset;

	}

	private String getZoneOffset() {
		return getZoneOffset(new Date(), _systemTimeZone);
	}

}
