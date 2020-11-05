/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.onebusaway.nyc.vehicle_tracking.services.unassigned.UnassignedVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;
@Component
@Qualifier("singleVehicleInputService")
/**
 * Listen for a single vehicle update for development/testing/debugging.
 * Set the vehicleId to configure.
 */
public class SingleVehicleQueueInputServiceImpl extends InputServiceImpl
		implements InputService {

	private String _vehicleId = "MTA NYCT_7182";
	private boolean _debug = false;
	private int _debugCounter = 0;

	public void setVehicleId(String vehicleId) {
		_vehicleId = vehicleId;
	}

	@Autowired
	private UnassignedVehicleService _service;

	@Autowired
	protected ConfigurationService _configurationService;

	@Override
	public boolean acceptMessage(RealtimeEnvelope envelope) {
		if (envelope == null || envelope.getCcLocationReport() == null)
			return false;

		final CcLocationReport message = envelope.getCcLocationReport();
		final CPTVehicleIden vehicleIdent = message.getVehicle();
		final AgencyAndId vehicleId = new AgencyAndId(
				vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId()
						+ "");

		boolean accept = _vehicleId.equals(vehicleId.toString());
		if (accept && _debug) {
			accept = acceptSpooking(envelope);

		}
		return accept;
	}

	/**
	 * Test code to simulate a bus with real-time to spooking transitions.  Set the debug
	 * flag to enable
	 * @param envelope
	 * @return
	 */
	private boolean acceptSpooking(RealtimeEnvelope envelope) {
		_debugCounter++;
		if (_debugCounter < 2) {
			return true; // build up some state
		}
		// reset every 200 observations
		if (_debugCounter > 200) {
			_debugCounter = 0;
		}
		_service.enqueueTestRecord(envelope);
		return false;

	}

	@Override
	public String replaceMessageContents(String contents) {
		return replaceAllStringOccurances(contents);
	}

	private String replaceAllStringOccurances(String contents) {
		final String[] searchList = new String[] { "vehiclepowerstate" };
		final String[] replacementList = new String[] { "vehiclePowerState" };
		return StringUtils.replaceEach(contents, searchList, replacementList);
	}

	@Refreshable(dependsOn = { "inference-engine.singleVehicleQueueVehicleId", "inference-engine.singleVehicleQueueDebugEnabled" })
	public void refreshCache(){
		_vehicleId = _configurationService.getConfigurationValueAsString(
				"inference-engine.singleVehicleQueueVehicleId", "MTA NYCT_7182");
		_debug = _configurationService.getConfigurationValueAsBoolean(
				"inference-engine.singleVehicleQueueDebugEnabled", false);
	}

	@Override
	@PostConstruct
	public void setup() {
		refreshCache();
		super.setup();
	}

}
