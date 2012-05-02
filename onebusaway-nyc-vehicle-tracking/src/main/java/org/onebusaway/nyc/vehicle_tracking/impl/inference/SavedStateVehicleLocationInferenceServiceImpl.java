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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.onebusaway.nyc.vehicle_tracking.model.VehicleInferenceInstanceState;
import org.onebusaway.nyc.vehicle_tracking.model.library.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.utility.ObjectSerializationLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import tcip_3_0_5_local.NMEA;
import tcip_final_3_0_5_1.CcLocationReport;
import tcip_final_3_0_5_1.CcLocationReport.EmergencyCodes;

import com.google.common.base.Strings;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.jhlabs.map.proj.ProjectionException;

@Component
public class SavedStateVehicleLocationInferenceServiceImpl implements VehicleLocationInferenceService {

  private static Logger _log = LoggerFactory.getLogger(VehicleLocationInferenceServiceImpl.class);

  private static final DateTimeFormatter XML_DATE_TIME_FORMAT = ISODateTimeFormat.dateTimeParser();

  @Autowired
  private OutputQueueSenderService _outputQueueSenderService;

  @Autowired
  private VehicleAssignmentService _vehicleAssignmentService;

  @Autowired
  private BundleManagementService _bundleManagementService;

  private boolean _simulation = false;
  
  private NycRawLocationRecord _inferenceRecord;

  private NycTestInferredLocationRecord _nycTestInferredLocationRecord;

  private AgencyAndId _vehicleId = null;
  
  private VehicleInferenceInstance _inferenceInstance = null;

  @Autowired
  private ApplicationContext _applicationContext;

  /****
   * Service Methods
   ****/

  @Override
  public void handleNycRawLocationRecord(NycRawLocationRecord record) {
	  _inferenceInstance = _applicationContext.getBean(VehicleInferenceInstance.class);

	  _vehicleId = record.getVehicleId();
      _inferenceRecord = record;
      run();
  }

