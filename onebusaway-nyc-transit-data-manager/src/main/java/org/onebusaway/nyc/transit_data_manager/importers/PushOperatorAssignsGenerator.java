package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import tcip_final_3_0_5_1.CPTPushHeader;
import tcip_final_3_0_5_1.ObjectFactory;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SchPushOperatorAssignments;
import tcip_final_3_0_5_1.SchPushOperatorAssignments.Assignments;

public class PushOperatorAssignsGenerator {

	private GregorianCalendar headerCal;
	
	public PushOperatorAssignsGenerator(GregorianCalendar headerCal) {
		super();
		this.headerCal = headerCal;
	}
	
	public SchPushOperatorAssignments generateFromOpAssignList (List<SCHOperatorAssignment> opAssignList) {
				
		SchPushOperatorAssignments resultOpAssigns = new SchPushOperatorAssignments();
		
		resultOpAssigns.setPushHeader(generatePushHeader());
		resultOpAssigns.setAssignments(generateAssignments(opAssignList));
		
		return resultOpAssigns;
	}
	
	private CPTPushHeader generatePushHeader () {
		DatatypeFactory df = null;
		try {
			df = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		XMLGregorianCalendar xmlHeaderCal = df.newXMLGregorianCalendar(headerCal);
		
		CPTPushHeader ph = new CPTPushHeader();
		
		ph.setFileType("operator-assignments-file");
		ph.setEffective(xmlHeaderCal);
		ph.setSource(0);
		ph.setUpdatesOnly(false);
		ph.setUpdatesThru(xmlHeaderCal);
		ph.setTimeSent(xmlHeaderCal);
		
		return ph;
	}
	
	private SchPushOperatorAssignments.Assignments generateAssignments(List<SCHOperatorAssignment> assignmentList) {
		SchPushOperatorAssignments.Assignments assignmentsBlock = new SchPushOperatorAssignments.Assignments();
		
		// iterate over assignmentList and add each element using assignmentsBlock.getAssignment().add()
		Iterator<SCHOperatorAssignment> itr = assignmentList.iterator();
		while (itr.hasNext()) {
			assignmentsBlock.getAssignment().add(itr.next());
		}
		
		return assignmentsBlock;
	}
}
