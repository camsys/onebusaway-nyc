package org.onebusaway.nyc.gtfsrt.tests;

import org.onebusaway.transit_data.model.ListBean;

import java.util.Arrays;
import java.util.List;

public class UnitTestSupport {

  public static <T> ListBean<T> listBean(T... objs) {
    List<T> list = Arrays.asList(objs);
    ListBean<T> bean = new ListBean<T>();
    bean.setList(list);
    return bean;
  }

}
