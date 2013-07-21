/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This component listens to the inference output queue and injects records into
 * the local TDF/TDS.
 * 
 * @author jmaki
 * @author altang
 * 
 */

public class InferenceInputQueueListenerTask extends InferenceQueueListenerTask {

	@Autowired
	private VehicleLocationListener _vehicleLocationListener;

	@Autowired
	private PredictionIntegrationService _predictionIntegrationService;

	private boolean useTimePredictions = false;
	private boolean checkAge = false;
	private int ageLimit = 300;
	private boolean refreshCheck;

	public InferenceInputQueueListenerTask() {
		setConfigurationService(new ConfigurationServiceImpl());
		refreshCache();
	}

	public String getQueueDisplayName() {
		return "InferenceInputQueueListenerTask";
	}

	protected void setPredictionIntegrationService( PredictionIntegrationService predictionIntegrationService) {
		_predictionIntegrationService = predictionIntegrationService;
	}

	protected void setVehicleLocationListener( VehicleLocationListener vehicleLocationListener) {
		_vehicleLocationListener = vehicleLocationListener;
	}

	protected void setConfigurationService(ConfigurationServiceImpl config) {
		_configurationService = config;
		refreshCache();
	}
	
	@Refreshable(dependsOn = {"display.checkAge", "display.useTimePredictions", "display.ageLimit"})
	protected void refreshCache() {
		checkAge = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.checkAge", "false"));
		useTimePredictions = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.useTimePredictions", "false"));
		ageLimit = Integer.parseInt(_configurationService.getConfigurationValueAsString("display.ageLimit", "300"));
	}
	 
	protected boolean getRefreshCheck() {
		return refreshCheck;
	}

	protected boolean getUseTimePredictions() {
		return useTimePredictions;
	}
	
	protected long getAgeLimit() {
		return ageLimit;
	}

	protected long computeTimeDifference(long timestamp) {
		return (System.currentTimeMillis() - timestamp) / 1000; // output in seconds
	}

	@Override
	protected void processResult(NycQueuedInferredLocationBean inferredResult, String contents) {
		VehicleLocationRecord vlr = new VehicleLocationRecord();
		if (checkAge) {
			long difference = computeTimeDifference(inferredResult.getRecordTimestamp());
			if (difference > ageLimit) {
				_log.info("VehicleLocationRecord for "+ inferredResult.getVehicleId() + " discarded.");
				return;
			}
		}
		vlr.setVehicleId(AgencyAndIdLibrary.convertFromString(inferredResult.getVehicleId()));
		vlr.setTimeOfRecord(inferredResult.getRecordTimestamp());
		vlr.setTimeOfLocationUpdate(inferredResult.getRecordTimestamp());
		vlr.setBlockId(AgencyAndIdLibrary.convertFromString(inferredResult.getBlockId()));
		vlr.setTripId(AgencyAndIdLibrary.convertFromString(inferredResult.getTripId()));
		vlr.setServiceDate(inferredResult.getServiceDate());
		vlr.setDistanceAlongBlock(inferredResult.getDistanceAlongBlock());
		vlr.setCurrentLocationLat(inferredResult.getInferredLatitude());
		vlr.setCurrentLocationLon(inferredResult.getInferredLongitude());
		vlr.setPhase(EVehiclePhase.valueOf(inferredResult.getPhase()));
		vlr.setStatus(inferredResult.getStatus());
		if (_vehicleLocationListener != null) {
			_vehicleLocationListener.handleVehicleLocationRecord(vlr);
		}
		if (useTimePredictions) {
			// if we're updating time predictions with the generation service,
			// tell the integration service to fetch
			// a new set of predictions now that the TDS has been updated
			// appropriately.
			_predictionIntegrationService.updatePredictionsForVehicle(vlr.getVehicleId());
		}
	}
}