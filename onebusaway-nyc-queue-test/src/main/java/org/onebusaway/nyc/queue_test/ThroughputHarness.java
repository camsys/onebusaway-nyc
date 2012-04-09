/**
 * Send a controlled amount of data at realtime and inference queues to test
 * archive throughput.
 */
package org.onebusaway.nyc.queue_test;

import com.eaio.uuid.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.zeromq.ZMQ;

import tcip_3_0_5_local.NMEA;
import tcip_final_3_0_5_1.CPTOperatorIden;
import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;
import tcip_final_3_0_5_1.SCHRouteIden;
import tcip_final_3_0_5_1.SCHRunIden;
import tcip_final_3_0_5_1.SPDataQuality;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import lrms_final_09_07.Angle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThroughputHarness {

  private static Logger _log = LoggerFactory.getLogger(ThroughputHarness.class);
  // largest value in db
  private static final double MAX_DISTANCE_ALONG_BLOCK = 377270.390814062; 
  private static final double MAX_DISTANCE_ALONG_TRIP = 96028.2459595257;
  private static final double MIN_LAT = 40.221755;
  private static final double MAX_LAT = 40.780869;
  private static final double MIN_LONG = -74.252339;
  private static final double MAX_LONG = -73.388287;
  private static final int MIN_DSC = 0; // actually 1346
  private static final int MAX_DSC = 8000;
  private static final int MAX_VEHICLES = 8000;
  private static final int MIN_OPERATOR = 0;
  private static final int MAX_OPERATOR = 100000; // fake
  private static final int MIN_REQUEST = 1;
  private static final int MAX_REQUEST = 1286650;
  private static final int MIN_ROUTE = 1;
  private static final int MAX_ROUTE = 99;
  private static final int MIN_RUN = 0;
  private static final int MAX_RUN = 100000; // fake
  private static final int MIN_SPEED = -30;
  private static final int MAX_SPEED = 149;
  private static final String[] DEPOTS = {
      "CA", "JG", "CAST", "CH", "CHAR", "YU", "YUK", "MA", "FR", "JA", "FB",
      "JK", "MV", "KB", "YO", "QV", "GA", "EN", "FP", "SC", "EA", "OH", "OS",
      "BP", "RC", "WF", "MQ", "UP", "CS", "CP", "LA", "NL", "GH", "FRK", "JAM",
      "FLAT", "JFK", "MANVI", "YNK", "GRAND", "ENY", "SPRCK", "ECH", "100th",
      "126", "BPK", "RVC", "MJQ", "ULPK", "STG", "LAG", "NJL"};
  private ObjectMapper _ccmapper;
  private ObjectMapper _imapper;

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("usage: ThroughputHarness realtimeQueue_host realtimeQueue_port inferenceQueue_host inferenceQueue_port sends/second");
      System.exit(-1);
    }
    ThroughputHarness harness = new ThroughputHarness();
    harness.main(args[0], Integer.decode(args[1]), args[2],
        Integer.decode(args[3]), Integer.decode(args[4]));
  }

  public void main(String realtimeHost, int realtimePort, String inferenceHost,
      int inferencePort, int sends) {

    _log.warn("opening context");
    ZMQ.Context context = ZMQ.context(1);
    TestPublisher realtimePublisher = new TestPublisher(context, "bhs_queue");
    realtimePublisher.open("tcp", realtimeHost, realtimePort);
    TestPublisher inferencePublisher = new TestPublisher(context,
        "inference_queue");
    inferencePublisher.open("tcp", inferenceHost, inferencePort);

    setupMappers();

    int vehicleCount = 0;
    long timeStamp = System.currentTimeMillis();
    int sent = 0;
    while (!Thread.currentThread().isInterrupted()) {
      long now = System.currentTimeMillis();
      if ((now - timeStamp) > 1000) {
        // 1 second has passed, reset
        System.out.println("sent " + sent + " messages in " + (now - timeStamp)
            + " milliseconds.");
        sent = 0;
        timeStamp = System.currentTimeMillis();
      } else if (sent < sends) {
        // not throttled, send
        String uuid = createUUID();
        realtimePublisher.sendAndWrap(
            createRealtimeMessage(vehicleCount, timeStamp), uuid);
        inferencePublisher.send(createInferenceMessage(uuid, vehicleCount,
            timeStamp));
        sent++;
        vehicleCount++;
        if (vehicleCount > MAX_VEHICLES) {
          vehicleCount = 0;
        }
      } else {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          // bury
        }
      }
    }

  }

  private void setupMappers() {
    _ccmapper = new ObjectMapper();
    HashMap<String, String> exceptions = new HashMap<String, String>();
    exceptions.put("destSignCode", "destSignCode");
    exceptions.put("operatorID", "operatorID");
    exceptions.put("routeID", "routeID");
    exceptions.put("runID", "runID");
    exceptions.put("recordTimestamp", "recordTimestamp");
    exceptions.put("vehicleId", "vehicleId");

    _ccmapper.setPropertyNamingStrategy(new SimplePropertyNamingStrategy(
        exceptions));

    _imapper = new ObjectMapper();

  }

  private String createUUID() {
    return new UUID().toString();
  }

  private String createRealtimeMessage(int vehicleCount, long timeStamp) {
    CcLocationReport m = new CcLocationReport();
    m.setDataQuality(new SPDataQuality());
    m.getDataQuality().setQualitativeIndicator("4");
    m.setDestSignCode(new Long(distributeInt(MIN_DSC, MAX_DSC)));
    m.setDirection(new Angle());
    m.getDirection().setDeg(new BigDecimal(distributeDouble(0, 360)));
    m.setLatitude((int) Math.round(distributeDouble(MIN_LAT, MAX_LAT) * 1000000));
    m.setLongitude((int) Math.round(distributeDouble(MIN_LONG, MAX_LONG) * 1000000));
    m.setManufacturerData("VFTP123456789");
    m.setOperatorID(new CPTOperatorIden());
    m.getOperatorID().setOperatorId(0);
    m.getOperatorID().setDesignator("" + distributeInt(0, MAX_OPERATOR));
    m.setRequestId(distributeInt(MIN_REQUEST, MAX_REQUEST));
    m.setRouteID(new SCHRouteIden());
    m.getRouteID().setRouteId(0);
    m.getRouteID().setRouteDesignator("" + distributeInt(MIN_ROUTE, MAX_ROUTE));
    m.setRunID(new SCHRunIden());
    m.getRunID().setRunId(0);
    m.getRunID().setDesignator("" + distributeInt(MIN_RUN, MAX_RUN));
    m.setSpeed((short) distributeInt(MIN_SPEED, MAX_SPEED).intValue());
    m.setStatusInfo(0);
    m.setTimeReported(toDate(timeStamp));
    m.setVehicle(new CPTVehicleIden());
    m.getVehicle().setAgencydesignator("MTA NYCT");
    m.getVehicle().setAgencyId(2008l);
    m.getVehicle().setVehicleId(vehicleCount);
    m.setLocalCcLocationReport(new tcip_3_0_5_local.CcLocationReport());
    m.getLocalCcLocationReport().setNMEA(new NMEA());
    m.getLocalCcLocationReport().getNMEA().getSentence().add(
        "$GPRMC,105850.00,A,4038.445646,N,07401.094043,W,002.642,128.77,220611,,,A*7C");
    m.getLocalCcLocationReport().getNMEA().getSentence().add(
        "$GPGGA,105850.000,4038.44565,N,07401.09404,W,1,09,01.7,+00042.0,M,,M,,*49");
    // serialize to json
    try {
      return _ccmapper.writeValueAsString(m);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

  }

  private byte[] createInferenceMessage(String uuid, int vehicleCount,
      long timeStamp) {
    NycQueuedInferredLocationBean r = new NycQueuedInferredLocationBean();
    r.setRecordTimestamp(timeStamp);
    r.setVehicleId("MTA NYCT_" + vehicleCount);
    r.setServiceDate(timeStamp);
    r.setScheduleDeviation(0);
    r.setBlockId("" + vehicleCount);
    r.setTripId("" + vehicleCount);
    r.setDistanceAlongBlock(distributeDouble(0.0, MAX_DISTANCE_ALONG_BLOCK));
    r.setDistanceAlongTrip(distributeDouble(0.0, MAX_DISTANCE_ALONG_TRIP));
    r.setInferredLatitude(distributeDouble(MIN_LAT, MAX_LAT));
    r.setInferredLongitude(distributeDouble(MIN_LONG, MAX_LONG));
    r.setObservedLatitude(distributeDouble(MIN_LAT, MAX_LAT));
    r.setObservedLongitude(distributeDouble(MIN_LONG, MAX_LONG));
    r.setPhase("phase");
    r.setStatus("status");
    r.setManagementRecord(createManagementRecord(uuid, vehicleCount, timeStamp));
    r.setRunId("" + distributeInt(MIN_RUN, MAX_RUN));
    r.setRouteId("" + distributeInt(MIN_ROUTE, MAX_ROUTE));
    r.setBearing(distributeInt(0, 360));

    try {
      return _imapper.writeValueAsString(r).getBytes();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private Double distributeDouble(double min, double max) {
    // generate random value between min and max
    return min + (int) (Math.random() * ((max - min) + 1));
  }

  private Integer distributeInt(int min, int max) {
    return (int) (min + (int) (Math.random() * ((max - min) + 1)));
  }

  private NycVehicleManagementStatusBean createManagementRecord(String uuid,
      int vehicleCount, long timeStamp) {
    NycVehicleManagementStatusBean r = new NycVehicleManagementStatusBean();
    r.setUUID(uuid);
    r.setInferenceIsEnabled(true);
    r.setLastUpdateTime(timeStamp);
    r.setLastLocationUpdateTime(timeStamp);
    r.setLastObservedLatitude((int) Math.round(distributeDouble(MIN_LAT,
        MAX_LAT) * 1000000));
    r.setLastObservedLongitude((int) Math.round(distributeDouble(MIN_LONG,
        MAX_LONG) * 1000000));
    r.setMostRecentObservedDestinationSignCode(""
        + distributeInt(MIN_DSC, MAX_DSC));
    r.setLastInferredDestinationSignCode("" + distributeInt(MIN_DSC, MAX_DSC));
    r.setInferenceEngineIsPrimary(true);
    r.setActiveBundleId("foo");
    r.setInferenceIsFormal(true);
    r.setDepotId(distributeDepot());
    r.setEmergencyFlag(false);
    r.setLastInferredOperatorId("" + distributeInt(MIN_OPERATOR, MAX_OPERATOR));
    r.setInferredRunId("" + distributeInt(MIN_RUN, MAX_RUN));
    r.setAssignedRunId("" + distributeInt(MIN_RUN, MAX_RUN));
    return r;
  }

  private String distributeDepot() {
    return DEPOTS[distributeInt(0, DEPOTS.length - 1)];
  }

  private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSS-04:00");

  private String toDate(long timestamp) {
    return dateFormatter.format(new Date(timestamp));
  }

}