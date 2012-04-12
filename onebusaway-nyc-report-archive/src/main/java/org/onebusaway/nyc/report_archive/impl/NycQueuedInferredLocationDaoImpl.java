package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.model.InferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class NycQueuedInferredLocationDaoImpl implements
		NycQueuedInferredLocationDao {

	protected static Logger _log = LoggerFactory
			.getLogger(NycQueuedInferredLocationDaoImpl.class);
	public static final int BATCH_SIZE = 1000;
	private HibernateTemplate _template;
	private List<ArchivedInferredLocationRecord> reports = Collections.synchronizedList(new ArrayList<ArchivedInferredLocationRecord>());
	private int _batchCount = 0;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		_template = new HibernateTemplate(sessionFactory);
	}

	public HibernateTemplate getHibernateTemplate() {
		return _template;
	}

	public void queueForUpdate(ArchivedInferredLocationRecord report) {
		_batchCount++;
		reports.add(report);
		if (_batchCount == BATCH_SIZE) {
			// clear from level one cache
			saveOrUpdateRecords(reports.toArray(new ArchivedInferredLocationRecord[0]));
			reports.clear();
			_batchCount = 0;
		}
	}
	
	@Transactional(rollbackFor = Throwable.class)
	@Override
	public void saveOrUpdateRecord(ArchivedInferredLocationRecord record) {
		_batchCount++;
		_template.saveOrUpdate(record);

		InferredLocationRecord currentRecord = new InferredLocationRecord(
				record);

		_template.saveOrUpdate(currentRecord);
		if (_batchCount == BATCH_SIZE) {
			// clear from level on cache
			_log.warn("inference flush");
			_template.flush();
			_template.clear();
			_batchCount = 0;
		}
	}

	public void saveOrUpdateRecords(ArchivedInferredLocationRecord... records) {
		List<ArchivedInferredLocationRecord> list = new ArrayList<ArchivedInferredLocationRecord>(
				records.length);
		for (ArchivedInferredLocationRecord record : records)
			list.add(record);
		_template.saveOrUpdateAll(list);

		List<InferredLocationRecord> currentRecords = new ArrayList<InferredLocationRecord>();
		for (ArchivedInferredLocationRecord record : records) {
			InferredLocationRecord currentRecord = new InferredLocationRecord(
					record);
			currentRecords.add(currentRecord);
		}
		_template.saveOrUpdateAll(currentRecords);
		_template.flush();
		_template.clear();
	}

	@Override
	public List<CcAndInferredLocationRecord> getAllLastKnownRecords() {

		List<CcAndInferredLocationRecord> firstArchivedRecord = new ArrayList<CcAndInferredLocationRecord>();
		/*
		 * here we do a join for real and inferred data based on vehicle id and
		 * UID, and then join against the current record pointer to retrieve the
		 * single last known record for that bus
		 */
		String hql = "select inferenceRecord, bhsRecord "
				+ "from InferredLocationRecord map, "
				+ "CcLocationReportRecord bhsRecord, "
				+ "ArchivedInferredLocationRecord inferenceRecord "
				+ "where map.currentRecord = inferenceRecord "
				+ "and map.vehicleId = bhsRecord.vehicleId "
				+ "and map.currentRecord.uuid = bhsRecord.uuid "
				+ "order by map.vehicleId";

		List<Object[]> list = _template.find(hql);
		// our join will return a list of object arrays now, in the order
		// we selected above
		for (Object[] o : list) {
			CcAndInferredLocationRecord record = new CcAndInferredLocationRecord(
					(ArchivedInferredLocationRecord) o[0],
					(CcLocationReportRecord) o[1]);
			firstArchivedRecord.add(record);
		}
		return firstArchivedRecord;
	}

	@Override
	public CcAndInferredLocationRecord getLastKnownRecordForVehicle(
			Integer vehicleId) throws Exception {

		CcAndInferredLocationRecord rec = null;

		if (vehicleId == null) {
			return null;
		}

		String hql = "select inferenceRecord, bhsRecord "
				+ "from InferredLocationRecord map, "
				+ "CcLocationReportRecord bhsRecord, "
				+ "ArchivedInferredLocationRecord inferenceRecord "
				+ "where map.currentRecord = inferenceRecord "
				+ "and map.vehicleId = bhsRecord.vehicleId "
				+ "and map.currentRecord.uuid = bhsRecord.uuid "
				+ "and map.vehicleId = ?";

		List<Object[]> list = _template.find(hql, vehicleId);

		if (list.size() > 1) {
			throw new Exception(
					"Query for a single latest record of a single vehicle returned multiple records");
		} else if (list.size() == 0) {
			rec = null;
		} else {
			List<CcAndInferredLocationRecord> firstArchivedRecords = new ArrayList<CcAndInferredLocationRecord>();
			for (Object[] o : list) {
				CcAndInferredLocationRecord record = new CcAndInferredLocationRecord(
						(ArchivedInferredLocationRecord) o[0],
						(CcLocationReportRecord) o[1]);
				firstArchivedRecords.add(record);
			}
			rec = firstArchivedRecords.get(0);
		}

		return rec;
	}
}