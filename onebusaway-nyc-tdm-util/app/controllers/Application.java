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
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.impl.provider.entity.XMLRootElementProvider;

import play.Logger;
import play.modules.spring.Spring;
import play.mvc.Before;
import play.mvc.Controller;
import util.SiriHelper;

public class Application extends Controller {

  static String defaultTarget = "http://localhost:8083/siri/situation-exchange";
  static String defaultTdsUrl = "http://app.staging.obanyc.com/onebusaway-nyc-transit-data-federation-webapp/remoting/transit-data-service";

  @Before
  static void setuser() {
    setSessionValues("target", defaultTarget);
    setSessionValues("tdsUrl", defaultTdsUrl);
  }

  private static void setSessionValues(String name, String defaultValue) {
    String value = session.get(name);
    if (value == null) {
      value = defaultValue;
    }
    renderArgs.put(name, value);
    session.put(name,  value);
  }

  public static void index(boolean refresh, String tdsUrl) {
    String error = "";
    if (tdsUrl == null)
      tdsUrl = session.get("tdsUrl");
    ListBean<ServiceAlertBean> serviceAlerts = new ListBean<ServiceAlertBean>();
    if (refresh) {
      HessianProxyFactory factory = new HessianProxyFactory();
      try {
        TransitDataService tds;
        Logger.info("creating factory");
        tds = (TransitDataService) factory.create(TransitDataService.class,
            tdsUrl);
        Logger.info("Sending tds request");
        serviceAlerts = tds.getAllServiceAlertsForAgencyId("MTA NYCT");
        Logger.info("Service alerts: " + serviceAlerts);
        Logger.info("Size: " + serviceAlerts.getList().size());
      } catch (Exception e) {
        error = e.getMessage();
      }
    }
    render(serviceAlerts, tdsUrl, error);
  }

  public static void deleteServiceAlert(String serviceAlertId, String tdsUrl) {
    String error = "";
    ListBean<ServiceAlertBean> serviceAlerts = new ListBean<ServiceAlertBean>();
    if (tdsUrl == null) {
      error = "TDS url not given?";
    } else {
      HessianProxyFactory factory = new HessianProxyFactory();
      try {
        TransitDataService tds;
        tds = (TransitDataService) factory.create(TransitDataService.class,
            tdsUrl);
        tds.removeServiceAlert(serviceAlertId);
        serviceAlerts = tds.getAllServiceAlertsForAgencyId("MTA NYCT");
      } catch (Exception e) {
        error = e.getMessage();
      }
    }
    renderTemplate("Application/index.html", serviceAlerts, tdsUrl, error);
  }

  public static void enterSiri() {
    render();
  }

  public static void createSiri(String title, String statusType, String id,
      String begins, String expires, String affected, String text, String progress, String tdm) {
    SiriHelper s = new SiriHelper();
    s.addPtSituationElementStructure(title, text, id, begins, expires,
        affected, null, progress);
    String result = s.asString();
    render(result, tdm);
  }

  public static void sendToOba(String siri, String tdm, boolean incremental) {
    String error = "";
    String postResult = "";
    try {
      System.err.println("result=" + siri);
      ClientConfig config = new DefaultClientConfig();
      Client client = Client.create(config);
      WebResource r = client.resource(tdm + "?incremental=" + incremental);
      postResult = r.accept(MediaType.APPLICATION_XML_TYPE).type(
          MediaType.APPLICATION_XML_TYPE).post(String.class, siri);
      System.err.println("postResult=" + postResult);
    } catch (Exception e) {
      error = e.getMessage();
    }
    render(postResult, error);
  }

  public static void setSettings(String stuff) {
    renderJSON("'test': 'three'");
  }

}