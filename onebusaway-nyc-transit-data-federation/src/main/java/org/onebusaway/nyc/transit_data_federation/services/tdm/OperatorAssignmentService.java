/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
