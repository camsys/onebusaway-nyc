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
