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
package org.onebusaway.nyc.geocoder.impl;

import org.onebusaway.geocoder.enterprise.services.EnterpriseGeocoderResult;
import org.onebusaway.geocoder.enterprise.services.EnterpriseGeocoderService;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.service.cache.NycGeocoderCacheServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of OBA's geocoder class to plug into front-end apps that
 * require it.
 */

public class AdaptiveGeocoderImpl extends FilteredGeocoderBase implements EnterpriseGeocoderService {

  @Autowired
  NycGeocoderCacheServiceImpl _geocoderCacheService;

  NycGeocoderService geocoder;

	public List<NycGeocoderResult> nycGeocode(String location) {
    if (!_geocoderCacheService.containsKey(location)) {
      String geocoderInstance = _configurationService.getConfigurationValueAsString("display.geocoderInstance", "google");
  		if (geocoderInstance.equals("google") && (geocoder == null || !geocoder.getClass().equals(GoogleGeocoderImpl.class))) {
  			geocoder = new GoogleGeocoderImpl(this);
  		} else if (geocoderInstance.equals("bing") && (geocoder == null || !geocoder.getClass().equals(BingGeocoderImpl.class))) {
  			geocoder = new BingGeocoderImpl(this);
      }
  		_geocoderCacheService.store(location, geocoder.nycGeocode(location));
    }
    return _geocoderCacheService.retrieve(location);
  }

    @Override
    public List<EnterpriseGeocoderResult> enterpriseGeocode(String location) {
        List<NycGeocoderResult> nyc = nycGeocode(location);
        List<EnterpriseGeocoderResult> results = new ArrayList<EnterpriseGeocoderResult>();
        for (NycGeocoderResult r : nyc) {
            results.add((EnterpriseGeocoderResult) r);
        }
        return results;
    }
}