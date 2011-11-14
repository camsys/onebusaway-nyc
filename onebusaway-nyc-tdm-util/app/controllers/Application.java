package controllers;

import java.net.MalformedURLException;

import javax.ws.rs.core.MediaType;

import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;

import com.caucho.hessian.client.HessianProxyFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.impl.provider.entity.XMLRootElementProvider;
import com.sun.org.apache.xerces.internal.parsers.XML11Configuration;

import play.modules.spring.Spring;
import play.mvc.Controller;
import util.SiriHelper;

public class Application extends Controller {

  public static void index(boolean refresh) throws MalformedURLException {
    // TransitDataService tds = Spring.getBeanOfType(TransitDataService.class);
    ListBean<ServiceAlertBean> serviceAlerts = new ListBean<ServiceAlertBean>();
    if (refresh) {
      HessianProxyFactory factory = new HessianProxyFactory();
      TransitDataService tds = (TransitDataService) factory.create(
          TransitDataService.class,
          "http://app.staging.obanyc.com/onebusaway-nyc-transit-data-federation-webapp/remoting/transit-data-service");
      serviceAlerts = tds.getAllServiceAlertsForAgencyId("MTA NYCT");
    }
    render(serviceAlerts);
  }

  public static void enterSiri() {
    render();
  }

  public static void createSiri(String title, String statusType, String id,
      String begins, String expires, String affected, String text, String tdm) {
    SiriHelper s = new SiriHelper();
    s.addPtSituationElementStructure(title, text, id, begins, expires,
        affected, null);
    String result = s.asString();
    render(result, tdm);
  }

  public static void sendToOba(String siri, String tdm) {
    System.err.println("result=" + siri);
    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource r = client.resource(tdm);
    String postResult = r.accept(MediaType.APPLICATION_XML_TYPE).type(
        MediaType.APPLICATION_XML_TYPE).post(String.class, siri);
    System.err.println("postResult=" + postResult);
    render(postResult);
  }

}