package org.onebusaway.nyc.transit_data_federation.services.tdm;

import java.util.Collection;
import java.util.Date;

import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;

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
  public Collection<OperatorAssignmentItem> getOperatorsForServiceDate(Date serviceDate) 
      throws Exception;

  public OperatorAssignmentItem getOperatorAssignmentItemForServiceDate(Date serviceDate, 
      String operatorId) throws Exception;

}
