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
package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An output service for debugging--sends inference output to the VTW-embedded TDS instead
 * of to an inference output queue.
 * 
 * @author jmaki
 *
 */
public class DummyOutputQueueSenderServiceImpl implements
    OutputQueueSenderService {

  public boolean _isPrimaryInferenceInstance = true;

  public String _primaryHostname = null;

  @Autowired
  private ConfigurationService _configurationService;
  
  @Autowired
  private PredictionIntegrationService _predictionIntegrationService;
  
  @Autowired
  private VehicleLocationListener _vehicleLocationListener;

  private Boolean useTimePredictionsIfAvailable() {
    return Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.useTimePredictions", "false"));
  }

  @Override
  public void enqueue(NycQueuedInferredLocationBean r) {
    final VehicleLocationRecord vlr = RecordLibrary.getNycQueuedInferredLocationBeanAsVehicleLocationRecord(r);
    _vehicleLocationListener.handleVehicleLocationRecord(vlr);

    if(useTimePredictionsIfAvailable())
    	_predictionIntegrationService.updatePredictionsForVehicle(vlr.getVehicleId());
  }

  @Override
  public void setIsPrimaryInferenceInstance(boolean isPrimary) {
    _isPrimaryInferenceInstance = isPrimary;
  }

  @Override
  public boolean getIsPrimaryInferenceInstance() {
    return _isPrimaryInferenceInstance;
  }

  @Override
  public void setPrimaryHostname(String hostname) {
    _primaryHostname = hostname;
  }

  @Override
  public String getPrimaryHostname() {
    return _primaryHostname;
  }

}
