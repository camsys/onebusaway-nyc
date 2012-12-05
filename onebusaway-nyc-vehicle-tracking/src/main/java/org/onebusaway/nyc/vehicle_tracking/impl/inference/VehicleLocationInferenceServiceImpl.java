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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.DummyOperatorAssignmentServiceImpl;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.trips.TripBean;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.jhlabs.map.proj.ProjectionException;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import tcip_3_0_5_local.NMEA;
import tcip_final_3_0_5_1.CcLocationReport;
import tcip_final_3_0_5_1.CcLocationReport.EmergencyCodes;

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

@Component
public class VehicleLocationInferenceServiceImpl implements
    VehicleLocationInferenceService {

  private static Logger _log = LoggerFactory.getLogger(VehicleLocationInferenceServiceImpl.class);

  private static final DateTimeFormatter XML_DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeParser();

  @Autowired
  private ObservationCache _observationCache;

  @Autowired
  private OutputQueueSenderService _outputQueueSenderService;

  @Autowired
  private VehicleAssignmentService _vehicleAssignmentService;

  @Autowired
  private BundleManagementService _bundleManagementService;

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  private BundleItem _lastBundle = null;

  private ExecutorService _executorService;

  private int _skippedUpdateLogCounter = 0;

  private boolean _bypassInference = false;

  private final ConcurrentMap<AgencyAndId, VehicleInferenceInstance> _vehicleInstancesByVehicleId = 
		  new ConcurrentHashMap<AgencyAndId, VehicleInferenceInstance>();

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

  @PostConstruct
  public void start() {
    _executorService = Executors.newCachedThreadPool();
  }

  @PreDestroy
  public void stop() {
    _executorService.shutdownNow();
  }

  /****
   * Service Methods
   ****/

  @Override
  public void handleNycRawLocationRecord(NycRawLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();
    _bypassInference = false;

    _executorService.execute(new ProcessingTask(record));
  }

  @Override
  public void handleNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record) {
    verifyVehicleResultMappingToCurrentBundle();
    _bypassInference = false;

    _executorService.execute(new ProcessingTask(record));
  }

  @Override
  public void handleBypassUpdateForNycTestInferredLocationRecord(
      NycTestInferredLocationRecord record) {
    _bypassInference = true;

    _executorService.execute(new ProcessingTask(record));
  }

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
    if (gpsData != null) {
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

    final Future<?> result = _executorService.submit(new ProcessingTask(r));
    _bundleManagementService.registerInferenceProcessingThread(result);
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
        if (state.getInferredBlockId() == null
            || state.getInferredTripId() == null) {
          _log.info("Vehicle " + vehicleId
              + " reset on bundle change: no matched trip/block.");

          this.resetVehicleLocation(vehicleId);
          continue;
        }

        // trip or block matched have disappeared!
        final TripBean trip = _nycTransitDataService.getTrip(state.getInferredTripId());
        final BlockBean block = _nycTransitDataService.getBlockForId(state.getInferredBlockId());

        if (trip == null || block == null) {
          _log.info("Vehicle "
              + vehicleId
              + " reset on bundle change: trip/block is no longer present in new bundle.");

          this.resetVehicleLocation(vehicleId);
          continue;
        }
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

    private final NycRawLocationRecord _inferenceRecord;

    private NycTestInferredLocationRecord _nycTestInferredLocationRecord;

    private boolean _simulation = false;

    public ProcessingTask(NycRawLocationRecord record) {
      _vehicleId = record.getVehicleId();
      _inferenceRecord = record;
    }

    public ProcessingTask(NycTestInferredLocationRecord record) {
      _vehicleId = record.getVehicleId();
      _nycTestInferredLocationRecord = record;
      _inferenceRecord = RecordLibrary.getNycTestInferredLocationRecordAsNycRawLocationRecord(record);
      _simulation = true;
    }

    private VehicleInferenceInstance getInstanceForVehicle(AgencyAndId vehicleId) {
      VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);

      if (instance == null) {
        final VehicleInferenceInstance newInstance = _applicationContext.getBean(VehicleInferenceInstance.class);

        instance = _vehicleInstancesByVehicleId.putIfAbsent(vehicleId, newInstance);
        if (instance == null)
          instance = newInstance;

        if (_simulation) {
          final DummyOperatorAssignmentServiceImpl opSvc = new DummyOperatorAssignmentServiceImpl();
          newInstance.setOperatorAssignmentService(opSvc);
          _log.warn("Set operator assignment service to dummy for debugging!");
        }
      }

      return instance;
    }

    @Override
    public void run() {
      try {
        final VehicleInferenceInstance existing = getInstanceForVehicle(_vehicleId);

        if (_simulation) {
          final String assignedRun = _nycTestInferredLocationRecord.getAssignedRunId();
          final String operatorId = _nycTestInferredLocationRecord.getOperatorId();
          if (!Strings.isNullOrEmpty(assignedRun) && !Strings.isNullOrEmpty(operatorId)) {
            DummyOperatorAssignmentServiceImpl opSvc;
            if ((existing.getOperatorAssignmentService() instanceof DummyOperatorAssignmentServiceImpl)) {
              opSvc = (DummyOperatorAssignmentServiceImpl) existing.getOperatorAssignmentService();
              final String[] runParts = assignedRun.split("-");
              opSvc.setOperatorAssignment(new AgencyAndId(
                  _inferenceRecord.getVehicleId().getAgencyId(), operatorId),
                  runParts[1], runParts[0]);
            }
          }
        }

        final boolean passOnRecord = sendRecord(existing);

        if (passOnRecord) {
          // send input "actuals" as inferred result to output queue
          if (_bypassInference == true) {
            final NycQueuedInferredLocationBean record = RecordLibrary.getNycTestInferredLocationRecordAsNycQueuedInferredLocationBean(_nycTestInferredLocationRecord);
            record.setVehicleId(_vehicleId.toString());

            // fix up the service date if we're shifting the dates of the record to now, since
            // the wrong service date will make the TDS not match the trip properly.
            if(record.getServiceDate() == 0) {
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
            // management bean (becomes part of inference bean)
            final NycVehicleManagementStatusBean managementRecord = existing.getCurrentManagementState();
            managementRecord.setInferenceEngineIsPrimary(_outputQueueSenderService.getIsPrimaryInferenceInstance());
            managementRecord.setDepotId(_vehicleAssignmentService.getAssignedDepotForVehicleId(_vehicleId));

            final BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();
            if (currentBundle != null) {
              managementRecord.setActiveBundleId(currentBundle.getId());
            }

            // inference result bean
            final NycQueuedInferredLocationBean record = existing.getCurrentStateAsNycQueuedInferredLocationBean();
            record.setVehicleId(_vehicleId.toString());
            record.setManagementRecord(managementRecord);

            _outputQueueSenderService.enqueue(record);
          }
        }
      } catch (final ProjectionException e) {
        // discard
        _observationCache.purge(_vehicleId);
      } catch (final Throwable ex) {
        _observationCache.purge(_vehicleId);
        _log.error("Error processing new location record for inference on vehicle " + _vehicleId + ": ", ex);
      }
    }

    // sends the record to the inference engine and returns a boolean 
    // of whether to send the result to the queue or not.
    private boolean sendRecord(VehicleInferenceInstance existing) {
      if (_inferenceRecord != null) {
        if (_bypassInference == true) {
          return existing.handleBypassUpdate(_nycTestInferredLocationRecord);
        } else {
          return existing.handleUpdate(_inferenceRecord);
        }
      }

      return false;
    }
  }

  @Override
  public void setSeeds(long cdfSeed, long factorySeed) {
    ParticleFactoryImpl.setSeed(factorySeed);
    BlockStateTransitionModel.setSeed(factorySeed);
    CategoricalDist.setSeed(cdfSeed);
  }
}
