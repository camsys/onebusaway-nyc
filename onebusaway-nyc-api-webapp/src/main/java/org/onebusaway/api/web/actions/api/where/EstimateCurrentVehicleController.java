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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.collections.Max;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateQueryBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateQueryBean.Record;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/where/estimate-current-vehicle")
public class EstimateCurrentVehicleController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  public EstimateCurrentVehicleController() {
    super(V2);
  }


/**
 * Handles the API endpoint relating to current state of a vehicle.
 * This method requires data in the "Data" field, formatted as multiple entries.
 * Each entry must be a comma-separated value containing timestamp, latitude, longitude, and accuracy,
 * and entries are separated by the pipe ('|') symbol.
 *
 * <p>The method also requires a "VehicleId" field to identify the vehicle.
 *
 * @param data A string containing the vehicle data. Each entry in the format
 *             "timestamp,lat,lon,accuracy" and entries are separated by '|'.
 *             For example: "1609459200000,34.0522,-118.2437,10|1609459260000,34.0522,-118.2437,15".
 * @param query container for records, should include a vehicleId.
 * @return some well wrapped CurrentVehicleEstimateV2Bean, or an error message if
 *         the request is invalid.
**/
  @GetMapping
  public ResponseBean index(@RequestParam(name ="Data", required = false) String data,
                            CurrentVehicleEstimateQueryBean query) throws IOException, ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    FieldErrorSupport fieldErrors = new FieldErrorSupport()
            .hasFieldError(data,"Data");

    fillInQuery(query,data, fieldErrors);
    if (fieldErrors.hasErrors())
      return getValidationErrorsResponseBean(fieldErrors.getErrors());
//    if (hasErrors())
//      return getValidationErrorsResponseBean();

    BeanFactoryV2 factory = getBeanFactoryV2();

    ListBean<CurrentVehicleEstimateBean> estimates = _service.getCurrentVehicleEstimates(query);
    return getOkResponseBean(factory.getCurrentVehicleEstimates(estimates));
  }

  private void fillInQuery(CurrentVehicleEstimateQueryBean _query,String _data, FieldErrorSupport fieldErrors) {

    List<CurrentVehicleEstimateQueryBean.Record> records = new ArrayList<CurrentVehicleEstimateQueryBean.Record>();
    
    Max<Record> max = new Max<Record>(); 

    for (String record : _data.split("\\|")) {

      String[] tokens = record.split(",");

      if (tokens.length != 4) {
        fieldErrors.addError("data", FieldErrorSupport.INVALID_FIELD_VALUE);
        return;
      }

      try {
        long t = Long.parseLong(tokens[0]);
        double lat = Double.parseDouble(tokens[1]);
        double lon = Double.parseDouble(tokens[2]);
        double accuracy = Double.parseDouble(tokens[3]);

        Record r = new Record();
        r.setTimestamp(t);
        r.setLocation(new CoordinatePoint(lat, lon));
        r.setAccuracy(accuracy);
        records.add(r);
        
        max.add(t, r);

      } catch (NumberFormatException ex) {
        fieldErrors.addError("data", FieldErrorSupport.INVALID_FIELD_VALUE);
        return;
      }
    }

    _query.setRecords(records);

    if (records.isEmpty()) {
      fieldErrors.addError("data", FieldErrorSupport.INVALID_FIELD_VALUE);
      return;
    }
    
    _query.setMostRecentLocation(max.getMaxElement().getLocation());
  }
}
