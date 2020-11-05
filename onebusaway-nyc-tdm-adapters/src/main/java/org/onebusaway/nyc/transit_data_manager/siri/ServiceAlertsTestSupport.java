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

package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;

public class ServiceAlertsTestSupport {

  public static final String TEST_ROUTE_ID = "test route id";

  public static ServiceAlertBean createServiceAlertBean(String id) {
    ServiceAlertBean serviceAlertBean = new ServiceAlertBean();
    serviceAlertBean.setId(id);
    List<NaturalLanguageStringBean> summaries = new ArrayList<NaturalLanguageStringBean>();
    summaries.add(createNaturalLanguageStringBean("summary"));
    serviceAlertBean.setSummaries(summaries);
    List<NaturalLanguageStringBean> descriptions = new ArrayList<NaturalLanguageStringBean>();
    descriptions.add(createNaturalLanguageStringBean("description"));
    serviceAlertBean.setDescriptions(descriptions);
  
    List<SituationAffectsBean> allAffects = new ArrayList<SituationAffectsBean>();
    SituationAffectsBean saBean = new SituationAffectsBean();
    saBean.setRouteId(TEST_ROUTE_ID);
    allAffects.add(saBean );
    serviceAlertBean.setAllAffects(allAffects);
    return serviceAlertBean;
  }

  private static NaturalLanguageStringBean createNaturalLanguageStringBean(
      String string) {
    NaturalLanguageStringBean n = new NaturalLanguageStringBean();
    n.setValue(string);
    n.setLang("EN");
    return n;
  }

}
