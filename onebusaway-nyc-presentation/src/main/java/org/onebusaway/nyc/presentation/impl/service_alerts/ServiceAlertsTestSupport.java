package org.onebusaway.nyc.presentation.impl.service_alerts;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;

/**
 * Methods to assist testing Service Alerts.
 * 
 * Note that although this is really a test support class, it needs to be in
 * src/main/java (not src/test/java) so it can be used by other modules.
 *
 */
public class ServiceAlertsTestSupport {

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
    serviceAlertBean.setAllAffects(allAffects);

    allAffects.add(addAffects("MTA NYCT_B63", "0"));
    allAffects.add(addAffects("MTA NYCT_B63", "1"));
    allAffects.add(addAffects("MTA NYCT_S55", "0"));
    allAffects.add(addAffects("MTA NYCT_S55", "1"));
    
    return serviceAlertBean;
  }

  public static SituationAffectsBean addAffects(String route, String direction) {
    SituationAffectsBean sab = new SituationAffectsBean();
    sab.setRouteId(route);
    sab.setDirectionId(direction);
    return sab;
  }

  public static NaturalLanguageStringBean createNaturalLanguageStringBean(
      String string) {
    NaturalLanguageStringBean n = new NaturalLanguageStringBean();
    n.setValue(string);
    n.setLang("EN");
    return n;
  }

  public static List<NaturalLanguageStringBean> createNaturalLanguageStringBeanList(
      String string) {
    List<NaturalLanguageStringBean> l = new ArrayList<NaturalLanguageStringBean>();
    l.add(createNaturalLanguageStringBean(string));
    return l ;
  }

}
