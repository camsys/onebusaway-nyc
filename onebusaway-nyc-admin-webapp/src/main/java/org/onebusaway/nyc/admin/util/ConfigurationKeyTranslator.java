package org.onebusaway.nyc.admin.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates keys used in TDM configuration to the keys used in admin parameters UI and vice versa.
 * This is required to hide the actual keys used in tdm configuration from client.
 * @author abelsare
 *
 */
public class ConfigurationKeyTranslator {

	private Map<String, String> configToUIKeys;
	
	public ConfigurationKeyTranslator() {
		configToUIKeys = new HashMap<String, String>();
		
		configToUIKeys.put("tdm.crewAssignmentRefreshInterval", "tdmCrewAssignmentRefreshKey");
		configToUIKeys.put("tdm.vehicleAssignmentRefreshInterval", "tdmVehicleAssignmentRefreshKey");
		configToUIKeys.put("admin.senderEmailAddress", "adminSenderEmailAddressKey");
		configToUIKeys.put("admin.instanceId", "adminInstanceIdKey");
		configToUIKeys.put("data.gpsTimeSkewThreshold", "dataGpsTimeSkewKey");
		configToUIKeys.put("display.stalledTimeout", "displayStalledTimeoutKey");
		configToUIKeys.put("display.staleTimeout", "displayStaleTimeoutKey");
		configToUIKeys.put("display.offRouteDistance", "displayOffrouteDistanceKey");
		configToUIKeys.put("display.hideTimeout", "displayHideTimeoutKey");
		configToUIKeys.put("display.bingMapsKey", "displayBingMapsKey");
		configToUIKeys.put("display.previousTripFilterDistance", "displayPreviousFilterKey");
		configToUIKeys.put("display.googleAnalyticsSiteId", "displayGoogleAnalyticsIdKey");
		configToUIKeys.put("display.googleMapsClientId","displayGoogleMapsIdKey");
		configToUIKeys.put("display.googleMapsSecretKey", "displayGoogleMapsSecretKey");
		configToUIKeys.put("display.useTimePredictions", "displayUseTimePredictionsKey");
		configToUIKeys.put("inference-engine.inputQueueName", "ieInputQueueNameKey");
		configToUIKeys.put("inference-engine.outputQueueName", "ieOutputQueueNameKey");
		configToUIKeys.put("inference-engine.inputQueuePort", "ieInputQueuePortKey");
		configToUIKeys.put("inference-engine.outputQueuePort", "ieOutputQueuePortKey");
		configToUIKeys.put("inference-engine.inputQueueHost", "ieInputQueueHostKey");
		configToUIKeys.put("inference-engine.outputQueueHost", "ieOutputQueueHostKey");
		configToUIKeys.put("tds.inputQueueName", "tdsInputQueueNameKey");
		configToUIKeys.put("tds.inputQueuePort", "tdsInputQueuePortKey");
		configToUIKeys.put("tds.inputQueueHost", "tdsInputQueueHostKey");
		configToUIKeys.put("operational-api.host", "opsApiHostKey");
	}
	
	
	public String getTranslatedKey(String keyToTranslate) {
		return configToUIKeys.get(keyToTranslate);
	}

}
