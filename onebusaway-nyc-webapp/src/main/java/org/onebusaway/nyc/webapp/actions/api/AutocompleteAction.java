/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.api;

import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

@ParentPackage("json-default")
//@Result(type="json", params={"root", "suggestions"})
@Result(type="json", params={"callbackParameter", "callback", "root", "suggestions"})
public class AutocompleteAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  @Autowired
  private NycGeocoderService _geocoderService;

  private List<String> suggestions = null;

  private String _term = null;

  public void setTerm(String term) {
    if(term != null) {
      _term = term.trim();
    }
  }

  @Override
  public String execute() {
    if (_term == null || _term.isEmpty() )
      return SUCCESS;

    suggestions = _nycTransitDataService.getSearchSuggestions(null, _term.toLowerCase());

    if (suggestions.size() == 0 && _term.length() >= 3) {
    	List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(_term);
      if (geocoderResults.size() > 0) {
        for (int i = 0; i < 10; i++) {
          suggestions.add(geocoderResults.get(i).getFormattedAddress());
          if (i + 1 == geocoderResults.size())
            break;
        }
      }
    }
    return SUCCESS;
  }

  /** 
   * VIEW METHODS
   */
  public List<String> getSuggestions() {
    return suggestions;
  }

}