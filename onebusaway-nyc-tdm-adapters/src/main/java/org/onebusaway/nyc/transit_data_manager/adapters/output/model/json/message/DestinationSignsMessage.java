package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DestinationSign;

public class DestinationSignsMessage {

  private List<DestinationSign> signs;
  private String status;

  public void setSigns(List<DestinationSign> signs) {
    this.signs = signs;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
