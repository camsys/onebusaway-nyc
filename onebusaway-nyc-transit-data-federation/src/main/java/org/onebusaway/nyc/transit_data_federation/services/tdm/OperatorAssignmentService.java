package org.onebusaway.nyc.transit_data_federation.services.tdm;

import java.util.ArrayList;
import java.util.Date;

import org.onebusaway.nyc.transit_data_federation.impl.tdm.model.OperatorAssignmentItem;

/**
 * Service interface for getting which operators are scheduled to be on the job.
 * 
 * @author jmaki
 */
public interface OperatorAssignmentService {

  /**
   * Get a list of operators scheduled to a run.
   * 
   * @param serviceDate The service date to return scheduled operators for.
   */
  public ArrayList<OperatorAssignmentItem> getOperatorsForServiceDate(Date serviceDate);

  public OperatorAssignmentItem getOperatorAssignmentItem(Date today,
      String operatorId);

}
