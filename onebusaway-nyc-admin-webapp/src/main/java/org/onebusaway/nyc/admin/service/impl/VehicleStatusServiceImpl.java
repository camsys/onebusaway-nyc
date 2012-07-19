package org.onebusaway.nyc.admin.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
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
		
		//get vehicle pipo data
		List<VehiclePullout> vehiclePullouts = getPulloutData();
		
		//get last known record data
		Map<String, VehicleLastKnownRecord> vehicleLastKnownRecords = getLastKnownRecordData();
		
		//Build vehicle status objects by getting the required fields from both collections
		for(VehiclePullout pullout : vehiclePullouts) {
			VehicleStatus vehicleStatus = buildVehicleStatus(pullout, vehicleLastKnownRecords.get(pullout.getVehicleId()));
			vehicleStatusRecords.add(vehicleStatus);
		}
		
		return vehicleStatusRecords;
	}
	
	private List<VehiclePullout> getPulloutData() {
		String tdmHost = System.getProperty("tdm.host");
		List<VehiclePullout> vehiclePullouts = new ArrayList<VehiclePullout>();

		String url = buildURL(tdmHost, "/pullouts/list");
		log.debug("making request for : " +url);

		String vehiclePipocontent = remoteConnectionService.getContent(url);

		String json = extractJsonArrayString(vehiclePipocontent);
		
		try {
			JSONArray pulloutContentArray = new JSONArray("[" +json + "]");
			for(int i=0; i<pulloutContentArray.length(); i++) {
				VehiclePullout vehiclePullout = convertToObject(pulloutContentArray.getString(i), VehiclePullout.class);
				//pullout can be done if no data is returned by web service call
				if(vehiclePullout != null) {
					vehiclePullouts.add(vehiclePullout);
				}
			}
		} catch (JSONException e) {
			log.error("Error parsing json content : " +e);
			e.printStackTrace();
		}
		
		return vehiclePullouts;
	}

	private Map<String, VehicleLastKnownRecord> getLastKnownRecordData() {
		Map<String, VehicleLastKnownRecord> lastKnownRecords = new HashMap<String, VehicleLastKnownRecord>();

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
		
		String json = extractJsonArrayString(lastknownContent);

		try {
			JSONArray lastKnownContentArray = new JSONArray("[" +json + "]");
			for(int i=0; i<lastKnownContentArray.length(); i++) {
				VehicleLastKnownRecord lastKnownRecord = convertToObject(lastKnownContentArray.getString(i), VehicleLastKnownRecord.class);
				//lastknownRecord can be done if no data is returned by web service call
				if(lastKnownRecord !=null) {
					lastKnownRecords.put(lastKnownRecord.getVehicleId(), lastKnownRecord);
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
		vehicleStatus.setVehicleId(pullout.getVehicleId());
		vehicleStatus.setPullinTime(pullout.getPullinTime());
		vehicleStatus.setPulloutTime(pullout.getPulloutTime());
		if(lastknownRecord != null) {
			vehicleStatus.setInferredDSC(lastknownRecord.getInferredDSC());
			vehicleStatus.setInferredState(lastknownRecord.getInferredPhase());
			vehicleStatus.setObservedDSC(lastknownRecord.getDestinationSignCode());
			vehicleStatus.setRoute(lastknownRecord.getInferredRunId().split("-")[0]);
			vehicleStatus.setDirection(lastknownRecord.getInferredDirectionId());
		}
		return vehicleStatus;
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
