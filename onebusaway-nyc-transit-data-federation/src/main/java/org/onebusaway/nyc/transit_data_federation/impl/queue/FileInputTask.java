package org.onebusaway.nyc.transit_data_federation.impl.queue;

import java.io.FileReader;
import java.io.Reader;
import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Populate the TDS from a JSON file. The expected input looks like this:
 * 
 * <pre>
 *  {'status': 'default', 'distanceAlongBlock': None, 'tripId': None, 'vehicleId': 'MTA NYCT_339', 'blockId': None, 'recordTimestamp': 1372455001000L, 'inferredLatitude': '40.652985', 'serviceDate': 0L, 'phase': 'DEADHEAD_BEFORE', 'inferredLongitude': '-73.999039'} {'status': 'default', 'distanceAlongBlock': 22472.939858083413, 'tripId': 'MTA NYCT_20130407CA_100300_SBS12_0103_SBS12_228', 'vehicleId': 'MTA NYCT_1231', 'blockId': 'MTA NYCT_BX_20130407CA_C_GH_55560_SBS12-228', 'recordTimestamp': 1372455001000L, 'inferredLatitude': '40.858017', 'serviceDate': 1372392000000L, 'phase': 'DEADHEAD_DURING', 'inferredLongitude': '-73.826607'}
 * </pre>
 * 
 * Sadly this is not newline delimited, but that can easily be changed.
 * 
 */
public class FileInputTask {

	protected static Logger _log = LoggerFactory.getLogger(FileInputTask.class);

	@Autowired
	private VehicleLocationListener _vehicleLocationListener;

	private final ObjectMapper mapper = new ObjectMapper();

	private String filename = "/tmp/queue_log.json";
	private Reader inputReader = null;
	private StringBuffer currentRecord = new StringBuffer();

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@PostConstruct
	// TODO: do work on background thread
	public void execute() {
		_log.debug("in execute");

		try {
			_log.info("opening file=" + filename);
			open();
			_log.debug("file successfully opened");
			while (next()) {
				Record record = parseCurrentRecord();
				processResult(record);
			}
		} catch (Exception e) {
			_log.error("fail: ", e);
		} finally {
			_log.info("exiting");
			try {
				close();
			} catch (Exception buryIt) {
				// bury
			}
		}
	}

	// advance record pointer
	private boolean next() throws Exception {
		currentRecord = new StringBuffer();
		int i;
		while ((i = inputReader.read()) > 0) {
			char c = (char) i;
			if (c == '}') {
				currentRecord.append(c);
				// read last space
				inputReader.read();
				_log.debug("currentRecord=|" + currentRecord + "|");
				return true;
			} else {
				currentRecord.append(c);
			}
		}
		return false;
	}

	// parse the current json string into a record object
	private Record parseCurrentRecord() throws Exception {
		String s = preProcessRecord(currentRecord.toString());
		_log.info("s=|" + s + "|");
		Record record = mapper.readValue(s, Record.class);
		return postProcess(record);
	}

	// fixup common invalid JSON mistakes
	private String preProcessRecord(String s) {
		s = s.replace('\'', '"');
		s = s.replace("\"distanceAlongBlock\": None",
				"\"distanceAlongBlock\": \"NaN\"");
		s = s.replace("\"tripId\": None", "\"tripId\": null");
		s = s.replace("\"blockId\": None", "\"blockId\": null");
		s = s.replace("L, \"inferredLatitude\"", ", \"inferredLatitude\"");
		s = s.replace("L, \"phase\"", ", \"phase\"");
		return s;
	}

	// fixup invalid records to protect the TDS
	private Record postProcess(Record record) {
		final String NULL_RECORD = "None";
		if (record == null)
			return null;
		if (NULL_RECORD.equals(record.getDistanceAlongBlock()))
			record.setDistanceAlongBlock(Double.NaN);
		if (NULL_RECORD.equals(record.getTripId()))
			record.setTripId(null);
		if (NULL_RECORD.equals(record.getBlockId()))
			record.setBlockId(null);
		if (0L == record.getServiceDate())
			record.setServiceDate(System.currentTimeMillis());
		return record;
	}

	
	// populate the VLR and pass to the TDS
	private void processResult(Record record) {
		if (record == null)
			return;
		VehicleLocationRecord vlr = new VehicleLocationRecord();
		vlr.setVehicleId(AgencyAndIdLibrary.convertFromString(record
				.getVehicleId()));
		vlr.setTimeOfRecord(record.getRecordTimestamp());
		vlr.setTimeOfLocationUpdate(record.getTimeOfLocationUpdate());
		vlr.setBlockId(AgencyAndIdLibrary.convertFromString(record.getBlockId()));
		vlr.setTripId(AgencyAndIdLibrary.convertFromString(record.getTripId()));
		vlr.setServiceDate(record.getServiceDate());
		vlr.setDistanceAlongBlock(record.getDistanceAlongBlock());
		vlr.setCurrentLocationLat(record.getInferredLatitude());
		vlr.setCurrentLocationLon(record.getInferredLongitude());
		vlr.setPhase(record.getPhase());
		vlr.setStatus(record.getStatus());
		_vehicleLocationListener.handleVehicleLocationRecord(vlr);
	}

	// perform file handling
	private void open() throws Exception {
		inputReader = new FileReader(filename);
	}

	// perform file handling cleanup
	private void close() throws Exception {
		if (inputReader != null)
			inputReader.close();
	}

	/**
	 * Object representation of the expected JSON record. 
	 *
	 */
	private static class Record {

		private String vehicleId;
		private long recordTimestamp;
		private long timeOfLocationUpdate;
		private String blockId;
		private String tripId;
		private long serviceDate;
		private double distanceAlongBlock;
		private double lat;
		private double lon;
		private EVehiclePhase phase;
		private String status;

		public String getVehicleId() {
			return vehicleId;
		}

		public void setVehicleId(String vehicleId) {
			this.vehicleId = vehicleId;
		}

		public long getRecordTimestamp() {
			return recordTimestamp;
		}

		public void setRecordTimestamp(long timeOfRecord) {
			this.recordTimestamp = timeOfRecord;
		}

		public long getTimeOfLocationUpdate() {
			return timeOfLocationUpdate;
		}

		public void setTimeOfLocationUpdate(long timeOfLocationUpdate) {
			this.timeOfLocationUpdate = timeOfLocationUpdate;
		}

		public String getBlockId() {
			return blockId;
		}

		public void setBlockId(String blockId) {
			this.blockId = blockId;
		}

		public String getTripId() {
			return tripId;
		}

		public void setTripId(String tripId) {
			this.tripId = tripId;
		}

		public long getServiceDate() {
			return serviceDate;
		}

		public void setServiceDate(long serviceDate) {
			this.serviceDate = serviceDate;
		}

		public double getDistanceAlongBlock() {
			return distanceAlongBlock;
		}

		public void setDistanceAlongBlock(double distanceAlongBlock) {
			this.distanceAlongBlock = distanceAlongBlock;
		}

		public double getInferredLatitude() {
			return lat;
		}

		public void setInferredLatitude(double lat) {
			this.lat = lat;
		}

		public double getInferredLongitude() {
			return lon;
		}

		public void setInferredLongitude(double lon) {
			this.lon = lon;
		}

		public EVehiclePhase getPhase() {
			return phase;
		}

		public void setPhase(EVehiclePhase phase) {
			this.phase = phase;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}
}
