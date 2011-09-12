package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import tcip_final_3_0_5_1.CPTPushHeader;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SchPushOperatorAssignments;

public class TCIPCrewAssignmentsOutputConverter implements CrewAssignmentsOutputConverter {

	private List<MtaUtsCrewAssignment> crewAssignInputData = null;
	
	public TCIPCrewAssignmentsOutputConverter (List<MtaUtsCrewAssignment> data) {
		crewAssignInputData = data;
	}
	
	
	public List<SCHOperatorAssignment> convertAssignments () {
		MtaUtsToTcipAssignmentConverter dataConverter = new MtaUtsToTcipAssignmentConverter();
		
		List<SCHOperatorAssignment> opAssigns = new ArrayList<SCHOperatorAssignment>();
		
		Iterator<MtaUtsCrewAssignment> itr = crewAssignInputData.iterator();
		
		SCHOperatorAssignment opAssign = null;
		
		while (itr.hasNext()) {
			opAssign = dataConverter.ConvertToOutput(itr.next());
			opAssigns.add(opAssign);
		}
		
		return opAssigns;
	}
}
