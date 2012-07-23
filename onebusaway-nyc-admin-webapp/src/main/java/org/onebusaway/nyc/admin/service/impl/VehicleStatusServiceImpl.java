package org.onebusaway.nyc.admin.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.onebusaway.nyc.admin.model.json.VehicleLastKnownRecord;
import org.onebusaway.nyc.admin.model.json.VehiclePullout;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.admin.service.VehicleStatusService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;

/**
 * Default implementation of {@link VehicleStatusService}
 * @author abelsare
 *
 */
public class VehicleStatusServiceImpl implements VehicleStatusService {

	private static Logger log = LoggerFactory.getLogger(VehicleStatusServiceImpl.class);
	private static final String DEFAULT_OPERATIONAL_API_HOST = "archive.dev.obanyc.com";
	
	private ConfigurationService configurationService;
	private RemoteConnectionService remoteConnectionService;
	private final ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public List<VehicleStatus> getVehicleStatus() {
		
		List<VehicleStatus> vehicleStatusRecords = new ArrayList<VehicleStatus>();
		
		//get last known record data from operational API
		List<VehicleLastKnownRecord> vehicleLastKnownRecords = getLastKnownRecordData();

		//get vehicle pipo data
		Map<String, VehiclePullout> vehiclePullouts = getPulloutData();
		
		
		//Build vehicle status objects by getting the required fields from both collections
		for(VehicleLastKnownRecord lastknownRecord : vehicleLastKnownRecords) {
			VehiclePullout pullout = vehiclePullouts.get(lastknownRecord.getVehicleId());
			VehicleStatus vehicleStatus = buildVehicleStatus(pullout, lastknownRecord);
			vehicleStatusRecords.add(vehicleStatus);
		}
		
		return vehicleStatusRecords;
	}
	
	private Map<String, VehiclePullout> getPulloutData() {
		String tdmHost = System.getProperty("tdm.host");
		Map<String, VehiclePullout> pullouts = new HashMap<String, VehiclePullout>();

		String url = buildURL(tdmHost, "/pullouts/list");
		log.debug("making request for : " +url);

		String vehiclePipocontent = remoteConnectionService.getContent(url);

		String json = extractJsonArrayString(vehiclePipocontent);
		
		try {
			JSONArray pulloutContentArray = new JSONArray("[" +json + "]");
			for(int i=0; i<pulloutContentArray.length(); i++) {
				VehiclePullout pullout = convertToObject(pulloutContentArray.getString(i), VehiclePullout.class);
				//pullout can be null if no data is returned by web service call
				if(pullout !=null) {
					pullouts.put(pullout.getVehicleId(), pullout);
				}
			}
		} catch (JSONException e) {
			log.error("Error parsing json content : " +e);
			e.printStackTrace();
		}
		
		return pullouts;
	}
	
	private List<VehicleLastKnownRecord> getLastKnownRecordData() {
		List<VehicleLastKnownRecord> lastKnownRecords = new ArrayList<VehicleLastKnownRecord>();
		
		String operationalAPIHost = null;
		
		try {
			operationalAPIHost = configurationService.getConfigurationValueAsString("operational-api.host", DEFAULT_OPERATIONAL_API_HOST);
		} catch(RemoteConnectFailureException e) {
			log.error("Failed retrieving operational API host from TDM. Setting to default value");
			operationalAPIHost = DEFAULT_OPERATIONAL_API_HOST;
		}
		
		String url = buildURL(operationalAPIHost, "/record/last-known/list");
		log.debug("making request for : " +url);
		
		String lastknownContent = remoteConnectionService.getContent(url);
		lastknownContent = lastknownContent.replace(System.getProperty("line.separator"), "");
		
		String json = extractJsonArrayString(lastknownContent);
		
		try {
			JSONArray lastKnownContentArray = new JSONArray("[" +json + "]");
			for(int i=0; i<lastKnownContentArray.length(); i++) {
				VehicleLastKnownRecord lastKnownRecord = convertToObject(lastKnownContentArray.getString(i), VehicleLastKnownRecord.class);
				//lastknownrecord can be null if no data is returned by web service call
				if(lastKnownRecord != null) {
					lastKnownRecords.add(lastKnownRecord);
				}
			}
		} catch (JSONException e) {
			log.error("Error parsing json content : " +e);
			e.printStackTrace();
		}
		
		return lastKnownRecords;
	}

