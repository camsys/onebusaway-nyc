package org.onebusaway.nyc.transit_data_manager.siri;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class WebResourceWrapper {

  public String post(String siri, String tdm) {
    String postResult = "";
    ClientConfig config = new DefaultClientConfig();
    config.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 5*1000);
    config.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 5*1000);
    Client client = Client.create(config);
    WebResource r = client.resource(tdm);
    postResult = r.accept(MediaType.APPLICATION_XML_TYPE).type(
        MediaType.APPLICATION_XML_TYPE).post(String.class, siri);
    return postResult;
  }

}
