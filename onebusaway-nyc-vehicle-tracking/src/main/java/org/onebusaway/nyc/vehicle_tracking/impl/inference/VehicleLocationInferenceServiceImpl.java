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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lrms_final_09_07.Angle;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.DummyOperatorAssignmentServiceImpl;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.InferenceOutputState;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import tcip_3_0_5_local.NMEA;
import tcip_final_3_0_5_1.CcLocationReport;
import tcip_final_3_0_5_1.CcLocationReport.EmergencyCodes;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.jhlabs.map.proj.ProjectionException;

@Component
public class VehicleLocationInferenceServiceImpl implements VehicleLocationInferenceService {

  private static Logger _log = LoggerFactory.getLogger(VehicleLocationInferenceServiceImpl.class);

  private static final DateTimeFormatter XML_DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeParser();

  private static final int MAX_SURGE = 1000;

  @Autowired
  private OutputQueueSenderService _outputQueueSenderService;

  @Autowired
  private VehicleAssignmentService _vehicleAssignmentService;

  @Autowired
  private TransitGraphDao _transitGraphDao;

  @Autowired
  private BundleManagementService _bundleManagementService;
  
  @Autowired
  private NycTransitDataService _nycTransitDataService;
  
  @Autowired
  private VehicleLocationListener _vehicleLocationListener;

  @Autowired
  private PredictionIntegrationService _predictionIntegrationService;
  
  @Autowired
  protected ConfigurationService _configurationService;

  @Autowired
  private VehicleInferenceProcessorFactory _vehicleInferenceProcessorFactory;
  
  private ArrayBlockingQueue<InferenceOutputState> _outqueue = new ArrayBlockingQueue<InferenceOutputState>(MAX_SURGE);

  private BundleItem _lastBundle = null;

  private ExecutorService _executorService;

  private int _skippedUpdateLogCounter = 0;

  private final ConcurrentMap<AgencyAndId, VehicleInferenceInstance> _vehicleInstancesByVehicleId = 
		  new ConcurrentHashMap<AgencyAndId, VehicleInferenceInstance>();

  private final ConcurrentMap<AgencyAndId, VehicleInferenceProcessor> _vehicleProcessorByVehicleId =
      new ConcurrentHashMap<AgencyAndId, VehicleInferenceProcessor>();
  private final ConcurrentHashMap<AgencyAndId, InferenceOutputState> _inferenceOutputByVehicleId =
      new ConcurrentHashMap<AgencyAndId, InferenceOutputState>();
  
  private boolean refreshCheck;

  public VehicleLocationInferenceServiceImpl() {
	  setConfigurationService(new ConfigurationServiceImpl());
//	  refreshCache();
  }

  @PostConstruct
  public void start() {
    _executorService = Executors.newFixedThreadPool(1);
    ProcessingTask task = new ProcessingTask(_outqueue, _inferenceOutputByVehicleId);
    _executorService.submit(task);
    
  }  

  @PreDestroy
  public void stop() {
    _executorService.shutdownNow();
  }
  
  protected void setPredictionIntegrationService( PredictionIntegrationService predictionIntegrationService) {
	  _predictionIntegrationService = predictionIntegrationService;
  }

  protected void setVehicleLocationListener( VehicleLocationListener vehicleLocationListener) {
	  _vehicleLocationListener = vehicleLocationListener;
  }

  protected void setConfigurationService(ConfigurationServiceImpl config) {
	  _configurationService = config;
  }
  
  protected boolean getRefreshCheck() {
	  return refreshCheck;
  }
  
  /****
   * Service Methods
   ****/

  /**
   * This method is used by the simulator to inject a trace into the inference process.
   */
  @Override
  public void handleNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();
    
    VehicleInferenceProcessor processor = getProcessorForVehicle(record.getVehicleId());
    
