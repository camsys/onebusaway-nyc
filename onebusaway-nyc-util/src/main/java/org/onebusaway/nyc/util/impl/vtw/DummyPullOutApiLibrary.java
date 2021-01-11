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

package org.onebusaway.nyc.util.impl.vtw;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.impl.RestApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DummyPullOutApiLibrary {

  private static Logger _log = LoggerFactory.getLogger(DummyPullOutApiLibrary.class);

  private String _vehiclePipoHostname = null;

  private Integer _vehiclePipoPort = 443;

  private String _apiEndpointPath = "/yardtrek-cron/";
  
  private Boolean _ssl = Boolean.TRUE;
  
  private RestApiLibrary _restApiLibrary;

  /**
    * Constructor injection necessary due to the usage of RestApiLibrary.
    */
  public DummyPullOutApiLibrary(String hostname, Integer port, String path, Boolean ssl) {
  }

  public String getContentsOfUrlAsString(String baseObject, String... params)
      throws Exception {
    return null;
  }
  
  public URL buildUrl(String baseObject, String... params) throws Exception {
    return null;
  }
  
  public URL buildSSLUrl(String baseObject, String... params) throws Exception {
    return null;
  }
  
  public String log(String baseObject, String component, Integer priority, String message) {
	  return null;
  }

}
