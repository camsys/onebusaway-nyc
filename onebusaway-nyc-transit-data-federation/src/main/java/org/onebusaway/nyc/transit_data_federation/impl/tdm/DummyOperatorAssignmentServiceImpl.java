package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * This class is used for simulated playback. With it we can test operator-assigned
 * results given a truth value from a trace.
 * 
 * @author bwillard
 * 
 */
public class DummyOperatorAssignmentServiceImpl implements OperatorAssignmentService {

  Map<AgencyAndId, OperatorAssignmentItem> assignmentMap = Maps.newHashMap();

  @Override
  public Collection<OperatorAssignmentItem> getOperatorsForServiceDate(
		  ServiceDate serviceDate) throws Exception {
    return Collections.emptyList();
  }

  @Override
  public OperatorAssignmentItem getOperatorAssignmentItemForServiceDate(
      ServiceDate serviceDate, AgencyAndId operatorId) throws Exception {
    return assignmentMap.get(operatorId);
  }

  public void setOperatorAssignment(AgencyAndId operatorId, String runNumber,
      String runRoute) {
    final OperatorAssignmentItem item = new OperatorAssignmentItem();
    item.setAgencyId(operatorId.getAgencyId());
    item.setRunNumber(runNumber);
    item.setRunRoute(runRoute);

    assignmentMap.put(operatorId, item);
  }

}
