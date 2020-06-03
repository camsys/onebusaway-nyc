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

package org.onebusaway.nyc.transit_data_manager.barcode.model;

public class MtaBarcode {

  public MtaBarcode (String contents) {
    this.contents = contents;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MtaBarcode other = (MtaBarcode) obj;

    if ((other.getContents() != null && getContents() != null) && (other.getContents().equals(getContents()))) {
      return true;
    } else {
      return false;
    }
    
  }
  
  @Override
  public int hashCode () {
    return contents.hashCode();
  }
  
  private String contents = null;
  private String stopIdStr = null;
  
  
  public String getStopIdStr() {
    return stopIdStr;
  }

  public void setStopIdStr(String stopIdStr) {
    this.stopIdStr = stopIdStr;
  }

  public String getContents() {
    return contents;
  }
  
  
}
