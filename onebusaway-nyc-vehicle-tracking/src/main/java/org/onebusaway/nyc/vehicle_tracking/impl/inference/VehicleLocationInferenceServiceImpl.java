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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
public class VehicleLocationInferenceServiceImpl implements
    VehicleLocationInferenceService {

  private static Logger _log = LoggerFactory.getLogger(VehicleLocationInferenceServiceImpl.class);

  private static final DateTimeFormatter XML_DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeParser();

  private static final long MIN_RECORD_INTERVAL = 5 * 1000; // 5 seconds

  @Autowired
  private ObservationCache _observationCache;

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

  private BundleItem _lastBundle = null;

  private ExecutorService _executorService;

  private int _skippedUpdateLogCounter = 0;

  private int _numberOfProcessingThreads = 2 + (Runtime.getRuntime().availableProcessors() * 20);

  private final ConcurrentMap<AgencyAndId, VehicleInferenceInstance> _vehicleInstancesByVehicleId = new ConcurrentHashMap<AgencyAndId, VehicleInferenceInstance>();

  private final ConcurrentMap<AgencyAndId, Long> _timeReceivedByVehicleId = new ConcurrentHashMap<AgencyAndId, Long>(
      10000);

  private ApplicationContext _applicationContext;

  // Process Record Fields (TDS)
  private boolean useTimePredictions = false;

  private boolean checkAge = false;

  private int ageLimit = 300;

  private boolean refreshCheck;

  public VehicleLocationInferenceServiceImpl() {
    setConfigurationService(new ConfigurationServiceImpl());
    refreshCache();
  }

  public void setNumberOfProcessingThreads(int numberOfProcessingThreads) {
    _numberOfProcessingThreads = numberOfProcessingThreads;
  }

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

  @PostConstruct
  public void start() {
    if (_numberOfProcessingThreads <= 0)
      throw new IllegalArgumentException(
          "numberOfProcessingThreads must be positive");

    _log.info("Creating thread pool of size=" + _numberOfProcessingThreads);
    _executorService = Executors.newFixedThreadPool(_numberOfProcessingThreads);
  }

  @PreDestroy
  public void stop() {
    _executorService.shutdownNow();
  }

  protected void setPredictionIntegrationService(
      PredictionIntegrationService predictionIntegrationService) {
    _predictionIntegrationService = predictionIntegrationService;
  }

  protected void setVehicleLocationListener(
      VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  protected void setConfigurationService(ConfigurationServiceImpl config) {
    _configurationService = config;
    refreshCache();
  }

  protected boolean getRefreshCheck() {
    return refreshCheck;
  }

  @Refreshable(dependsOn = {
      "display.checkAge", "display.useTimePredictions", "display.ageLimit"})
  protected void refreshCache() {
    checkAge = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString(
        "display.checkAge", "false"));
    useTimePredictions = Boolean.parseBoolean(_configurationService.getConfigurationValueAsString(
        "display.useTimePredictions", "false"));
    ageLimit = Integer.parseInt(_configurationService.getConfigurationValueAsString(
        "display.ageLimit", "300"));
  }

  /****
   * Service Methods
   ****/

  /**
   * This method is used by the simulator to inject a trace into the inference
   * process.
   */
  @Override
  public void handleNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();

    if (isValidRecord(record.getVehicleId(), record.getTimestamp())) {
      final VehicleInferenceInstance i = getInstanceForVehicle(record.getVehicleId());
      final Future<?> result = _executorService.submit(new ProcessingTask(i,
          record, true, false));
      _bundleManagementService.registerInferenceProcessingThread(result);
    }
  }

  /**
   * This method is used by the simulator to inject a trace into the inference
   * PIPELINE, but not through the inference algorithm itself.
   */
  @Override
  public void handleBypassUpdateForNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();

    if (isValidRecord(record.getVehicleId(), record.getTimestamp())) {
      final VehicleInferenceInstance i = getInstanceForVehicle(record.getVehicleId());

      final Future<?> result = _executorService.submit(new ProcessingTask(i,
          record, true, true));
      _bundleManagementService.registerInferenceProcessingThread(result);
    }
  }

  /**
   * This method is used by the simulator to inject real time records into the
   * inference process. This is used as a replacement to the queue
   * infrastructure by some users, so we don't handle this as a simulation.
   */
  @Override
  public void handleNycRawLocationRecord(NycRawLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();
    if (isValidRecord(record.getVehicleId(), record.getTime())) {
      final VehicleInferenceInstance i = getInstanceForVehicle(record.getVehicleId());

      final Future<?> result = _executorService.submit(new ProcessingTask(i,
          record, false, false));
      _bundleManagementService.registerInferenceProcessingThread(result);
    }
  }

  /**
   * This method is used by the queue listener to process raw messages from the
   * message queue.
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
    if (isValidRecord(r.getVehicleId(), r.getTime())) {
      final VehicleInferenceInstance i = getInstanceForVehicle(vehicleId);
      final Future<?> result = _executorService.submit(new ProcessingTask(i, r,
          false, false));
      _bundleManagementService.registerInferenceProcessingThread(result);
    }
  }

  @Override
  public NycTestInferredLocationRecord getNycTestInferredLocationRecordForVehicle(
      AgencyAndId vid) {
    final VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vid);
    if (instance == null)
      return null;

    final NycTestInferredLocationRecord record = instance.getCurrentState();
    if (record != null)
      record.setVehicleId(vid);

    return record;
  }

  @Override
  public void resetVehicleLocation(AgencyAndId vid) {
    _vehicleInstancesByVehicleId.remove(vid);
    _observationCache.purge(vid);
  }

  /****
   * Debugging Methods
   ****/

  @Override
  public List<NycTestInferredLocationRecord> getLatestProcessedVehicleLocationRecords() {
    final List<NycTestInferredLocationRecord> records = new ArrayList<NycTestInferredLocationRecord>();

    for (final Map.Entry<AgencyAndId, VehicleInferenceInstance> entry : _vehicleInstancesByVehicleId.entrySet()) {
      final AgencyAndId vehicleId = entry.getKey();
      final VehicleInferenceInstance instance = entry.getValue();
      if (instance != null) {
        final NycTestInferredLocationRecord record = instance.getCurrentState();
        if (record != null) {
          record.setVehicleId(vehicleId);
          records.add(record);
        }
      } else {
        _log.warn("No VehicleInferenceInstance found: vid=" + vehicleId);
      }
    }

    return records;
  }

  public List<NycQueuedInferredLocationBean> getLatestProcessedQueuedVehicleLocationRecords() {
    final List<NycQueuedInferredLocationBean> records = new ArrayList<NycQueuedInferredLocationBean>();

    for (final Map.Entry<AgencyAndId, VehicleInferenceInstance> entry : _vehicleInstancesByVehicleId.entrySet()) {
      final AgencyAndId vehicleId = entry.getKey();
      final VehicleInferenceInstance instance = entry.getValue();
      if (instance != null) {
        final NycQueuedInferredLocationBean record = instance.getCurrentStateAsNycQueuedInferredLocationBean();
        if (record != null) {
          record.setVehicleId(vehicleId.toString());
          postProcess(record);
          records.add(record);
        }
      } else {
        _log.warn("No VehicleInferenceInstance found: vid=" + vehicleId);
      }
    }

    return records;
  }

  @Override
  public Multiset<Particle> getCurrentParticlesForVehicleId(
      AgencyAndId vehicleId) {
    final VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return ImmutableMultiset.of();
    return instance.getCurrentParticles();
  }

  @Override
  public Multiset<Particle> getCurrentSampledParticlesForVehicleId(
      AgencyAndId vehicleId) {
    final VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return ImmutableMultiset.of();
    return instance.getCurrentSampledParticles();
  }

  @Override
  public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
      AgencyAndId vehicleId) {
    final VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return Collections.emptyList();
    return instance.getJourneySummaries();
  }

  @Override
  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId) {
    final VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return null;
    final VehicleLocationDetails details = instance.getDetails();
    details.setVehicleId(vehicleId);
    return details;
  }

  @Override
  public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId) {
    final VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return null;
    final VehicleLocationDetails details = instance.getBadParticleDetails();
    details.setVehicleId(vehicleId);
    details.setParticleFilterFailureActive(true);
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

  /****
   * Private Methods
   ****/
  private VehicleInferenceInstance getInstanceForVehicle(AgencyAndId vehicleId) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance != null)
      return instance;
    synchronized (_vehicleInstancesByVehicleId) {
      // check to see if a thread ahead of us won
      instance = _vehicleInstancesByVehicleId.get(vehicleId);
      if (instance != null)
        return instance;

      final VehicleInferenceInstance newInstance = _applicationContext.getBean(VehicleInferenceInstance.class);
      instance = _vehicleInstancesByVehicleId.putIfAbsent(vehicleId,
          newInstance);
      if (instance == null)
        instance = newInstance;
    }
    return instance;
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

    for (final AgencyAndId vehicleId : _vehicleInstancesByVehicleId.keySet()) {
      try {
        final VehicleInferenceInstance vehicleInstance = _vehicleInstancesByVehicleId.get(vehicleId);
        final NycTestInferredLocationRecord state = vehicleInstance.getCurrentState();

        // no state
        if (state == null) {
          _log.info("Vehicle " + vehicleId
              + " reset on bundle change: no state available.");

          this.resetVehicleLocation(vehicleId);
          continue;
        }

        // no match to any trip
        if (_transitGraphDao.getBlockEntryForId(AgencyAndIdLibrary.convertFromString(state.getInferredBlockId())) == null
            || _transitGraphDao.getTripEntryForId(AgencyAndIdLibrary.convertFromString(state.getInferredTripId())) == null) {
          _log.info("Vehicle " + vehicleId
              + " reset on bundle change: no matched trip/block.");

          this.resetVehicleLocation(vehicleId);
          continue;
        }

        // make sure none of the current journey summaries contain blocks we no
        // longer have
        boolean vehicleStateResetVehicle = false;
        for (Particle particle : getCurrentSampledParticlesForVehicleId(vehicleId)) {
          final VehicleState particleState = particle.getData();
          final BlockStateObservation particleBlockState = particleState.getBlockStateObservation();
          if (particleBlockState == null)
            continue;

          final BlockInstance blockInstance = particleBlockState.getBlockState().getBlockInstance();
          final BlockConfigurationEntry blockConfig = blockInstance.getBlock();
          final BlockEntry block = blockConfig.getBlock();

          final ScheduledBlockLocation blockLocation = particleBlockState.getBlockState().getBlockLocation();
          final BlockTripEntry activeTrip = blockLocation.getActiveTrip();

          if (_transitGraphDao.getBlockEntryForId(block.getId()) == null
              || _transitGraphDao.getTripEntryForId(activeTrip.getTrip().getId()) == null) {
            _log.info("Vehicle "
                + vehicleId
                + " reset on bundle change: particle had no matched trip/block.");

            this.resetVehicleLocation(vehicleId);
            vehicleStateResetVehicle = true;
            break;
          }
        }
        if (vehicleStateResetVehicle)
          continue;

        _log.info("NOT resetting vehicle ID " + vehicleId);
      } catch (final Exception e) {
        // if something goes wrong, reset inference state
        _log.info("Vehicle " + vehicleId
            + " reset on bundle change: exception thrown: " + e.getMessage());

        this.resetVehicleLocation(vehicleId);
      }
    }
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

    private final AgencyAndId _vehicleId;

    private final NycRawLocationRecord _nycRawLocationRecord;

    private NycTestInferredLocationRecord _nycTestInferredLocationRecord;

    private final VehicleInferenceInstance _inferenceInstance;

    private boolean _bypass = false;

    private boolean _simulation = false;

    public ProcessingTask(VehicleInferenceInstance inferenceInstance,
        NycRawLocationRecord record, boolean simulation, boolean bypass) {
      _inferenceInstance = inferenceInstance;
      _vehicleId = record.getVehicleId();

      _nycRawLocationRecord = record;

      _simulation = simulation;
      _bypass = bypass;
    }

    public ProcessingTask(VehicleInferenceInstance inferenceInstance,
        NycTestInferredLocationRecord record, boolean simulation, boolean bypass) {
      _inferenceInstance = inferenceInstance;
      _vehicleId = record.getVehicleId();

      _nycTestInferredLocationRecord = record;
      _nycRawLocationRecord = RecordLibrary.getNycTestInferredLocationRecordAsNycRawLocationRecord(record);

      _simulation = simulation;
      _bypass = bypass;
    }

    @Override
    public void run() {
      try {
        // operator assignment service in simulation case: returns a 1:1 result
        // to what the trace indicates is the
        // true operator assignment.
        if (_simulation) {
          final DummyOperatorAssignmentServiceImpl opSvc = new DummyOperatorAssignmentServiceImpl();

          final String assignedRun = _nycTestInferredLocationRecord.getAssignedRunId();
          final String operatorId = _nycTestInferredLocationRecord.getOperatorId();
          if (!Strings.isNullOrEmpty(assignedRun)
              && !Strings.isNullOrEmpty(operatorId)) {
            final String[] runParts = assignedRun.split("-");
            if (runParts.length > 2) {
              opSvc.setOperatorAssignment(new AgencyAndId(
                  _nycRawLocationRecord.getVehicleId().getAgencyId(),
                  operatorId), runParts[1] + "-" + runParts[2], runParts[0]);

            } else {
              opSvc.setOperatorAssignment(new AgencyAndId(
                  _nycRawLocationRecord.getVehicleId().getAgencyId(),
                  operatorId), runParts[1], runParts[0]);
            }
          }

          _inferenceInstance.setOperatorAssignmentService(opSvc);
          _log.warn("Set operator assignment service to dummy for debugging!");
        }

        // bypass/process record through inference
        boolean inferenceSuccess = false;
        NycQueuedInferredLocationBean record = null;
        if (_nycRawLocationRecord != null) {
          if (_bypass == true) {
            record = _inferenceInstance.handleBypassUpdateWithResults(_nycTestInferredLocationRecord);
          } else {
            record = _inferenceInstance.handleUpdateWithResults(_nycRawLocationRecord);
          }
        }
        if (record != null)
          inferenceSuccess = true;

        if (inferenceSuccess) {
          // send input "actuals" as inferred result to output queue to bypass
          // inference process
          if (_bypass == true) {
            record.setVehicleId(_vehicleId.toString());

            // fix up the service date if we're shifting the dates of the record
            // to now, since
            // the wrong service date will make the TDS not match the trip
            // properly.
            if (record.getServiceDate() == 0) {
              final GregorianCalendar gc = new GregorianCalendar();
              gc.setTime(_nycTestInferredLocationRecord.getTimestampAsDate());
              gc.set(Calendar.HOUR_OF_DAY, 0);
              gc.set(Calendar.MINUTE, 0);
              gc.set(Calendar.SECOND, 0);
              record.setServiceDate(gc.getTimeInMillis());
            }

            _outputQueueSenderService.enqueue(record);

            // send inferred result to output queue
          } else {
            // add some more properties to the record before sending off
            record.setVehicleId(_vehicleId.toString());
            record.getManagementRecord().setInferenceEngineIsPrimary(
                _outputQueueSenderService.getIsPrimaryInferenceInstance());
            record.getManagementRecord().setDepotId(
                _vehicleAssignmentService.getAssignedDepotForVehicleId(_vehicleId));

            // Process TDS Fields - OBANYC-2184 (Previously Archive
            // PostProceess)
            postProcess(record);

            final BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();
            if (currentBundle != null) {
              record.getManagementRecord().setActiveBundleId(
                  currentBundle.getId());
            }

            _outputQueueSenderService.enqueue(record);
          }
        }
      } catch (final ProjectionException e) {
        // discard this one
      } catch (final Throwable ex) {
        _log.error(
            "Error processing new location record for inference on vehicle "
                + _vehicleId + ": ", ex);
        resetVehicleLocation(_vehicleId);
        _observationCache.purge(_vehicleId);
      }
    }
  }

  @Override
  public void setSeeds(long cdfSeed, long factorySeed) {
    ParticleFactoryImpl.setSeed(factorySeed);
    CategoricalDist.setSeed(cdfSeed);
  }

  /**
   * This method is used to populate additional record values using the TDS
   */
  protected void postProcess(NycQueuedInferredLocationBean record) {
    // Populate TDS Fields (VLR)
    processRecord(record);

    // Process TDS Fields - OBANYC-2184 (Previously Archive PostProceess)
    VehicleStatusBean vehicle = _nycTransitDataService.getVehicleForAgency(
        record.getVehicleId(), new Date(record.getRecordTimestamp()).getTime());
    record.setVehicleStatusBean(vehicle);

    VehicleLocationRecordBean vehicleLocation = _nycTransitDataService.getVehicleLocationRecordForVehicleId(
        record.getVehicleId(), new Date(record.getRecordTimestamp()).getTime());

    record.setVehicleLocationRecordBean(vehicleLocation);

  }

  /**
   * This method is used to generate values for the TDS
   */
  protected void processRecord(NycQueuedInferredLocationBean inferredResult) {
    VehicleLocationRecord vlr = new VehicleLocationRecord();
    if (checkAge) {
      long difference = computeTimeDifference(inferredResult.getRecordTimestamp());
      if (difference > ageLimit) {
        _log.info("VehicleLocationRecord for " + inferredResult.getVehicleId()
            + " discarded.");
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

  private long computeTimeDifference(long timestamp) {
    return (System.currentTimeMillis() - timestamp) / 1000; // output in seconds
  }

  private boolean isValidRecord(AgencyAndId vid, Long timeReceived) {
    synchronized (_timeReceivedByVehicleId) {
      if (vid == null)
        return true;
      boolean isValid = true;
      Long mapTimeReceived = _timeReceivedByVehicleId.get(vid);
      if (mapTimeReceived != null) {
        if (Math.abs(timeReceived - mapTimeReceived) <= MIN_RECORD_INTERVAL) {
          _log.warn("Minimum record interval of " + MIN_RECORD_INTERVAL / 1000
              + "sec reached, dropping inference instance for vehicleId " + vid);
          isValid =  false;
        }
      }
      if(isValid)
        _timeReceivedByVehicleId.put(vid, timeReceived);
      
      return isValid;
    }
  }
}