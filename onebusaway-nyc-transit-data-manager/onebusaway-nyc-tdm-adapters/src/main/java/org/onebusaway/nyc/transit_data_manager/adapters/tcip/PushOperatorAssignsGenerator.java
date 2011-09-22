package org.onebusaway.nyc.transit_data_manager.adapters.tcip;

import java.util.Iterator;
import java.util.List;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

import tcip_final_3_0_5_1.CPTPushHeader;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SchPushOperatorAssignments;

public class PushOperatorAssignsGenerator {

  private DateTime nowDateTime;
  private DateMidnight headerEffectiveDate;
  private UtsMappingTool mappingTool = null;

  public PushOperatorAssignsGenerator(DateMidnight headerEffectiveDate) {
    super();
    this.nowDateTime = new DateTime();
    this.headerEffectiveDate = headerEffectiveDate;
    this.mappingTool = new UtsMappingTool();
  }

  public SchPushOperatorAssignments generateFromOpAssignList(
      List<SCHOperatorAssignment> opAssignList) {

    SchPushOperatorAssignments resultOpAssigns = new SchPushOperatorAssignments();

    resultOpAssigns.setPushHeader(generatePushHeader());
    resultOpAssigns.setAssignments(generateAssignments(opAssignList));

    return resultOpAssigns;
  }

  private CPTPushHeader generatePushHeader() {
    CPTPushHeader ph = new CPTPushHeader();

    ph.setFileType("operator-assignments-file");
    ph.setEffective(mappingTool.dateTimeToXmlDatetimeFormat(headerEffectiveDate));
    ph.setSource(0);
    ph.setUpdatesOnly(false);
    ph.setUpdatesThru(mappingTool.dateTimeToXmlDatetimeFormat(nowDateTime));
    ph.setTimeSent(mappingTool.dateTimeToXmlDatetimeFormat(nowDateTime));

    return ph;
  }

  private SchPushOperatorAssignments.Assignments generateAssignments(
      List<SCHOperatorAssignment> assignmentList) {
    SchPushOperatorAssignments.Assignments assignmentsBlock = new SchPushOperatorAssignments.Assignments();

    // iterate over assignmentList and add each element using
    // assignmentsBlock.getAssignment().add()
    Iterator<SCHOperatorAssignment> itr = assignmentList.iterator();
    while (itr.hasNext()) {
      assignmentsBlock.getAssignment().add(itr.next());
    }

    return assignmentsBlock;
  }
}
