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

import org.onebusaway.geocoder.enterprise.impl.EnterpriseBingGeocoderImpl;
import org.onebusaway.geocoder.enterprise.impl.EnterpriseFilteredGeocoderBase;
import org.onebusaway.geocoder.enterprise.impl.EnterpriseGoogleGeocoderImpl;
import org.onebusaway.geocoder.enterprise.services.EnterpriseGeocoderResult;
import org.onebusaway.geocoder.enterprise.services.EnterpriseGeocoderService;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.presentation.service.cache.NycGeocoderCacheServiceImpl;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.ConfigurationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * An implementation of OBA's geocoder class to plug into front-end apps that
 * require it.
 */
public class AdaptiveGeocoderImpl extends EnterpriseFilteredGeocoderBase {

  @Autowired
  NycGeocoderCacheServiceImpl _geocoderCacheService;

  @Autowired
  private ConfigurationService _configurationService;

  EnterpriseGeocoderService geocoder;

  private CoordinateBounds _resultBiasingBounds = null;

  public void setResultBiasingBounds(CoordinateBounds bounds) {
    _resultBiasingBounds = bounds;
  }

  public List<EnterpriseGeocoderResult> enterpriseGeocode(String location) {
    if (!_geocoderCacheService.containsKey(location)) {
      String geocoderInstance = _configurationService.getConfigurationValueAsString("display.geocoderInstance", "google");
  		if (geocoderInstance.equals("google") && (geocoder == null || !geocoder.getClass().equals(EnterpriseGoogleGeocoderImpl.class))) {
  			EnterpriseGoogleGeocoderImpl impl = new EnterpriseGoogleGeocoderImpl();
            impl.setResultBiasingBounds(_resultBiasingBounds);
            impl.setConfiguration((ConfigurationServiceImpl) _configurationService);
            geocoder = impl;
  		} else if (geocoderInstance.equals("bing") && (geocoder == null || !geocoder.getClass().equals(EnterpriseBingGeocoderImpl.class))) {
  			EnterpriseBingGeocoderImpl impl = new EnterpriseBingGeocoderImpl();
            impl.setResultBiasingBounds(_resultBiasingBounds);
            geocoder = impl;
      }
  		_geocoderCacheService.store(location, geocoder.enterpriseGeocode(location));
    }
    return _geocoderCacheService.retrieve(location);
  }


}