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
