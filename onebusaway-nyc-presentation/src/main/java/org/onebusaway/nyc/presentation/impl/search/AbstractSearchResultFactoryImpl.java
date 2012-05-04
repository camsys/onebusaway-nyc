package org.onebusaway.nyc.presentation.impl.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public abstract class AbstractSearchResultFactoryImpl implements SearchResultFactory {

  public AbstractSearchResultFactoryImpl() {
    super();
  }

  protected void populateServiceAlerts(Set<String> serviceAlertDescriptions, List<ServiceAlertBean> serviceAlertBeans) {
    for (ServiceAlertBean serviceAlertBean : serviceAlertBeans) {
      boolean descriptionsAdded = false;
      descriptionsAdded = setDescription(serviceAlertDescriptions,
          serviceAlertBean.getDescriptions())
          || setDescription(serviceAlertDescriptions,
              serviceAlertBean.getSummaries());
      if (!descriptionsAdded) {
        serviceAlertDescriptions.add("(no description)");
      }
    }
  }

  // TODO This a problem, assumes English
  protected void populateServiceAlerts(List<NaturalLanguageStringBean> serviceAlertDescriptions, List<ServiceAlertBean> serviceAlertBeans) {
    Set<String> d = new HashSet<String>();
    populateServiceAlerts(d , serviceAlertBeans);
    for (String s: d) {
      serviceAlertDescriptions.add(new NaturalLanguageStringBean(s, "EN"));
    }
  }

  private boolean setDescription(Set<String> serviceAlertDescriptions, List<NaturalLanguageStringBean> descriptions) {
    boolean descriptionsAdded = false;
    if (descriptions != null) {
      for (NaturalLanguageStringBean description : descriptions) {
        if (description.getValue() != null) {
          serviceAlertDescriptions.add(description.getValue().replace("\n",
              "<br/>"));
          descriptionsAdded = true;
        }
      }
    }
    return descriptionsAdded;
  }

}