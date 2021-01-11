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

package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;

public class UTSVehiclePullInOutDataUtility {
  /**
   * This static method is designed to filter a list of pull outs and return
   * only those that don't have funny bus numbers or other such garbage.
   * @param inputPullOuts the unfiltered list of pull outs
   * @return a subset of inputPullOuts which does not contain buses with unusable bus numbers.
   */
  public static List<MtaUtsVehiclePullInPullOut> filterOutStrangeRows (List<MtaUtsVehiclePullInPullOut> inputPullOuts) {
    List<MtaUtsVehiclePullInPullOut> resultList = new ArrayList<MtaUtsVehiclePullInPullOut>();
    
    Iterator<MtaUtsVehiclePullInPullOut> inputIt = inputPullOuts.iterator();
    while (inputIt.hasNext()) {
      MtaUtsVehiclePullInPullOut record = inputIt.next();
      
      if(containsUsefulPullOutData(record)) {
        resultList.add(record);
      }
    }
    
    return resultList;
  }
  
  private static boolean containsUsefulPullOutData (MtaUtsVehiclePullInPullOut pullout) {
    boolean result = true;
    
    if (new Long(-1).equals(pullout.getBusNumber())) // Something went wrong with the parsing (this may very well cover the cases below)
      result = false;
    
    if ("open".equalsIgnoreCase(pullout.getBusNumberField())) 
      result = false;
    
    if ("no op".equalsIgnoreCase(pullout.getBusNumberField()))
      result = false;
    
    if (StringUtils.isBlank(pullout.getBusNumberField())) // no bus number means not useful.
      result = false;
    
    return result;
  }
}
