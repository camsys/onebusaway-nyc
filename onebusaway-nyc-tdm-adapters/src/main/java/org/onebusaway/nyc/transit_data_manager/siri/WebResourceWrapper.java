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

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebResourceWrapper {

  public static final int USE_DEFAULT_TIMEOUTS = -1;
  @SuppressWarnings("unused")
  transient private static final Logger _log = LoggerFactory.getLogger(WebResourceWrapper.class);

  public String post(String siri, String tdm) {
    Client client = ClientBuilder.newClient( new ClientConfig().register( WebResourceWrapper.class ) );
    WebTarget webTarget = client.target(tdm);

    Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_XML);
    Response response = invocationBuilder.post(Entity.entity(siri, MediaType.APPLICATION_XML));
    return response.readEntity(String.class);
  }

}
