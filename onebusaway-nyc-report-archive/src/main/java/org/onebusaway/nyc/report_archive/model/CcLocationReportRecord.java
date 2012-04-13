package org.onebusaway.nyc.report_archive.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;

import tcip_final_3_0_5_1.CcLocationReport;
import tcip_final_3_0_5_1.SPDataQuality;

@Entity
@Table(name = "obanyc_cclocationreport")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class CcLocationReportRecord implements Serializable {

	/**
   * 
   */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(generator = "increment")
	@GenericGenerator(name="increment", strategy="increment")
	@AccessType("property")
	private Long id;

	@Index(name = "UUID")
	@Column(nullable = false, name = "UUID", length = 36)
	private String uuid;

	@Column(nullable = false, name = "request_id")
	private Integer requestId;

	@Column(nullable = false, name = "vehicle_id")
	@Index(name = "vehicle_id")
	private Integer vehicleId;

	@Column(nullable = false, name = "vehicle_agency_id")
	private Integer vehicleAgencyId;

	@Column(nullable = false, name = "vehicle_agency_designator", length = 64)
	private String vehicleAgencyDesignator;

	@Column(nullable = false, name = "time_reported")
	@Index(name = "time_reported")
	private Date timeReported;

	// this is the system time received -- when the queue first saw it
	@Column(nullable = false, name = "time_received")
	@Index(name = "time_received")
	private Date timeReceived;

	@Column(nullable = false, name = "archive_time_received")
	@Index(name = "archive_time_received")
	private Date archiveTimeReceived;

	@Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "latitude")
	private BigDecimal latitude;

	@Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "longitude")
	private BigDecimal longitude;

	@Column(nullable = false, columnDefinition = "DECIMAL(5,2)", name = "direction_deg")
	private BigDecimal directionDeg;

	@Column(nullable = false, columnDefinition = "DECIMAL(4,1)", name = "speed")
	private BigDecimal speed;

	@Column(name = "data_quality_qualitative_indicator")
	private Byte dataQuality;

	@Column(nullable = false, name = "manufacturer_data", length = 64)
	private String manufacturerData;

	@Column(nullable = false, name = "operator_id_designator", length = 16)
	private String operatorIdDesignator;

	@Column(nullable = false, name = "run_id_designator", length = 32)
	private String runIdDesignator;

	@Column(nullable = false, name = "dest_sign_code")
	private Integer destSignCode;

	@Column(name = "emergency_code", length = 1)
	private String emergencyCode;

	@Column(nullable = false, name = "route_id_designator", length = 16)
	private String routeIdDesignator;

	@Column(name = "nmea_sentence_gpgga", length = 160)
	private String nmeaSentenceGPGGA;

	@Column(name = "nmea_sentence_gprmc", length = 160)
	private String nmeaSentenceGPRMC;

	@Column(nullable = false, name = "raw_message", length = 1400)
	private String rawMessage;

	public CcLocationReportRecord() {
	}

	public CcLocationReportRecord(RealtimeEnvelope envelope, String contents,
			String zoneOffset) {
		super();
		if (envelope == null || envelope.getCcLocationReport() == null)
			return; // deserialization failure, abort
		setUUID(envelope.getUUID());
		CcLocationReport message = envelope.getCcLocationReport();

		setRequestId((int) message.getRequestId());

		// Data Quality requires special handling
		SPDataQuality quality = message.getDataQuality();
		Byte qualityValue = null;
		if (quality != null) {
			String indicator = quality.getQualitativeIndicator();

			if (indicator != null) {
				qualityValue = new Byte(indicator);
			}
		}
		setDataQuality(qualityValue);

		setDestSignCode(message.getDestSignCode().intValue());
		setDirectionDeg(message.getDirection().getDeg());
		setLatitude(convertMicrodegreesToDegrees(message.getLatitude()));
		setLongitude(convertMicrodegreesToDegrees(message.getLongitude()));
		setManufacturerData(message.getManufacturerData());
		setOperatorIdDesignator(message.getOperatorID().getDesignator());
		setRouteIdDesignator(message.getRouteID().getRouteDesignator());
		setRunIdDesignator(message.getRunID().getDesignator());
		setSpeed(convertSpeed(message.getSpeed()));
		setTimeReported(convertTime(message.getTimeReported(), zoneOffset));
		setArchiveTimeReceived(new Date(System.currentTimeMillis()));
		setTimeReceived(new Date(envelope.getTimeReceived()));
		setVehicleAgencyDesignator(message.getVehicle().getAgencydesignator());
		setVehicleAgencyId(message.getVehicle().getAgencyId().intValue());
		setVehicleId((int) message.getVehicle().getVehicleId());

		// Check for localCcLocationReport and extract sentences if available
		String gpggaSentence = null;
		String gprmcSentence = null;
		if ((message.getLocalCcLocationReport() != null)
				&& (message.getLocalCcLocationReport().getNMEA() != null)
				&& (message.getLocalCcLocationReport().getNMEA().getSentence() != null)) {
			for (String s : message.getLocalCcLocationReport().getNMEA()
					.getSentence()) {
				if (s != null) {
					if (s.indexOf("$GPGGA") != -1) {
						gpggaSentence = s;
					} else if (s.indexOf("$GPRMC") != -1) {
						gprmcSentence = s;
					}
				}
			}
		}
		setNmeaSentenceGPGGA(gpggaSentence);
		setNmeaSentenceGPRMC(gprmcSentence);
		// Check this.
		setRawMessage(contents);
	}

	/**
	 * Timestamp of the on-board device when this message is created, in
	 * standard XML timestamp format "time-reported":
	 * "2011-06-22T10:58:10.0-00:00" Package private for unit tests.
	 */
	Date convertTime(String timeString, String zoneOffset) {
		if (timeString == null) {
			return null;
		}
		// some times the date doesn't include UTC
		// 2011-08-06T10:40:38.825, we have to assume these are in
		// local, i.e. EST time and convert appropriately
		DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		formatter.withZone(DateTimeZone.UTC);

		if (timeString.length() > 20 && timeString.length() < 24) {
			// append correct offset
			timeString = timeString + zoneOffset;
		}
		return new Date(formatter.parseDateTime(timeString).getMillis());
	}

	// Instantaneous speed. Per SAE J1587 speed is in half mph increments with
	// an offset of -15mph.
	// For example, 002.642 knots (from RMC) is 3.04 mph, so (15*2) + (3 * 2) =
	// 36
	// Valid range is [0,255], which equates to [-15mph, +112.5mph]

	/*
	 * From the TCIP documentation:
	 * 
	 * In accordance with J-1587, this data element is an unsigned byte whose
	 * value is expressed in half mile per hour increments with an offset to
	 * allow backing up to be expressed. A value of 0 indicates a speed of
	 * -15mph, a value of 1 indicates -14.5mph and so on. Values in excess of
	 * 112.5 mph cannot be expressed.
	 */

	/*
	 * Basically this means the following, where 'speed' (little 's') is the
	 * J1587 value and 'Speed' (big 's') is the actual speed value (in mph):
	 * 
	 * Speed = (speed - 30) / 2
	 * 
	 * speed = (Speed * 2) + 30
	 * 
	 * For this method, we want the first of those equations.
	 */
	private BigDecimal convertSpeed(short saeSpeed) {
		BigDecimal noOffsetSaeSpeed = new BigDecimal(saeSpeed - 30);

		return noOffsetSaeSpeed.divide(new BigDecimal(2));
	}

	private BigDecimal convertMicrodegreesToDegrees(int latlong) {
		return new BigDecimal(latlong * Math.pow(10.0, -6));
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	public Integer getRequestId() {
		return requestId;
	}

	public void setRequestId(Integer requestId) {
		this.requestId = requestId;
	}

	public Integer getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(Integer vehicleId) {
		this.vehicleId = vehicleId;
	}

	public Integer getVehicleAgencyId() {
		return vehicleAgencyId;
	}

	public void setVehicleAgencyId(Integer vehicleAgencyId) {
		this.vehicleAgencyId = vehicleAgencyId;
	}

	public String getVehicleAgencyDesignator() {
		return vehicleAgencyDesignator;
	}

	public void setVehicleAgencyDesignator(String vehicleAgencyDesignator) {
		this.vehicleAgencyDesignator = vehicleAgencyDesignator;
	}

	public Date getTimeReported() {
		return timeReported;
	}

	public void setTimeReported(Date timeReported) {
		this.timeReported = timeReported;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	public BigDecimal getDirectionDeg() {
		return directionDeg;
	}

	public void setDirectionDeg(BigDecimal directionDeg) {
		this.directionDeg = directionDeg;
	}

	public BigDecimal getSpeed() {
		return speed;
	}

	public void setSpeed(BigDecimal speed) {
		this.speed = speed;
	}

	public Byte getDataQuality() {
		return dataQuality;
	}

	public void setDataQuality(Byte dataQuality) {
		this.dataQuality = dataQuality;
	}

	public String getManufacturerData() {
		return manufacturerData;
	}

	public void setManufacturerData(String manufacturerData) {
		this.manufacturerData = manufacturerData;
	}

	public String getOperatorIdDesignator() {
		return operatorIdDesignator;
	}

	public void setOperatorIdDesignator(String operatorIdDesignator) {
		this.operatorIdDesignator = operatorIdDesignator;
	}

	public String getRunIdDesignator() {
		return runIdDesignator;
	}

	public void setRunIdDesignator(String runIdDesignator) {
		this.runIdDesignator = runIdDesignator;
	}

	public Integer getDestSignCode() {
		return destSignCode;
	}

	public void setDestSignCode(Integer destSignCode) {
		this.destSignCode = destSignCode;
	}

	public String getEmergencyCode() {
		return emergencyCode;
	}

	public void setEmergencyCode(String emergencyCode) {
		this.emergencyCode = emergencyCode;
	}

	public String getRouteIdDesignator() {
		return routeIdDesignator;
	}

	public void setRouteIdDesignator(String routeIdDesignator) {
		this.routeIdDesignator = routeIdDesignator;
	}

	public String getNmeaSentenceGPGGA() {
		return nmeaSentenceGPGGA;
	}

	public void setNmeaSentenceGPGGA(String nmeaSentenceGPGGA) {
		this.nmeaSentenceGPGGA = nmeaSentenceGPGGA;
	}

	public String getNmeaSentenceGPRMC() {
		return nmeaSentenceGPRMC;
	}

	public void setNmeaSentenceGPRMC(String nmeaSentenceGPRMC) {
		this.nmeaSentenceGPRMC = nmeaSentenceGPRMC;
	}

	public Date getTimeReceived() {
		return timeReceived;
	}

	public void setTimeReceived(Date timeReceived) {
		this.timeReceived = timeReceived;
	}

	public Date getArchiveTimeReceived() {
		return archiveTimeReceived;
	}

	public void setArchiveTimeReceived(Date archiveTimeReceived) {
		this.archiveTimeReceived = archiveTimeReceived;
	}

	public String getRawMessage() {
		return rawMessage;
	}

	public void setRawMessage(String rawMessage) {
		this.rawMessage = rawMessage;
	}
}
