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
package org.onebusaway.api.web.actions.api.where;


import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.services.AlarmDetails;
import org.onebusaway.api.services.AlarmService;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.ArrivalAndDepartureForStopQueryBean;
import org.onebusaway.transit_data.model.RegisterAlarmQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/where/register-alarm-for-arrival-and-departure-at-stop/{stopId}")
public class RegisterAlarmForArrivalAndDepartureAtStopController extends
    ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  @Autowired
  private AlarmService _alarmService;

//  private ArrivalAndDepartureForStopQueryBean _query = new ArrivalAndDepartureForStopQueryBean();
//
//  private RegisterAlarmQueryBean _alarm = new RegisterAlarmQueryBean();
//
//  private String _data;

  public RegisterAlarmForArrivalAndDepartureAtStopController() {
    super(V2);
  }

//  @RequiredFieldValidator(message = FieldErrorSupport.MISSING_REQUIRED_FIELD)
//  public void setId(String id) {
//    _query.setStopId(id);
//  }
//
//  public String getId() {
//    return _query.getStopId();
//  }
//
//  public ArrivalAndDepartureForStopQueryBean getQuery() {
//    return _query;
//  }
//
//  public void setQuery(ArrivalAndDepartureForStopQueryBean query) {
//    _query = query;
//  }
//
//  public RegisterAlarmQueryBean getAlarm() {
//    return _alarm;
//  }
//
//  public void setAlarm(RegisterAlarmQueryBean alarm) {
//    _alarm = alarm;
//  }
//
//  public void setData(String data) {
//    _data = data;
//  }

  @GetMapping
  public ResponseEntity<ResponseBean> show(@PathVariable("stopId") String id,
                                           ArrivalAndDepartureForStopQueryBean query,
                                           RegisterAlarmQueryBean alarm,
                                           @RequestParam(name ="Data", required = false) String data) throws ServiceException {

    FieldErrorSupport fieldErrors = new FieldErrorSupport()
            .hasFieldError(query.getTripId(),"tripId");
    if (fieldErrors.hasErrors())
      return getValidationErrorsResponseBean(fieldErrors.getErrors());

    query.setStopId(id);
    if (query.getTime() == 0)
      query.setTime(System.currentTimeMillis());
    
    AlarmDetails details = _alarmService.alterAlarmQuery(alarm, data);

    String alarmId = _service.registerAlarmForArrivalAndDepartureAtStop(query,
        alarm);

    if (alarmId == null)
      return getResourceNotFoundResponseBean();
    
    if( details != null) { 
      _alarmService.registerAlarm(alarmId, details);
    }

    if (isVersion(V2)) {
      return getOkResponseBean(alarmId);
    } else {
      return getUnknownVersionResponseBean();
    }
  }
}
