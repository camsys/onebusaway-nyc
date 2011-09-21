package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

public class ImporterOperatorAssignmentData implements OperatorAssignmentData {

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

    List<SCHOperatorAssignment> opAssigns = new ArrayList<SCHOperatorAssignment>();

    Iterator<SCHOperatorAssignment> assignsIt = assignmentsData.iterator();
    DateMidnight thisDate = null;
    SCHOperatorAssignment assignment = null;

    while (assignsIt.hasNext()) {
      assignment = assignsIt.next();
      DateTimeFormatter xmlFormat = ISODateTimeFormat.dateTimeNoMillis();
      thisDate = new DateMidnight(
          xmlFormat.parseDateTime(assignment.getMetadata().getEffective()));

      if (inputServiceDate.equals(thisDate)) {
        opAssigns.add(assignment);
      }
    }

    return opAssigns;
  }

}
