package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

public class DestinationSign {
  private Long messageId;
  private String routeName;
  private String messageText;

  public void setMessageId(Long messageId) {
    this.messageId = messageId;
  }

  public void setRouteName(String routeName) {
    this.routeName = routeName;
  }

  public void setMessageText(String messageText) {
    this.messageText = messageText;
  }
}
