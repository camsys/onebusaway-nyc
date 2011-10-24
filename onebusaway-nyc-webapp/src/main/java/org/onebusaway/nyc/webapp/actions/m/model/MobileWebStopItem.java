/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.nyc.presentation.model.search.StopItem;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.util.List;

public class MobileWebStopItem extends StopItem {

  private List<NaturalLanguageStringBean> serviceAlerts;
  
  private String distanceAwayString;
  
  public MobileWebStopItem(StopBean stopBean) {
    super(stopBean);
  }

  public List<NaturalLanguageStringBean> getServiceAlerts() {
    return serviceAlerts;
  }

  public void setServiceAlerts(List<NaturalLanguageStringBean> serviceAlerts) {
    this.serviceAlerts = serviceAlerts;
  }

  public String getDistanceAwayString() {
    return distanceAwayString;
  }

  public void setDistanceAwayString(String distanceAwayString) {
    this.distanceAwayString = distanceAwayString;
  }
}
