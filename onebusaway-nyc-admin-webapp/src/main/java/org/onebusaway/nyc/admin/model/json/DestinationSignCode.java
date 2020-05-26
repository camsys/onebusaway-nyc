package org.onebusaway.nyc.admin.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DestinationSignCode {
  private String messageId;
  private String routeName;
  private String messageText;
  private String agency;

  public DestinationSignCode() {
    
  }
  
  @JsonProperty("message-id")
  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  @JsonProperty("route-name")
  public String getRouteName() {
    return routeName;
  }

  public void setRouteName(String routeName) {
    this.routeName = routeName;
  }

  @JsonProperty("message-text")
  public String getMessageText() {
    return messageText;
  }

  public void setMessageText(String messageText) {
    this.messageText = messageText;
  }

  @JsonProperty("agency")
  public String getAgency() {
    return agency;
  }

  public void setAgency(String agency) {
    this.agency = agency;
  }
  
}
