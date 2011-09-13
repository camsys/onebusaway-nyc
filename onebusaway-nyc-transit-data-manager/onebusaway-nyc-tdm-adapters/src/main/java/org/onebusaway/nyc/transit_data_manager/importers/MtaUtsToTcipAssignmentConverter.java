package org.onebusaway.nyc.transit_data_manager.importers;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.onebusaway.nyc.transit_data_manager.importers.tools.UtsMappingTool;
import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import tcip_final_3_0_5_1.CPTOperatorIden;
import tcip_final_3_0_5_1.CPTRowMetaData;
import tcip_final_3_0_5_1.CPTTransitFacilityIden;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SCHOperatorAssignment.DayTypes;
import tcip_final_3_0_5_1.SCHOperatorAssignment.Trips;
import tcip_final_3_0_5_1.SCHRunIden;
import tcip_final_3_0_5_1.SCHTripIden;

/***
 * 
 * @author sclark
 *
 */
public class MtaUtsToTcipAssignmentConverter {

	UtsMappingTool mappingTool = null;
	
	public MtaUtsToTcipAssignmentConverter() {
	}
	
	/***
	 * 
	 * @param inputAssignment
	 * @return
	 */
	public SCHOperatorAssignment ConvertToOutput (MtaUtsCrewAssignment inputAssignment) {
		DatatypeFactory df = null;
		
		try {
			df = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		mappingTool = new UtsMappingTool();
		
		SCHOperatorAssignment outputAssignment = new SCHOperatorAssignment();
		
		CPTOperatorIden operator = new CPTOperatorIden();
		operator.setOperatorId(inputAssignment.getPassNumberNumericPortion());
		operator.setAgencyId(mappingTool.getAgencyIdFromUtsAuthId(inputAssignment.getAuthId()));
		operator.setDesignator(inputAssignment.getOperatorDesignator());
		outputAssignment.setOperator(operator);
		
		SCHRunIden run = new SCHRunIden();
		// Changed with an update to the UTS Crew Assignments page on the wiki.
		// I had asked about run-ids with numbers in them and it turns out that we will just hardcode this field.
		// Simultaneously removing getRunNumberLong from MtaUtsCrewAssignment, so find it in the git history if you need it!
		//run.setRunId(inputAssignment.getRunNumberLong());
		run.setRunId(new Long(0));
		run.setDesignator(inputAssignment.getRunDesignator());
		outputAssignment.setRun(run);
		
		CPTTransitFacilityIden depot = new CPTTransitFacilityIden();
		depot.setFacilityName(inputAssignment.getDepot());
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
		
		XMLGregorianCalendar updateDate = df.newXMLGregorianCalendar(inputAssignment.getTimestamp());
		metaData.setUpdated(mappingTool.timestampToDateString(inputAssignment.getTimestamp()));
		XMLGregorianCalendar effectiveDate = df.newXMLGregorianCalendar(inputAssignment.getDate());
		metaData.setEffective(mappingTool.dateToDateString(inputAssignment.getDate()));
		
		outputAssignment.setMetadata(metaData);
		
		// Now set the local sch operator assignment.
		tcip_3_0_5_local.SCHOperatorAssignment localOpAssignment = new tcip_3_0_5_local.SCHOperatorAssignment();
		localOpAssignment.setRunRoute(inputAssignment.getRoute());
		outputAssignment.setLocalSCHOperatorAssignment(localOpAssignment);
		
		return outputAssignment;
	}
	
}