    processor.enqueue(record);
  }
  
  /**
   * This method is used by the simulator to inject a trace into the inference PIPELINE, but not through
   * the inference algorithm itself. 
   */
  @Override
  public void handleBypassUpdateForNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();
    VehicleInferenceProcessor processor = getProcessorForVehicle(record.getVehicleId());
    processor.enqueue(record);
  }

  /**
   * This method is used by the simulator to inject real time records into the inference
   * process. This is used as a replacement to the queue infrastructure by some users, so
   * we don't handle this as a simulation.
   */
  @Override
  public void handleNycRawLocationRecord(NycRawLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();
    VehicleInferenceProcessor processor = getProcessorForVehicle(record.getVehicleId());
    processor.enqueue(record);
  }

  /**
   * This method is used by the queue listener to process raw messages from the message queue. 
   *
   * ***This is the main entry point for data in the MTA project.***
   */
  @Override
  public void handleRealtimeEnvelopeRecord(RealtimeEnvelope envelope) {
    final CcLocationReport message = envelope.getCcLocationReport();

    if (!_bundleManagementService.bundleIsReady()) {
      _skippedUpdateLogCounter++;

      // only print this every 25 times so we don't fill up the logs!
      if (_skippedUpdateLogCounter > 25) {
        _log.warn("Bundle is not ready or none is loaded--we've skipped 25 messages since last log event.");
        _skippedUpdateLogCounter = 0;
      }

      return;
    }

    verifyVehicleResultMappingToCurrentBundle();

    // construct raw record
    final NycRawLocationRecord r = new NycRawLocationRecord();
    r.setUuid(envelope.getUUID());

    r.setLatitude(message.getLatitude() / 1000000f);
    r.setLongitude(message.getLongitude() / 1000000f);

    final Angle bearing = message.getDirection();
    if (bearing != null) {
      final Integer degrees = bearing.getCdeg();
      if (degrees != null)
        r.setBearing(degrees);
    }

    r.setSpeed(message.getSpeed());

    r.setDestinationSignCode(message.getDestSignCode().toString());
    r.setDeviceId(message.getManufacturerData());

    final AgencyAndId vehicleId = new AgencyAndId(
	        message.getVehicle().getAgencydesignator(),
	        Long.toString(message.getVehicle().getVehicleId()));
    r.setVehicleId(vehicleId);
    
    if (!StringUtils.isEmpty(message.getOperatorID().getDesignator()))
      r.setOperatorId(message.getOperatorID().getDesignator());

    if (!StringUtils.isEmpty(message.getRunID().getDesignator()))
      r.setRunNumber(message.getRunID().getDesignator());

    if (!StringUtils.isEmpty(message.getRouteID().getRouteDesignator()))
      r.setRunRouteId(message.getRouteID().getRouteDesignator());

    final EmergencyCodes emergencyCodes = message.getEmergencyCodes();
    if (emergencyCodes != null)
      r.setEmergencyFlag(true);
    else
      r.setEmergencyFlag(false);

    final tcip_3_0_5_local.CcLocationReport gpsData = message.getLocalCcLocationReport();
    if (gpsData != null && gpsData.getNMEA() != null) {
      final NMEA nemaSentences = gpsData.getNMEA();
      final List<String> sentenceStrings = nemaSentences.getSentence();

      for (final String sentence : sentenceStrings) {
        if (sentence.startsWith("$GPGGA"))
          r.setGga(sentence);

        if (sentence.startsWith("$GPRMC"))
          r.setRmc(sentence);
      }
    }

    final DateTime time = XML_DATE_TIME_FORMAT.parseDateTime(message.getTimeReported());
    r.setTime(time.getMillis());
    r.setTimeReceived(new Date().getTime());

    // validate timestamp from bus--for debugging only
    final String RMCSentence = r.getRmc();

    if (RMCSentence != null) {
      final String[] parts = RMCSentence.split(",");
      if (parts.length == 13) {
        final String timePart = parts[1];
        final String datePart = parts[9];
        if (timePart.length() >= 6 && datePart.length() == 6) {
          try {
            final DateFormat formatter = new SimpleDateFormat("ddMMyy HHmmss");
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));

            final Date fromDRU = formatter.parse(datePart + " " + timePart);
            final long differenceInSeconds = (fromDRU.getTime() - time.getMillis()) / 1000;

            if (differenceInSeconds > 30 * 60) { // 30m
              _log.debug("Vehicle "
                  + vehicleId
                  + " has significant time difference between time from DRU and time from record\n"
                  + "Difference in seconds: " + differenceInSeconds + "\n"
                  + "Difference in hours: " + (differenceInSeconds / 60 / 60)
                  + "\n" + "Raw timestamp: " + message.getTimeReported() + "\n"
                  + "From RMC: " + datePart + " " + timePart);
            }
          } catch (final ParseException e) {
            _log.debug("Unparseable date: " + datePart + " " + timePart);
          }
        }
      }
    }
    
    final VehicleInferenceProcessor p = getProcessorForVehicle(vehicleId);
    p.enqueue(r);
    _bundleManagementService.registerInferenceProcessor(p);
  }

  @Override
  public  NycTestInferredLocationRecord getNycTestInferredLocationRecordForVehicle(
      AgencyAndId vid) {
    final InferenceOutputState inferenceOutputState = _inferenceOutputByVehicleId.get(vid);
    if (inferenceOutputState == null) {
      //TODOSAB
    }
    return inferenceOutputState.getNycTestInferredLocationRecord();
  }

  @Override
  public void resetVehicleLocation(AgencyAndId vid) {
    _vehicleInstancesByVehicleId.remove(vid);
//    _vehicleProcessorByVehicleId.remove(vid); // TODOSAB
    this.getProcessorForVehicle(vid).resetVehicleLocation(vid);
  }

  /****
   * Debugging Methods
   ****/

  
  
  @Override
  public List<NycTestInferredLocationRecord> getLatestProcessedVehicleLocationRecords() {
    final List<NycTestInferredLocationRecord> records = new ArrayList<NycTestInferredLocationRecord>();
    for (AgencyAndId vehicleId :_inferenceOutputByVehicleId.keySet()) {
      final InferenceOutputState ios = _inferenceOutputByVehicleId.get(vehicleId);
      if (ios != null) {
        final NycTestInferredLocationRecord record = ios.getNycTestInferredLocationRecord(); 
        if (record != null) { 
          record.setVehicleId(vehicleId);
          records.add(record);
        }
      }
    }
    
    return records;
  }
  

  public List<NycQueuedInferredLocationBean> getLatestProcessedQueuedVehicleLocationRecords() {
    final List<NycQueuedInferredLocationBean> records = new ArrayList<NycQueuedInferredLocationBean>();
    
    for (AgencyAndId vehicleId : _inferenceOutputByVehicleId.keySet()) {
      final InferenceOutputState ios = _inferenceOutputByVehicleId.get(vehicleId);
      if (ios != null) {
        NycQueuedInferredLocationBean nycQueuedInferredLocationBean = ios.getNycQueuedInferredLocationBean();
        if (nycQueuedInferredLocationBean != null) {
          nycQueuedInferredLocationBean.setVehicleId(vehicleId.toString());
          records.add(nycQueuedInferredLocationBean);
        }
      }
    }
    return records;
  }

  @Override
  public Multiset<Particle> getCurrentParticlesForVehicleId(
      AgencyAndId vehicleId) {
    final InferenceOutputState ios = _inferenceOutputByVehicleId.get(vehicleId);
    if (ios == null) {
      return ImmutableMultiset.of();
    }
    return ios.getCurrentParticles();
  }

  @Override
  public Multiset<Particle> getCurrentSampledParticlesForVehicleId(
      AgencyAndId vehicleId) {
    final InferenceOutputState ios = _inferenceOutputByVehicleId.get(vehicleId);
    if (ios == null) {
      return ImmutableMultiset.of();
    }
    return ios.getCurrentSampledParticles();
  }

  @Override
  public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
      AgencyAndId vehicleId) {
    final InferenceOutputState ios = _inferenceOutputByVehicleId.get(vehicleId);
    if (ios == null) {
      return Collections.emptyList();
    }
    return ios.getCurrentJourneySummaries();
  }

  @Override
  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId) {
    final InferenceOutputState ios = _inferenceOutputByVehicleId.get(vehicleId);
    if (ios == null) {
      return null;
    }
    return ios.getVehicleLocationDetails();
  }

  @Override
  public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId) {
    final InferenceOutputState ios = _inferenceOutputByVehicleId.get(vehicleId);
    if (ios == null) {
      return null;
    }
    
    VehicleLocationDetails details = ios.getBadParticleDetails();
    if (details != null) {
      details.setVehicleId(vehicleId);
      details.setParticleFilterFailureActive(true);
    }
    return details;
  }

  @Override
  public VehicleLocationDetails getBadDetailsForVehicleId(
      AgencyAndId vehicleId, int particleId) {
    final VehicleLocationDetails details = getBadDetailsForVehicleId(vehicleId);
    if (details == null)
      return null;
    return findParticle(details, particleId);
  }

  @Override
  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId,
      int particleId) {
    final VehicleLocationDetails details = getDetailsForVehicleId(vehicleId);
    if (details == null)
      return null;
    return findParticle(details, particleId);
  }


  private VehicleInferenceProcessor getProcessorForVehicle(AgencyAndId vehicleId) {
    VehicleInferenceProcessor processor = _vehicleProcessorByVehicleId.get(vehicleId);
    if (processor != null && !processor.isDone()) return processor;
    synchronized (_vehicleProcessorByVehicleId) {
      processor = _vehicleProcessorByVehicleId.get(vehicleId);
      if (processor != null && !processor.isDone()) return processor;
      
      final VehicleInferenceProcessor newProcessor = _vehicleInferenceProcessorFactory.create(vehicleId,_outqueue);
      _vehicleProcessorByVehicleId.put(vehicleId, newProcessor);
      _log.info("Processors[" + vehicleId + "]=" + _vehicleProcessorByVehicleId.size());
      _bundleManagementService.registerInferenceProcessor(processor);
      if (processor == null)
        processor = newProcessor;
    }
    return processor;
  }
  
  /**
   * Has the bundle changed since the last time we returned a result?
   * 
   * @return boolean: bundle changed or not
   */
  private boolean bundleHasChanged() {
    boolean result = false;

    final BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();

    // active bundle was removed from BMS' list of active bundles
    if (currentBundle == null)
      return true;

    if (_lastBundle != null) {
      result = !_lastBundle.getId().equals(currentBundle.getId());
    }

    _lastBundle = currentBundle;
    return result;
  }

  /**
   * If the bundle has changed, verify that all vehicle results are present in
   * the current bundle. If not, reset the inference to map them to the current
   * reference data (bundle). Also reset vehicles with no current match, as they
   * may have a match in the new bundle.
   */
  private void verifyVehicleResultMappingToCurrentBundle() {
    if (!bundleHasChanged())
      return;

    // we have explicitly cleared out the map on a bundle change, no need to do any more
  }

  private VehicleLocationDetails findParticle(VehicleLocationDetails details,
      int particleId) {
    final List<Entry<Particle>> particles = details.getParticles();
    if (particles != null) {
      for (final Entry<Particle> pEntry : particles) {
        Particle p = pEntry.getElement();
        if (p.getIndex() == particleId) {
          final Multiset<Particle> history = TreeMultiset.create();
          while (p != null) {
            history.add(p, pEntry.getCount());
            p = p.getParent();
          }
          details.setParticles(history);
          details.setHistory(true);
          return details;
        }
      }
    }
    return null;
  }

  public class ProcessingTask implements Runnable {

    private ArrayBlockingQueue<InferenceOutputState> queue;
    private ConcurrentHashMap<AgencyAndId, InferenceOutputState> inferenceOutputByVehicleId;
    
    public ProcessingTask(ArrayBlockingQueue<InferenceOutputState> queue,
        ConcurrentHashMap<AgencyAndId, InferenceOutputState> inferenceOutputByVehicleId
        ) {
      this.queue = queue;
      this.inferenceOutputByVehicleId = inferenceOutputByVehicleId;
    }

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          InferenceOutputState state = queue.take();
          inferenceOutputByVehicleId.put(state.getVehicleId(), state);
        } catch (InterruptedException e) {
          // bury
        } catch (Throwable ex) {
          _log.error("ProcessorTask broke:", ex);
        }
      }
    }
  }

  @Override
  public void setSeeds(long cdfSeed, long factorySeed) {
    ParticleFactoryImpl.setSeed(factorySeed);
    CategoricalDist.setSeed(cdfSeed);
  }
  

}