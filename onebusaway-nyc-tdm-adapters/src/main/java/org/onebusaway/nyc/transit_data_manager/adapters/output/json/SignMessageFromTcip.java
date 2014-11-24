package org.onebusaway.nyc.transit_data_manager.adapters.output.json;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DestinationSign;

import tcip_final_4_0_0.CCDestinationSignMessage;

public class SignMessageFromTcip implements
    ModelCounterpartConverter<CCDestinationSignMessage, DestinationSign> {

  public DestinationSign convert(CCDestinationSignMessage input) {
    DestinationSign sign = new DestinationSign();

    sign.setRouteName(input.getRouteID().getRouteName());
    sign.setMessageId(input.getMessageID().getMsgID());
    String msg = input.getMessageText();
    if (msg != null) {
    	msg = msg.replace('\n', ' ');
    	msg = msg.replace('\r', ' ');
    	msg = msg.replace('\t', ' ');
    }
    sign.setMessageText(msg);
    sign.setAgency(input.getRouteID().getAgencydesignator());

    return sign;
  }

}
