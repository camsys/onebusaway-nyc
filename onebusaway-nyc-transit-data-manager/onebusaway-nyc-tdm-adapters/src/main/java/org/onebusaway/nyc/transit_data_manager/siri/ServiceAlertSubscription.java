package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.Date;
import java.util.Map;

import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;

public class ServiceAlertSubscription {

  private String address;
  private String subscriptionIdentifier;
  private String subscriptionRef;
  private Date createdAt;
  private WebResourceWrapper webResourceWrapper;
  
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getSubscriptionIdentifier() {
    return subscriptionIdentifier;
  }

  public void setSubscriptionIdentifier(String subscriptionIdentifier) {
    this.subscriptionIdentifier = subscriptionIdentifier;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getSubscriptionRef() {
    return subscriptionRef;
  }

  public void setSubscriptionRef(String subscriptionRef) {
    this.subscriptionRef = subscriptionRef;    
  }

  public void send(SituationExchangeResults results, Map<String, ServiceAlertBean> serviceAlerts) throws Exception {
    ServiceAlertsHelper h = new ServiceAlertsHelper();
    Siri s = new Siri();
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    
    h.addSituationExchangeToSiri(serviceDelivery, serviceAlerts);
    // TODO removed

    s.setServiceDelivery(serviceDelivery);
    String xml = SiriXmlSerializer.getXml(s);
    
    getWebResourceWrapper().post(xml, getAddress());
  }

  public WebResourceWrapper getWebResourceWrapper() {
    if (webResourceWrapper == null)
      webResourceWrapper = new WebResourceWrapper();
    return webResourceWrapper;
  }

  public void setWebResourceWrapper(WebResourceWrapper webResourceWrapper) {
    this.webResourceWrapper = webResourceWrapper;
  }
  
}
