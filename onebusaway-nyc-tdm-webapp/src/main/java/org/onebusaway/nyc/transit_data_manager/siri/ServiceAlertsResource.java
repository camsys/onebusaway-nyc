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

import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;


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
public class ServiceAlertsResource {

	private static Logger _log = LoggerFactory
			.getLogger(ServiceAlertsResource.class);

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  @GET
  @Produces("application/xml")
  public Response list() throws JAXBException {
	SituationQueryBean situationQueryBean = new SituationQueryBean();
    ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService.getServiceAlerts(situationQueryBean);
		return Response.ok(serviceAlerts).build();
  }

}
