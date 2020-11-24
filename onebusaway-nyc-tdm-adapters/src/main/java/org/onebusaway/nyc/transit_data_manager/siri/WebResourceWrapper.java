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

package org.onebusaway.nyc.transit_data_manager.siri;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class WebResourceWrapper {

  public static final int USE_DEFAULT_TIMEOUTS = -1;
  @SuppressWarnings("unused")
  transient private static final Logger _log = LoggerFactory.getLogger(WebResourceWrapper.class);

  public String post(String siri, String tdm) {
    String postResult = "";
    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource r = client.resource(tdm);
    postResult = r.accept(MediaType.APPLICATION_XML_TYPE).type(
        MediaType.APPLICATION_XML_TYPE).post(String.class, siri);
    return postResult;
  }

}
