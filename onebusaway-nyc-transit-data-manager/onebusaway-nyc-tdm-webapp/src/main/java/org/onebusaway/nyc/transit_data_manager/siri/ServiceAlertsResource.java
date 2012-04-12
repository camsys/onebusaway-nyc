package org.onebusaway.nyc.transit_data_manager.siri;

import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;

import com.sun.jersey.api.spring.Autowire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

@Path("/service-alerts")
@Component
@Scope("request")
@Autowire
public class ServiceAlertsResource {

	private static Logger _log = LoggerFactory
			.getLogger(ServiceAlertsResource.class);

  @Autowired
  private TransitDataService _transitDataService;

	@GET
  @Produces("application/xml")
  public Response list() throws JAXBException {
	  SituationQueryBean situationQueryBean = new SituationQueryBean();
	  situationQueryBean.setAgencyId("MTA NYCT");
    ListBean<ServiceAlertBean> serviceAlerts = _transitDataService.getServiceAlerts(situationQueryBean);
		return Response.ok(serviceAlerts).build();
  }

}
