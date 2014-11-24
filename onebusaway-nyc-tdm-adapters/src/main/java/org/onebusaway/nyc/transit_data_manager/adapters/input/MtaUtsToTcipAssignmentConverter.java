package org.onebusaway.nyc.transit_data_manager.adapters.input;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_4_0_0.CPTOperatorIden;
import tcip_final_4_0_0.CPTRowMetaData;
import tcip_final_4_0_0.CPTTransitFacilityIden;
import tcip_final_4_0_0.SCHOperatorAssignment;
import tcip_final_4_0_0.SCHOperatorAssignment.DayTypes;
import tcip_final_4_0_0.SCHOperatorAssignment.Trips;
import tcip_final_4_0_0.SCHRunIden;
import tcip_final_4_0_0.SCHTripIden;

/***
 * 
 * @author sclark
 * 
 */
public class MtaUtsToTcipAssignmentConverter {

  private static Logger _log = LoggerFactory.getLogger(MtaUtsToTcipAssignmentConverter.class);
  
  public MtaUtsToTcipAssignmentConverter() {
    mappingTool = new UtsMappingTool();
  }
  
  private static String DATASOURCE_SYSTEM = "UTS";
  
  private UtsMappingTool mappingTool;
  private DepotIdTranslator depotIdTranslator;

  public void setMappingTool(UtsMappingTool mappingTool) {
    this.mappingTool = mappingTool;
  }

  /***
   * 
   * @param inputAssignment
   * @return
   */
  public SCHOperatorAssignment ConvertToOutput(
      MtaUtsCrewAssignment inputAssignment) {

    if (inputAssignment.getTimestamp() == null) {
      //invalid record, discard
      _log.error("discarding invalid inputAssignment " + inputAssignment.getPassNumberNumericPortion());
      return null;
    }
    
    SCHOperatorAssignment outputAssignment = new SCHOperatorAssignment();

    CPTOperatorIden operator = new CPTOperatorIden();
    operator.setOperatorId(inputAssignment.getPassNumberNumericPortion());
    operator.setAgencyId(mappingTool.getAgencyIdFromUtsAuthId(inputAssignment.getAuthId()));
    operator.setDesignator(inputAssignment.getOperatorDesignator());
    outputAssignment.setOperator(operator);

    SCHRunIden run = new SCHRunIden();
    // Changed with an update to the UTS Crew Assignments page on the wiki.
    // I had asked about run-ids with numbers in them and it turns out that we
    // will just hardcode this field.
    // Simultaneously removing getRunNumberLong from MtaUtsCrewAssignment, so
    // find it in the git history if you need it!
    // run.setRunId(inputAssignment.getRunNumberLong());
    run.setRunId(new Long(0));
    run.setDesignator(inputAssignment.getRunDesignator());
    outputAssignment.setRun(run);

    CPTTransitFacilityIden depot = new CPTTransitFacilityIden();
    depot.setFacilityName(getMappedDepotId(inputAssignment));
    depot.setFacilityId(new Long(0));
    outputAssignment.setOperatorBase(depot);

    // Same boilerplate here as used in sample code provided by MTA.
    SCHTripIden trip = new SCHTripIden();
    trip.setTripId((long) 0);
    Trips trips = new Trips();
    trips.getTrip().add(trip);
    outputAssignment.setTrips(trips);

    // extra boilerplate to satisfy TCIP spec here too, as in the MTA example.
    outputAssignment.setRunType("255");

    DayTypes dt = new DayTypes();
    dt.getDayType().add("255");
    outputAssignment.setDayTypes(dt);

    CPTRowMetaData metaData = new CPTRowMetaData();

    DateTimeFormatter xmlDTF = ISODateTimeFormat.dateTimeNoMillis();

    metaData.setUpdated(xmlDTF.print(inputAssignment.getTimestamp()));

    metaData.setEffective(xmlDTF.print(inputAssignment.getDate()));

    outputAssignment.setMetadata(metaData);

    // Now set the local sch operator assignment.
    tcip_4_0_0_local.SCHOperatorAssignment localOpAssignment = new tcip_4_0_0_local.SCHOperatorAssignment();
    localOpAssignment.setRunRoute(inputAssignment.getRoute());
    outputAssignment.setLocalSCHOperatorAssignment(localOpAssignment);

    return outputAssignment;
  }

  private String getMappedDepotId(MtaUtsCrewAssignment inputAssignment) {
    if (depotIdTranslator != null) {
      return depotIdTranslator.getMappedId(DATASOURCE_SYSTEM, inputAssignment.getDepot());
    } else {
      return inputAssignment.getDepot();
    }
  }
  
  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;    
  }

}
