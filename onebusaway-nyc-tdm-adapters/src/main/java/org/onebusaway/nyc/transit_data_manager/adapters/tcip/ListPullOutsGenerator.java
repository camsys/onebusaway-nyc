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

package org.onebusaway.nyc.transit_data_manager.adapters.tcip;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

import tcip_final_3_0_5_1.CPTSubscriptionHeader;
import tcip_final_3_0_5_1.SchPullOutList;

public class ListPullOutsGenerator {

  private DateTime headerTime;

  public ListPullOutsGenerator(DateTime headerTime) {
    super();
    this.headerTime = headerTime;
  }

  public SchPullOutList generateFromVehAssignList(
      List<VehiclePullInOutInfo> vehAssignList) {

    SchPullOutList resultVehAssigns = new SchPullOutList();

    resultVehAssigns.setSubscriptionInfo(generateSubscriptionInfo());

    DateTimeFormatter dtf = DateTimeFormat.forPattern(UtsMappingTool.UTS_DATE_FIELD_DATEFORMAT);

    resultVehAssigns.setBeginDate(dtf.print(headerTime));
    resultVehAssigns.setBeginTime("0");
    resultVehAssigns.setEndDate(dtf.print(headerTime));
    resultVehAssigns.setEndTime("0");

    resultVehAssigns.setPullOuts(generatePullOuts(vehAssignList));

    return resultVehAssigns;
  }

  private CPTSubscriptionHeader generateSubscriptionInfo() {
    CPTSubscriptionHeader resultHeader = new CPTSubscriptionHeader();

    resultHeader.setRequestedType("Query");
    resultHeader.setExpirationDate("20400101");
    resultHeader.setExpirationTime("19:00:00");
    resultHeader.setRequestIdentifier(new Long(0));
    resultHeader.setSubscriberIdentifier(new Long(0));
    resultHeader.setPublisherIdentifier(new Long(0));

    return resultHeader;
  }

  private SchPullOutList.PullOuts generatePullOuts(
      List<VehiclePullInOutInfo> pullOutList) {
    SchPullOutList.PullOuts pullouts = new SchPullOutList.PullOuts();

    // iterate over assignmentList and add each element using
    // pullouts.getPullOut()
    for(VehiclePullInOutInfo vehiclePullInOut : pullOutList) {
    	pullouts.getPullOut().add(vehiclePullInOut.getPullOutInfo());
    	pullouts.getPullOut().add(vehiclePullInOut.getPullInInfo());
    }
    return pullouts;
  }
}
