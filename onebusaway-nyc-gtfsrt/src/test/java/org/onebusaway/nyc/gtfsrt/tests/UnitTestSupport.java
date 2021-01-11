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

package org.onebusaway.nyc.gtfsrt.tests;

import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnitTestSupport {

  public static <T> ListBean<T> listBean(T... objs) {
    List<T> list = Arrays.asList(objs);
    ListBean<T> bean = new ListBean<T>();
    bean.setList(list);
    return bean;
  }

  public static List<AgencyWithCoverageBean> agenciesWithCoverage(String... ids) {
    List<AgencyWithCoverageBean> ret = new ArrayList<AgencyWithCoverageBean>();
    for (String id : ids) {
      AgencyWithCoverageBean bean = new AgencyWithCoverageBean();
      bean.setAgency(new AgencyBean());
      bean.getAgency().setId(id);
      ret.add(bean);
    }
    return ret;
  }

}
