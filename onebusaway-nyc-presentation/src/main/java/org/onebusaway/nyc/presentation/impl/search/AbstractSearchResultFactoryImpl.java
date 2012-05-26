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
    populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans, true);
  }

  protected void populateServiceAlerts(Set<String> serviceAlertDescriptions,
      List<ServiceAlertBean> serviceAlertBeans, boolean htmlizeNewlines) {
    if (serviceAlertBeans == null)
      return;
    for (ServiceAlertBean serviceAlertBean : serviceAlertBeans) {
      boolean descriptionsAdded = false;
      descriptionsAdded = setDescription(serviceAlertDescriptions,
          serviceAlertBean.getDescriptions(), htmlizeNewlines)
          || setDescription(serviceAlertDescriptions,
              serviceAlertBean.getSummaries(), htmlizeNewlines);
      if (!descriptionsAdded) {
        serviceAlertDescriptions.add("(no description)");
      }
    }
  }

  protected void populateServiceAlerts(
      List<NaturalLanguageStringBean> serviceAlertDescriptions,
      List<ServiceAlertBean> serviceAlertBeans, boolean htmlizeNewlines) {
    Set<String> d = new HashSet<String>();
    populateServiceAlerts(d , serviceAlertBeans, htmlizeNewlines);
    for (String s: d) {
      serviceAlertDescriptions.add(new NaturalLanguageStringBean(s, "EN"));
    }
  }


  // TODO This a problem, assumes English
  protected void populateServiceAlerts(List<NaturalLanguageStringBean> serviceAlertDescriptions, List<ServiceAlertBean> serviceAlertBeans) {
    populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans, true);
  }

  private boolean setDescription(Set<String> serviceAlertDescriptions, List<NaturalLanguageStringBean> descriptions, boolean htmlizeNewlines) {
    boolean descriptionsAdded = false;
    if (descriptions != null) {
      for (NaturalLanguageStringBean description : descriptions) {
        if (description.getValue() != null) {
          serviceAlertDescriptions.add((htmlizeNewlines ? description.getValue().replace("\n",
              "<br/>") : description.getValue()));
          descriptionsAdded = true;
        }
      }
    }
    return descriptionsAdded;
  }

}