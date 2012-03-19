package org.onebusaway.nyc.sms.services;

public interface GoogleAnalyticsSessionAware {

  public void initializeSession(String sessionId);
  
}
