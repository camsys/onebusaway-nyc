/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tcip_final_3_0_5_1.CCDestinationSignMessage;

/**
 * Represents data from our parser, which is in TCIP format.
 * 
 * @author sclark
 * 
 */
public class ImporterSignCodeData implements SignCodeData {

  private List<CCDestinationSignMessage> destinations;

  public ImporterSignCodeData(List<CCDestinationSignMessage> destinations) {
    this.destinations = destinations;
  }

  /*
   * This basically loops over the set of sign codes searching for each DSC
   * with the given messageID.msgID (non-Javadoc)
   * 
   * @see org.onebusaway.nyc.transit_data_manager.adapters.data.SignCodeData#
   * getDisplayForCode(java.lang.String)
   */
  public List<CCDestinationSignMessage> getDisplayForCode(Long code) {
    List<CCDestinationSignMessage> result = new ArrayList<CCDestinationSignMessage>();

    // iterate over the list of destinations so we can check the codes.
    Iterator<CCDestinationSignMessage> destIt = destinations.iterator();

    CCDestinationSignMessage destSign = null;

    while (destIt.hasNext()) {
      destSign = destIt.next();

      if (code == destSign.getMessageID().getMsgID()) { // if we match by sign
                                                        // code id, then this is
                                                        // our result.
        result.add(destSign);
      }
    }

    return result;
  }

  public List<CCDestinationSignMessage> getAllDisplays() {
   return destinations;
  }

}
