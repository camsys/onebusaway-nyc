package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.List;

import tcip_final_3_0_5_1.CCDestinationSignMessage;

public interface SignCodeData {
  /**
   * Get the display for the given dsc.  Multiple messages can be returned due
   * to multi-agency support; check the agencyDesignator to discriminate.
   * 
   * @param code the DSC (destination sign code) code to get the readable
   *          display for.
   * @return the human readable display corresponding to the input sign code.
   */
  List<CCDestinationSignMessage> getDisplayForCode(Long code);
  
  List<CCDestinationSignMessage> getAllDisplays();
}
