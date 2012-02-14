package controllers;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.EEffect;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;
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
        session.put(name, value);
    }

    public static void index(boolean refresh, String tdsUrl) {
        String error = "";
        ListBean<ServiceAlertBean> serviceAlerts = new ListBean<ServiceAlertBean>();
        if (tdsUrl == null)
            tdsUrl = session.get("tdsUrl");
        if (refresh) {
            try {
                serviceAlerts = getAllServiceAlerts(tdsUrl);
            } catch (Exception e) {
                error = e.getMessage();
            }
        }
        render(serviceAlerts, tdsUrl, error);
    }

    private static ListBean<ServiceAlertBean> getAllServiceAlerts(String tdsUrl)
            throws MalformedURLException {
        ListBean<ServiceAlertBean> serviceAlerts;
        TransitDataService tds = getTds(tdsUrl);
        serviceAlerts = tds.getAllServiceAlertsForAgencyId("MTA NYCT");
        Logger.info("Service alerts: " + serviceAlerts);
        Logger.info("Size: " + serviceAlerts.getList().size());
        return serviceAlerts;
    }

    private static TransitDataService getTds(String tdsUrl)
            throws MalformedURLException {
        HessianProxyFactory factory = new HessianProxyFactory();
        TransitDataService tds;
        Logger.info("creating factory");
        tds = (TransitDataService) factory.create(TransitDataService.class,
                tdsUrl);
        Logger.info("Sending tds request");
        return tds;
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
                tds = (TransitDataService) factory.create(
                        TransitDataService.class, tdsUrl);
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
            String begins, String expires, String affected, String text,
            String progress, String tdm) {
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
            WebResource r = client
                    .resource(tdm + "?incremental=" + incremental);
            postResult = r.accept(MediaType.APPLICATION_XML_TYPE)
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, siri);
            System.err.println("postResult=" + postResult);
        } catch (Exception e) {
            error = e.getMessage();
        }
        render(postResult, error);
    }

    public static void enterSa() {
        render();
    }

    public static void sendToTds(String title, String statusType, String id,
            String begins, String expires, String affected, String text,
            String tdsUrl) throws MalformedURLException {
        String error = "";

        if (StringUtils.isEmpty(id)) {
            error = "ID may not be null";
            render("Application/enterSa.html", error);
            return;
        }

        ListBean<ServiceAlertBean> serviceAlerts = new ListBean<ServiceAlertBean>();
        if (tdsUrl == null)
            tdsUrl = session.get("tdsUrl");

        TransitDataService tds = getTds(tdsUrl);

        ServiceAlertBean serviceAlertBean = new ServiceAlertBean();
        List<SituationAffectsBean> allAffects = new ArrayList<SituationAffectsBean>();
        addAffects(affected, allAffects);
        serviceAlertBean.setAllAffects(allAffects);
        serviceAlertBean.setId(id);
        serviceAlertBean.setSummaries(createListNaturalLanguage(title));
        serviceAlertBean.setSeverity(ESeverity.NORMAL);
        List<TimeRangeBean> publicationWindows = new ArrayList<TimeRangeBean>();
        serviceAlertBean.setPublicationWindows(publicationWindows);
        serviceAlertBean.setDescriptions(createListNaturalLanguage(text));
        List<SituationConsequenceBean> consequences = new ArrayList<SituationConsequenceBean>();
        SituationConsequenceBean sitConsBean = new SituationConsequenceBean();
        sitConsBean.setEffect(EEffect.DETOUR);
        consequences.add(sitConsBean);
        serviceAlertBean.setConsequences(consequences);

        tds.createServiceAlert(id, serviceAlertBean);

        try {
            serviceAlerts = getAllServiceAlerts(tdsUrl);
        } catch (Exception e) {
            error = e.getMessage();
        }

        renderTemplate("Application/index.html", serviceAlerts, tdsUrl, error);
    }

    private static void addAffects(String affected,
            List<SituationAffectsBean> allAffects) {
        for (String route : affected.split(",")) {
            String[] directions = new String[] { "0", "1" };
            if (route.contains(":")) {
                String[] parts = route.split(":");
                directions = new String[] { parts[1] };
                route = parts[0];
            }
            for (String direction : directions) {
                SituationAffectsBean sab = new SituationAffectsBean();
//                sab.setAgencyId(route.split("_", 2)[0]);
                sab.setRouteId(route);
                sab.setDirectionId(direction);
                allAffects.add(sab);
            }
        }
    }

    private static List<NaturalLanguageStringBean> createListNaturalLanguage(
            String string) {
        NaturalLanguageStringBean n = new NaturalLanguageStringBean();
        n.setValue(string);
        n.setLang("EN");
        ArrayList<NaturalLanguageStringBean> list = new ArrayList<NaturalLanguageStringBean>();
        list.add(n);
        return list;
    }

    public static void setSettings(String stuff) {
        renderJSON("'test': 'three'");
    }

}