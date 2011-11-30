package org.onebusaway.nyc.transit_data_manager.siri;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class WebResourceWrapper {

  public String post(String siri, String tdm) {
    String postResult = "";
    // _log.debug("result=" + siri);
    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource r = client.resource(tdm);
    postResult = r.accept(MediaType.APPLICATION_XML_TYPE).type(
        MediaType.APPLICATION_XML_TYPE).post(String.class, siri);
    // _log.debug("postResult=" + postResult);
    return postResult;
  }

}
