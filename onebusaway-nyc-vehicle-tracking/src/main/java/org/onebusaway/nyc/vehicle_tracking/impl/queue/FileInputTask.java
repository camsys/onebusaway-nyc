package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

/**
 * Test/Debug Class for loading realtime data from the archive database 
 * as if it came from the queue.
 * 
 *  This class alters timestamps of the realtime data to make them appear as if they are
 *  current, and meters the flow of data to mimic the original flow.
 *  
 *  To enable this uncomment the bean definition in data-sources.xml.
 *
 */
public class FileInputTask implements ServletContextAware {

  protected static Logger _log = LoggerFactory.getLogger(FileInputTask.class);
  
  private VehicleLocationInferenceService _vehicleLocationService;
  private VehicleAssignmentService _vehicleAssignmentService;
  
  /*
   * Obtain dump_raw.sql via:
   *  mysql -u username -ppassword -h database.host.com 
   *  -e "select raw_message from obanyc_cclocationreport where time_received > subtime(now(), '1:00:00') 
   *  order by time_received" onebusaway_nyc >dump_raw.sql
   */
  private String filename = "/tmp/dump_raw.sql";
  private String[] _depotPartitionKeys = null;
  
  @Autowired
  public void setVehicleLocationService(
      VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }
  
  @Autowired
  public void setVehicleAssignmentService(
      VehicleAssignmentService vehicleAssignmentService) {
    _vehicleAssignmentService = vehicleAssignmentService;
  }
  
  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    // check for depot partition keys in the servlet context
    if (servletContext != null) {
      final String key = servletContext.getInitParameter("depot.partition.key");
      _log.info("servlet context provied depot.partition.key=" + key);
      
      if (key != null) {
        setDepotPartitionKey(key);
      }
    }
  }
  
  @PostConstruct
  public void execute() {
    InsertionThread thread = new InsertionThread(_vehicleLocationService, _vehicleAssignmentService, _depotPartitionKeys, filename);
    new Thread(thread).start();
  }
  
  public void setDepotPartitionKey(String depotPartitionKey) {
    _log.info("depotPartitionKey=" + depotPartitionKey);
    if (depotPartitionKey != null && !depotPartitionKey.isEmpty())
      _depotPartitionKeys = depotPartitionKey.split(",");
    else
      _depotPartitionKeys = null;
  }
  
  private static class InsertionThread implements Runnable {
    private VehicleLocationInferenceService _vehicleLocationService;
    private VehicleAssignmentService vehicleAssignmentService;
    private String[] depotPartitionKeys = null;
    private String filename = null;
    private Reader inputReader = null;
    private StringBuffer currentRecord = new StringBuffer();
    private final ObjectMapper mapper = new ObjectMapper();
    private long startTime = System.currentTimeMillis();
    private long firstRecordTime = 0;

    public InsertionThread(VehicleLocationInferenceService vls, 
        VehicleAssignmentService vas, 
        String[] depotPartitionKeys, 
        String filename) {
      this._vehicleLocationService = vls;
      this.vehicleAssignmentService = vas;
      this.depotPartitionKeys = depotPartitionKeys;
      this.filename = filename;
      final AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
      mapper.getDeserializationConfig().setAnnotationIntrospector(jaxb);
    }
    
    public void run() {
      try {
        open();
        while (next()) {
          RealtimeEnvelope record = parseCurrentRecord();
          postProcessResult(record);
          processResult(record);
        }
      } catch (Exception e) {
        _log.error("fail: ", e);
      }
    }

    private void postProcessResult(RealtimeEnvelope record) throws Exception {
      if (record == null)
    	return;
      // startTime -- program boot
      // now -- right now
      long now = System.currentTimeMillis();
      long localOffset = now - startTime;
      // firstRecordTime: lazy init to first record received time
      if (firstRecordTime == 0) {
        firstRecordTime = record.getTimeReceived();
      }
      long dataOffset = record.getTimeReceived()- firstRecordTime;
      while (dataOffset > localOffset) {
    	Thread.sleep(250);
        now = System.currentTimeMillis();
        localOffset = now - startTime;
      }
      
      record.setTimeReceived(now);
    }

    private boolean acceptMessage(RealtimeEnvelope envelope) {
      if (envelope == null || envelope.getCcLocationReport() == null)
        return false;

      final CcLocationReport message = envelope.getCcLocationReport();
      final ArrayList<AgencyAndId> vehicleList = new ArrayList<AgencyAndId>();
      
      if(depotPartitionKeys == null)
        return false;
      
      for (final String key : depotPartitionKeys) {
        try {
          vehicleList.addAll(vehicleAssignmentService.getAssignedVehicleIdsForDepot(key));
        } catch (final Exception e) {
          _log.warn("Error fetching assigned vehicles for depot " + key
              + "; will retry.");
          continue;
        }
      }
      
      final CPTVehicleIden vehicleIdent = message.getVehicle();
      final AgencyAndId vehicleId = new AgencyAndId(
          vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId() + "");

      return vehicleList.contains(vehicleId);
    }
    private void processResult(RealtimeEnvelope record) {
      if (acceptMessage(record)) {
        _vehicleLocationService.handleRealtimeEnvelopeRecord(record);
      }
      
    }

    private RealtimeEnvelope parseCurrentRecord() throws Exception {
      String s = currentRecord.toString();
      RealtimeEnvelope record = deserializeMessage(s);
      return record;
    }
    
    public RealtimeEnvelope deserializeMessage(String contents) {
      RealtimeEnvelope message = null;
      try {
    	  contents = "{" + contents + "}";
    	  contents = contents.replaceFirst("UUID.*UUID", "UUID");

        final JsonNode wrappedMessage = mapper.readValue(contents,
            JsonNode.class);
        final String ccLocationReportString = wrappedMessage.get(
            "RealtimeEnvelope").toString();
        String msg = ccLocationReportString.replace("vehiclepowerstate", "vehiclePowerState");

        message = mapper.readValue(msg,
            RealtimeEnvelope.class);
      } catch (final Exception e) {
        _log.warn("Received corrupted message from queue; discarding: "
            + e.getMessage());
        _log.warn("Contents: " + contents);
      }
      return message;
    }

    // advance record pointer
    private boolean next() throws Exception {
      currentRecord = new StringBuffer();
      int i;
      while ((i = inputReader.read()) > 0) {
        char c = (char) i;
        if (c == '\n') {
          currentRecord.append(c);
          // read last space
          inputReader.read();
          return true;
        } else {
          currentRecord.append(c);
        }
      }
      return false;
    }

    private void open() throws Exception {
    	inputReader = new FileReader(filename);
 
    }
  }
}
