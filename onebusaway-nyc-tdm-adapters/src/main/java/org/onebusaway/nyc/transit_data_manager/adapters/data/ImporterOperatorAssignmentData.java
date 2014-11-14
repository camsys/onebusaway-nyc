package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_4_0_0_0.SCHOperatorAssignment;

public class ImporterOperatorAssignmentData implements OperatorAssignmentData {
  
  private static Logger _log = LoggerFactory.getLogger(ImporterOperatorAssignmentData.class);

  private List<SCHOperatorAssignment> assignmentsData = null;

  public ImporterOperatorAssignmentData(
      List<SCHOperatorAssignment> assignmentsData) {
    this.assignmentsData = assignmentsData;
  }

  public List<DateMidnight> getAllServiceDates() {
    List<DateMidnight> serviceDates = new ArrayList<DateMidnight>();

    // Basically just iterate through assignmentsData and build a list of all
    // the unique service dates.
    Iterator<SCHOperatorAssignment> assignsIt = assignmentsData.iterator();
    DateMidnight serviceDate = null;

    while (assignsIt.hasNext()) {
      DateTimeFormatter xmlFormat = ISODateTimeFormat.dateTimeNoMillis();
      serviceDate = new DateMidnight(
          xmlFormat.parseDateTime(assignsIt.next().getMetadata().getEffective()));

      if (!serviceDates.contains(serviceDate)) {
        serviceDates.add(serviceDate);
      }
    }

    return serviceDates;
  }

  // This will work similarly to getAllServiceDates above
  public List<SCHOperatorAssignment> getOperatorAssignmentsByServiceDate(
      DateMidnight inputServiceDate) {
    
    // check for null input value
    if (inputServiceDate == null)
      return new ArrayList<SCHOperatorAssignment>();
    
    _log.debug("getting Operator Assignments for the service date " + inputServiceDate.toString());
    
    List<SCHOperatorAssignment> opAssigns = new ArrayList<SCHOperatorAssignment>();

    _log.debug("About to loop over " + assignmentsData.size() + " SCHOperatorAssignment objects, checking the service date of each one.");
    
    for (SCHOperatorAssignment opAssign : assignmentsData) {
      DateTimeFormatter xmlFormat = ISODateTimeFormat.dateTimeNoMillis();
      DateMidnight thisDate = new DateMidnight(
          xmlFormat.parseDateTime(opAssign.getMetadata().getEffective()));
      
      if (inputServiceDate.equals(thisDate)) {
        opAssigns.add(opAssign);
      }      
    }

    _log.debug("Returning " + opAssigns.size() + " results for this service date.");
    
    return opAssigns;
  }

}
