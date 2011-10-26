package org.onebusaway.api.impl;

import org.onebusaway.api.model.transit.ArrivalAndDepartureV2Bean;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RouteBean.Builder;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;
import org.onebusaway.transit_data.model.trips.TripBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HandlerTestHelper {

  static final String applicationKey = "myApplication";
  

  public EntryWithReferencesBean<ArrivalAndDepartureV2Bean> createTestObject() {
    Builder routeBuilder = RouteBean.builder();
    routeBuilder.setAgency(new AgencyBean());
    routeBuilder.setId("myRouteId");
  
    ArrivalAndDepartureBean adBean = new ArrivalAndDepartureBean();
    StopBean stopBean = new StopBean();
    stopBean.setId("myStopId");
    stopBean.setRoutes(new ArrayList<RouteBean>());
    stopBean.getRoutes().add(routeBuilder.create());
    adBean.setStop(stopBean);
    TripBean tripBean = new TripBean();
    tripBean.setRoute(routeBuilder.create());
    adBean.setTrip(tripBean);
  
    adBean.setSituations(new ArrayList<ServiceAlertBean>());
    adBean.getSituations().add(new ServiceAlertBean());
    ServiceAlertBean alertBean = adBean.getSituations().get(0);
  
    alertBean.setActiveWindows(createTimeRangeList(0, 100000));
  
    alertBean.setAllAffects(new ArrayList<SituationAffectsBean>());
    alertBean.getAllAffects().add(new SituationAffectsBean());
    SituationAffectsBean affects = alertBean.getAllAffects().get(0);
    affects.setAgencyId("affectsAgencyId");
    affects.setApplicationId(applicationKey);
    affects.setDirectionId("affectsDirectionId");
    affects.setRouteId("affectsRouteId");
    affects.setStopId("myStopId");
    affects.setTripId("affectsTripdId");
  
    alertBean.setConsequences(new ArrayList<SituationConsequenceBean>());
    alertBean.getConsequences().add(new SituationConsequenceBean());
    SituationConsequenceBean sc = alertBean.getConsequences().get(0);
    sc.setDetourPath("detourPath");
    sc.setDetourStopIds(Arrays.asList(new String[] {"detourStopId"}));
    sc.setEffect(EEffect.DETOUR);
  
    alertBean.setCreationTime(300000);
    alertBean.setDescriptions(createNlBeanList("description"));
    alertBean.setId("situationId");
    alertBean.setPublicationWindows(createTimeRangeList(100000, 200000));
    alertBean.setReason("reason");
    alertBean.setSeverity(ESeverity.SEVERE);
    alertBean.setSummaries(createNlBeanList("summary"));
    alertBean.setUrls(createNlBeanList("url"));
  
    BeanFactoryV2 beanFactoryV2 = new BeanFactoryV2(true);
    beanFactoryV2.setApplicationKey(applicationKey);
    beanFactoryV2.setLocale(Locale.US);
  
    EntryWithReferencesBean<ArrivalAndDepartureV2Bean> response = beanFactoryV2.getResponse(adBean);
    return response;
  }


  private  List<TimeRangeBean> createTimeRangeList(int start, int end) {
    List<TimeRangeBean> windows = new ArrayList<TimeRangeBean>();
    windows.add(new TimeRangeBean(start, end));
    return windows;
  }


  /**
   * Create a 1-element list with the given string, with language assigned to
   * Locale.US.
   * 
   * @param string
   * @return
   */
  private List<NaturalLanguageStringBean> createNlBeanList(String string) {
    List<NaturalLanguageStringBean> strings = new ArrayList<NaturalLanguageStringBean>();
    strings.add(new NaturalLanguageStringBean(string, Locale.US.getLanguage()));
    return strings;
  }

}
