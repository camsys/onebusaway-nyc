package org.onebusaway.nyc.transit_data_manager.adapters.input;

import tcip_final_3_0_5_1.CcAnnouncementInfo;

public interface CcAnnouncementInfoConverter {

    CcAnnouncementInfo.Destinations getDestinations();
}