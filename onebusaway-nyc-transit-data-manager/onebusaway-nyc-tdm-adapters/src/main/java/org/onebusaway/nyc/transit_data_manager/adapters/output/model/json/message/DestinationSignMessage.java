package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DestinationSign;

public class DestinationSignMessage {
  private DestinationSign sign;
  private String status;

  public void setSign(DestinationSign sign) {
    this.sign = sign;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
