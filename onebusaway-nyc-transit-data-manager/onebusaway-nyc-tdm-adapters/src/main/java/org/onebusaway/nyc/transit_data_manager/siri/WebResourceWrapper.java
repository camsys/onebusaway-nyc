package org.onebusaway.nyc.transit_data_manager.siri;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class WebResourceWrapper {

  public static final int USE_DEFAULT_TIMEOUTS = -1;

  public String post(String siri, String tdm) {
    return post(siri, tdm, 10*1000, 10*1000);
  }
  
  public String post(String siri, String tdm, int connectTimeout, int readTimeout) {
    String postResult = "";
    ClientConfig config = new DefaultClientConfig();
    if (connectTimeout != USE_DEFAULT_TIMEOUTS)
      config.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
    if (readTimeout != USE_DEFAULT_TIMEOUTS)
      config.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);
    Client client = Client.create(config);
    WebResource r = client.resource(tdm);
    postResult = r.accept(MediaType.APPLICATION_XML_TYPE).type(
        MediaType.APPLICATION_XML_TYPE).post(String.class, siri);
    return postResult;
  }

}
