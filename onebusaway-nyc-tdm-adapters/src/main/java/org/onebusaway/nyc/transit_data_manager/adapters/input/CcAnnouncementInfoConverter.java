package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.List;

import tcip_final_4_0_0.CCDestinationSignMessage;
import tcip_final_4_0_0.CcAnnouncementInfo;

public interface CcAnnouncementInfoConverter {

  CcAnnouncementInfo.Destinations getDestinations();

  List<CCDestinationSignMessage> getDestinationsAsList();
}