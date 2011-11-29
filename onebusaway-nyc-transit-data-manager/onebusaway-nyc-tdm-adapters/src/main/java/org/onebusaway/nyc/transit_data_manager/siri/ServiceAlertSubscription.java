package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.Date;

public class ServiceAlertSubscription {

  private String address;
  private String subscriptionIdentifier;
  private String subscriptionRef;
  private Date createdAt;
  
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
  
}
