package org.onebusaway.nyc.report_archive.result;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.transform.ResultTransformer;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Transforms result of historical operational API query to {@link HistoricalRecord}
 * @author abelsare
 *
 */
public class HistoricalRecordResultTransformer implements ResultTransformer{

	private static final long serialVersionUID = 1L;

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		HistoricalRecord record = new HistoricalRecord();
		
		record.setVehicleAgencyId((String) tuple[0]); 
		record.setTimeReported(toISODate((Date)tuple[1]));
		record.setTimeReceived(toISODate((Date)tuple[2]));
		record.setOperatorIdDesignator((String)tuple[3]);
		record.setRouteIdDesignator((String)tuple[4]);
		record.setRunIdDesignator((String)tuple[5]);
		record.setDestSignCode((Integer)tuple[6]);
		record.setEmergencyCode((String)tuple[7]);
		record.setLatitude((BigDecimal)tuple[8]);
		record.setLongitude((BigDecimal)tuple[9]);
		record.setNmeaSentenceGPRMC((String)tuple[10]);
		record.setNmeaSentenceGPGGA((String)tuple[11]);
		record.setSpeed((BigDecimal)tuple[12]);
		record.setDirectionDeg((BigDecimal)tuple[13]);
		record.setVehicleId((Integer)tuple[14]);
		record.setManufacturerData((String)tuple[15]);
		record.setRequestId((Integer)tuple[16]);
		
		record.setDepotId((String)tuple[17]);
		record.setServiceDate(toISODate((Date)tuple[18]));
		record.setInferredRunId((String)tuple[19]);
		record.setAssignedRunId((String)tuple[20]);
		record.setInferredBlockId((String)tuple[21]);
		record.setInferredTripId((String)tuple[22]);
		record.setInferredRouteId((String)tuple[23]);
		record.setInferredDirectionId((String)tuple[24]);
		record.setInferredDestSignCode((Integer)tuple[25]);
		record.setInferredLatitude((BigDecimal)tuple[26]);
		record.setInferredLongitude((BigDecimal)tuple[27]);
		record.setInferredPhase((String)tuple[28]);
		record.setInferredStatus((String)tuple[29]);
		//Fix for OBANYC-1910. Boolean value cannot be null
		record.setInferenceIsFormal((Boolean)tuple[30] == null ? Boolean.FALSE : (Boolean)tuple[30]);
		record.setDistanceAlongBlock((Double)tuple[31]);
		record.setDistanceAlongTrip((Double)tuple[32]);
		record.setNextScheduledStopId((String)tuple[33]);
		record.setNextScheduledStopDistance((Double)tuple[34]);
		record.setScheduleDeviation((Integer)tuple[35]);
		
		return record;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List transformList(List collection) {
		return collection;
	}
	
	private String toISODate(Date date) {
		if (date != null) {
			return ISODateTimeFormat.dateTime().print(new DateTime(date));
		}
		return null;
	}

}