  @Override
  public void handleNycTestInferredLocationRecord(NycTestInferredLocationRecord record) {
	  _inferenceInstance = _applicationContext.getBean(VehicleInferenceInstance.class);

		try {
			VehicleInferenceInstanceState s = ObjectSerializationLibrary.readObject(new File("/tmp/test"));
			 _inferenceInstance.setState(s);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	 
	  
	  _vehicleId = record.getVehicleId();
      _inferenceRecord = RecordLibrary.getNycTestInferredLocationRecordAsNycRawLocationRecord(record);

      _nycTestInferredLocationRecord = record;
      _simulation = true;
      
      DummyOperatorAssignmentServiceImpl opSvc = new DummyOperatorAssignmentServiceImpl();
      _inferenceInstance.setOperatorAssignmentService(opSvc);
      _log.warn("Set operator assignment service to dummy!");
      run();
      
	try {
		ObjectSerializationLibrary.writeObject(new File("/tmp/test"), _inferenceInstance.getState());
	} catch (IOException e) {
		e.printStackTrace();
	}
  }

  @Override
  public void handleRealtimeEnvelopeRecord(RealtimeEnvelope envelope) {
    CcLocationReport message = envelope.getCcLocationReport();
    
    // construct raw record
    NycRawLocationRecord r = new NycRawLocationRecord();
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
    String RMCSentence = r.getRmc();

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
                  + "Difference in hours: " + (differenceInSeconds / 60 / 60) + "\n" 
                  + "Raw timestamp: " + message.getTimeReported() + "\n"
                  + "From RMC: " + datePart + " " + timePart);
            }
          } catch (final ParseException e) {
            _log.debug("Unparseable date: " + datePart + " " + timePart);
          }
        }
      }
    }

    handleNycRawLocationRecord(r);
  }

  @Override
  public NycTestInferredLocationRecord getNycTestInferredLocationRecordForVehicle(AgencyAndId vid) {
    NycTestInferredLocationRecord record = _inferenceInstance.getCurrentState();
    if (record != null)
      record.setVehicleId(vid);
    return record;
  }

  @Override
  public void resetVehicleLocation(AgencyAndId vid) {	
	  // no state is maintained, so nothing can be reset!
  }

  @Override
  public void setVehicleStatus(AgencyAndId vid, boolean enabled) {
	  // no state is maintained, so nothing can be done here!
  }

  /****
   * Debugging Methods
   ****/

  @Override
  public List<NycTestInferredLocationRecord> getLatestProcessedVehicleLocationRecords() {
    final List<NycTestInferredLocationRecord> records = new ArrayList<NycTestInferredLocationRecord>();

    if (_inferenceInstance != null) {
      final NycTestInferredLocationRecord record = _inferenceInstance.getCurrentState();
      if (record != null) {
        record.setVehicleId(_vehicleId);
        records.add(record);
      }
    } else {
      _log.warn("No records found for vid=" + _vehicleId);
    }

    return records;
  }
  
  @Override
  public Multiset<Particle> getCurrentParticlesForVehicleId(
      AgencyAndId vehicleId) {
    if (_inferenceInstance == null)
      return null;
    return _inferenceInstance.getCurrentParticles();
  }

  @Override
  public Multiset<Particle> getCurrentSampledParticlesForVehicleId(
      AgencyAndId vehicleId) {
    if (_inferenceInstance == null)
      return null;
    return _inferenceInstance.getCurrentSampledParticles();
  }

  @Override
  public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
      AgencyAndId vehicleId) {
    if (_inferenceInstance == null)
      return Collections.emptyList();
    return _inferenceInstance.getJourneySummaries();
  }

  @Override
  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId) {
    if (_inferenceInstance == null)
      return null;
    final VehicleLocationDetails details = _inferenceInstance.getDetails();
    details.setVehicleId(vehicleId);
    return details;
  }

  @Override
  public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId) {
    if (_inferenceInstance == null)
      return null;
    final VehicleLocationDetails details = _inferenceInstance.getBadParticleDetails();
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

  public void run() {
	  try {
		  if (_simulation) {
			  String actualRun = _nycTestInferredLocationRecord.getActualRunId();
			  String operatorId = _nycTestInferredLocationRecord.getOperatorId();
			  if (!Strings.isNullOrEmpty(actualRun) && !Strings.isNullOrEmpty(operatorId)) {
				  DummyOperatorAssignmentServiceImpl opSvc;
				  if ((_inferenceInstance.getOperatorAssignmentService() instanceof DummyOperatorAssignmentServiceImpl)) {
					  opSvc = (DummyOperatorAssignmentServiceImpl)_inferenceInstance.getOperatorAssignmentService();
					  String[] runParts = actualRun.split("-");

					  opSvc.setOperatorAssignment(
							  new AgencyAndId(_inferenceRecord.getVehicleId().getAgencyId(), operatorId), 
							  runParts[1], runParts[0]);
				  }
			  }
		  }

		  
		  boolean passOnRecord = false;
		  if (_inferenceRecord != null) {
			  passOnRecord = _inferenceInstance.handleUpdate(_inferenceRecord);
		  } else if (_nycTestInferredLocationRecord != null) {
			  passOnRecord =  _inferenceInstance.handleBypassUpdate(_nycTestInferredLocationRecord);
		  }


		  if (passOnRecord) {
			  // management bean (becomes part of inference bean)
			  NycVehicleManagementStatusBean managementRecord = _inferenceInstance.getCurrentManagementState();
			  managementRecord.setInferenceEngineIsPrimary(_outputQueueSenderService.getIsPrimaryInferenceInstance());
			  managementRecord.setDepotId(_vehicleAssignmentService.getAssignedDepotForVehicleId(_vehicleId));

			  BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();
			  if (currentBundle != null) {
				  managementRecord.setActiveBundleId(currentBundle.getId());
			  }

			  // inference result bean
			  NycQueuedInferredLocationBean record = _inferenceInstance.getCurrentStateAsNycQueuedInferredLocationBean();
			  record.setVehicleId(_vehicleId.toString());
			  record.setManagementRecord(managementRecord);

			  _outputQueueSenderService.enqueue(record);
		  }
	  } catch (final ProjectionException e) {
		  // discard
	  } catch (final Throwable ex) {
		  _log.error("Error processing new location record for inference: ", ex);
	  }
  } 

  @Override
  public void setSeeds(long cdfSeed, long factorySeed) {
    ParticleFactoryImpl.setSeed(factorySeed);
    BlockStateTransitionModel.setSeed(factorySeed);
    CategoricalDist.setSeed(cdfSeed);
  }
}
