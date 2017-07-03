package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.CharMatcher;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

public abstract class InputServiceImpl {

	private static Logger _log = LoggerFactory
			.getLogger(InputServiceImpl.class);
	private String[] _depotPartitionKeys = null;
	private VehicleLocationInferenceService _vehicleLocationService;
	private VehicleAssignmentService _vehicleAssignmentService;
	private ObjectMapper _mapper;

	@Autowired
	public void setVehicleAssignmentService(
			VehicleAssignmentService vehicleAssignmentService) {
		_vehicleAssignmentService = vehicleAssignmentService;
	}

	@Autowired
	public void setVehicleLocationService(
			VehicleLocationInferenceService vehicleLocationService) {
		_vehicleLocationService = vehicleLocationService;
	}

	@SuppressWarnings("deprecation")
	@PostConstruct
	public void setup() {
		_mapper = new ObjectMapper();
		final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
		_mapper.getDeserializationConfig().setAnnotationIntrospector(jaxb);
	}

	public boolean processMessage(String address, byte[] buff) throws Exception {
		String contents = new String(buff);
		final RealtimeEnvelope message = deserializeMessage(contents);

		if (acceptMessage(message)) {
			_vehicleLocationService.handleRealtimeEnvelopeRecord(message);
			return true;
		}
		
		return false;
	}

	public RealtimeEnvelope deserializeMessage(String contents) {
		RealtimeEnvelope message = null;
		final String contentsPrintable = replaceNonPrintableCharacters(contents);
		final String contentsReplaced = replaceMessageContents(contentsPrintable);
		try {
			final JsonNode wrappedMessage = _mapper.readValue(contentsReplaced,
					JsonNode.class);
			final String ccLocationReportString = wrappedMessage.get(
					"RealtimeEnvelope").toString();
			message = _mapper.readValue(ccLocationReportString,
					RealtimeEnvelope.class);
		} catch (Exception e) {
			_log.warn("Received corrupted message from queue: ", e);
			_log.warn("Contents: " + contents);
			return null;
		}
		return message;
	}

	public String replaceNonPrintableCharacters(String contents) {
		return CharMatcher.JAVA_ISO_CONTROL.removeFrom(contents);
	}

	public abstract String replaceMessageContents(String contents);
	
	
	public boolean acceptMessage(RealtimeEnvelope envelope) {
		if (envelope == null || envelope.getCcLocationReport() == null)
			return false;

		final CcLocationReport message = envelope.getCcLocationReport();
		final ArrayList<AgencyAndId> vehicleList = new ArrayList<AgencyAndId>();

		if (_depotPartitionKeys == null)
			return false;

		for (final String key : _depotPartitionKeys) {
			try {
				vehicleList.addAll(_vehicleAssignmentService
						.getAssignedVehicleIdsForDepot(key));
			} catch (final Exception e) {
				_log.warn("Error fetching assigned vehicles for depot " + key
						+ "; will retry.");
				continue;
			}
		}

		final CPTVehicleIden vehicleIdent = message.getVehicle();
		final AgencyAndId vehicleId = new AgencyAndId(
				vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId()
						+ "");

		return vehicleList.contains(vehicleId);
	}
	
	public String getDepotPartitionKey() {
		final StringBuilder sb = new StringBuilder();
		for (final String key : _depotPartitionKeys) {
			if (sb.length() > 0)
				sb.append(",");
			sb.append(key);
		}
		return sb.toString();
	}

	public void setDepotPartitionKey(String depotPartitionKey) {
		_log.info("depotPartitionKey=" + depotPartitionKey);
		if (depotPartitionKey != null && !depotPartitionKey.isEmpty())
			_depotPartitionKeys = depotPartitionKey.split(",");
		else
			_depotPartitionKeys = null;
	}

	public ObjectMapper getMapper() {
		return _mapper;
	}

	public void setMapper(ObjectMapper _mapper) {
		this._mapper = _mapper;
	}
}
