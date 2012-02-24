package org.onebusaway.nyc.transit_data_federation.services.tdm;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;

import java.util.Collection;

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
  public Collection<OperatorAssignmentItem> getOperatorsForServiceDate(ServiceDate serviceDate) 
      throws Exception;

  public OperatorAssignmentItem getOperatorAssignmentItemForServiceDate(
      ServiceDate serviceDate, AgencyAndId operatorId) throws Exception;

}
