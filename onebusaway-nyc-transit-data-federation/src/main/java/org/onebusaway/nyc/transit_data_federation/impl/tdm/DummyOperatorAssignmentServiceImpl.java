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
    item.setRunId(runRoute + "-" + runNumber);

    assignmentMap.put(operatorId, item);
  }

}