	private <T> T convertToObject(String content, Class<T> objectType) {
		T object = null;
		try {
			object = mapper.readValue(content, objectType);

		} catch (JsonParseException e) {
			log.error("Error parsing json content : " +e);
			e.printStackTrace();
		} catch (JsonMappingException e) {
			log.error("Error parsing json content : " +e);
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Error parsing json content : " +e);
			e.printStackTrace();
		}
		return object;
	}
	
	private String extractJsonArrayString(String content) {
		String json = null;
		final Pattern pattern = Pattern.compile("\\[(.+?)\\]");
		final Matcher matcher = pattern.matcher(content);
		if(matcher.find() ) {
			json = matcher.group(1);
		}
		return json;
	}
	
	private String buildURL(String host, String api) {
		 return "http://" + host + "/api" + api;
	}
	
	private VehicleStatus buildVehicleStatus(VehiclePullout pullout, VehicleLastKnownRecord lastknownRecord) {
		VehicleStatus vehicleStatus = new VehicleStatus();
		if(pullout != null) {
			vehicleStatus.setPullinTime(extractTime(pullout.getPullinTime()));
			vehicleStatus.setPulloutTime(extractTime(pullout.getPulloutTime()));
		}
		vehicleStatus.setVehicleId(lastknownRecord.getVehicleId());
		
		String inferredDestination = getInferredDestination(lastknownRecord);
		vehicleStatus.setInferredDestination(inferredDestination);
				
		vehicleStatus.setInferredState(getInferredState(lastknownRecord));
		vehicleStatus.setObservedDSC(lastknownRecord.getDestinationSignCode());
		return vehicleStatus;
	}

	private String getInferredState(VehicleLastKnownRecord lastknownRecord) {
		String inferredPhase = lastknownRecord.getInferredPhase();
		String inferredState = inferredPhase;
		if(inferredPhase.startsWith("IN")) {
			inferredState = "IN PROGRESS";
		}
		if(inferredPhase.startsWith("DEAD")) {
			inferredState = "DEADHEAD";
		}
		if(inferredPhase.startsWith("LAY")) {
			inferredState = "LAYOVER";
		}
		
		return inferredState;
	}

	private String getInferredDestination(
			VehicleLastKnownRecord lastknownRecord) {
		StringBuilder inferredDestination = new StringBuilder();
		//all these fields can be blank
		if(StringUtils.isNotBlank(lastknownRecord.getInferredDSC())) {
			inferredDestination.append(lastknownRecord.getInferredDSC() + ":");
		}
		if(StringUtils.isNotBlank(lastknownRecord.getInferredRunId())) {
			inferredDestination.append(lastknownRecord.getInferredRunId().split("-")[0]);
			inferredDestination.append(" Direction: ");
		}
		if(StringUtils.isNotBlank(lastknownRecord.getInferredDirectionId())) {
			inferredDestination.append(lastknownRecord.getInferredDirectionId());
		}
		return inferredDestination.toString();
	}
	
	private String extractTime(String date) {
		DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
		DateTime dateTime = formatter.parseDateTime(date);
		int hour = dateTime.getHourOfDay();
		String formattedHour = String.format("%02d", hour);
		int minute = dateTime.getMinuteOfHour();
		String formattedMinute = String.format("%02d", minute);
		return formattedHour + ":" +formattedMinute;
	}
	

	/**
	 * Injects configuration service
	 * @param configurationService the configurationService to set
	 */
	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	/**
	 * Injects remote connection service
	 * @param remoteConnectionService the remoteConnectionService to set
	 */
	@Autowired
	public void setRemoteConnectionService(
			RemoteConnectionService remoteConnectionService) {
		this.remoteConnectionService = remoteConnectionService;
	}

}
