/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.actions.api.where;

import java.util.List;

import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.services.AlarmService;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/cancel-alarms")
public class CancelAlarmsResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  @Autowired
  private AlarmService _alarmService;

  private List<String> _ids;

  public CancelAlarmsResource() {
    super(V2);
  }

  @QueryParam("Ids")
  public void setId(List<String> ids) {
    _ids = ids;
  }

  @GET
  public Response index() throws ServiceException {

    if (hasErrors())
      return getValidationErrorsResponse();

    if (_ids != null) {
      for (String id : _ids) {
        _service.cancelAlarmForArrivalAndDepartureAtStop(id);
        _alarmService.cancelAlarm(id);
      }
    }

    if (isVersion(V2)) {
      return getOkResponse("");
    } else {
      return getUnknownVersionResponse();
    }
  }
}
