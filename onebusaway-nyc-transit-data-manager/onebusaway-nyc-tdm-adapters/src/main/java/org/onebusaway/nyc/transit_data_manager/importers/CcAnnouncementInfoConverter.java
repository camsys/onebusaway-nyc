package org.onebusaway.nyc.transit_data_manager.importers;

import tcip_final_3_0_5_1.*;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CcAnnouncementInfoConverter {

    CcAnnouncementInfo.Destinations getDestinations();
}