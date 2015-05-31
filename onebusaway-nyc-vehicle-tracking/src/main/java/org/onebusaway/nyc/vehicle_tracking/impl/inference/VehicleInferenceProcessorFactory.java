package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.concurrent.ArrayBlockingQueue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.onebusaway.nyc.vehicle_tracking.model.InferenceOutputState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class VehicleInferenceProcessorFactory {

  
  private ApplicationContext _applicationContext;
  /**
   * Usually, we shoudn't ever have a reference to ApplicationContext, but we
   * need it for the prototype
   * 
   * @param applicationContext
   */
  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    _applicationContext = applicationContext;
  }
  
  @Autowired
  private VehicleLocationListener _vehicleLocationListener;
  @Autowired
  private PredictionIntegrationService _predictionIntegrationService;
  @Autowired
  private OutputQueueSenderService _outputQueueSenderService;
  @Autowired
  private NycTransitDataService _nycTransitDataService;
  @Autowired 
  private ObservationCache _observationCache;
  @Autowired
  protected ConfigurationService _configurationService;
  @Autowired
  private BundleManagementService _bundleManagementService;
  @Autowired
  private VehicleAssignmentService _vehicleAssignmentService;


  // Process Record Fields (TDS)
  private boolean useTimePredictions = false;
  
  private boolean checkAge = false;
  
  private int ageLimit = 300;

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


  public VehicleInferenceProcessor create(AgencyAndId vehicleId, ArrayBlockingQueue<InferenceOutputState> outQueue) {
    final VehicleInferenceInstance newInstance = _applicationContext.getBean(VehicleInferenceInstance.class);
    VehicleInferenceProcessor processor = new VehicleInferenceProcessor();
    processor.setVehicleInferenceInstance(newInstance);
    processor.setVehicleId(vehicleId);
    processor.setOutputQueueSenderService(_outputQueueSenderService);
    processor.setNycTransitDataService(_nycTransitDataService);
    processor.setCheckAge(checkAge);
    processor.setAgeLimit(ageLimit);
    processor.setVehicleLocationListener(_vehicleLocationListener);
    processor.setPredictionIntegrationService(_predictionIntegrationService);
    processor.setUseTimePredictions(useTimePredictions);
    processor.setObservationCache(_observationCache);
    processor.setOutQueue(outQueue);
    final BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();
    processor.setCurrentBundle(currentBundle);
    final String depotId = _vehicleAssignmentService.getAssignedDepotForVehicleId(vehicleId);
    processor.setDepotId(depotId);
    processor.start();
    return processor;
  }

}
