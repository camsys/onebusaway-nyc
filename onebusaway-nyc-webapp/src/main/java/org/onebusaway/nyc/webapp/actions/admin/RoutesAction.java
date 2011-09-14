/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.presentation.service.NycConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

@Results({@Result(type = "redirectAction", name = "redirect", params = {
    "namespace", "/admin", "actionName", "routes"})})
public class RoutesAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private TransitDataService transitDataService;

  @Autowired
  private NycConfigurationService configService;

  // TODO the service notices are hard coded by route+direction for now
  // for the B63
  private String noticeB63CobbleHill;
  private String noticeB63BayRidge;

  private boolean detourB63CobbleHill;
  private boolean detourB63BayRidge;

  private static final String cobbleHillDirection = "0";
  private static final String bayRidgeDirection = "1";

  @Override
  public String execute() throws Exception {

    /**
     * Note: constructing the route id this way is kind of a hack, but it will suffice for the pilot
     */
    String agencyId = configService.getDefaultAgencyId();

    List<ServiceAlertBean> serviceAlerts = fetchServiceAlerts(agencyId);
    for (ServiceAlertBean situationBean : serviceAlerts) {
      List<NaturalLanguageStringBean> descriptions = situationBean.getDescriptions();	
      if(descriptions.size() <= 0)
    	  continue;

      String serviceAlert = descriptions.get(0).getValue();
      String reason = situationBean.getReason(); 
      boolean hasDetour = reason.equals("Detour");

      List<SituationAffectsBean> affects = situationBean.getAllAffects();
      for (SituationAffectsBean affectBean : affects) {
    	String lineId = affectBean.getRouteId();
        String direction = affectBean.getDirectionId();
        // we only support one service notice for each line+direction
        if (lineId.equals(agencyId + "_B63")) {
          if (direction.equals(cobbleHillDirection)) {
            setNoticeB63CobbleHill(serviceAlert);
            setDetourB63CobbleHill(hasDetour);
          } else if (direction.equals(bayRidgeDirection)) {
            setNoticeB63BayRidge(serviceAlert);
            setDetourB63BayRidge(hasDetour);
          }
        }
      }
    }

    return SUCCESS;
  }

  private List<ServiceAlertBean> fetchServiceAlerts(String agencyId) {
    SituationQueryBean query = new SituationQueryBean();
    query.setAgencyId(agencyId);
    query.setTime(System.currentTimeMillis());
    ListBean<ServiceAlertBean> serviceAlertsList = transitDataService.getServiceAlerts(query);
    List<ServiceAlertBean> serviceAlerts = serviceAlertsList.getList();
    return serviceAlerts;
  }

  private void handleSituationUpdateOrDelete(ServiceAlertBean situationBean,
      String notice, boolean hasDetour) {
    if ((notice == null || notice.trim().length() == 0) && !hasDetour) {
      String situationId = situationBean.getId();
      transitDataService.removeServiceAlert(situationId);
    } else {
      NaturalLanguageStringBean descriptionBean = new NaturalLanguageStringBean();
      descriptionBean.setValue(notice);
      
      ArrayList<NaturalLanguageStringBean> descriptions = new ArrayList<NaturalLanguageStringBean>();
      descriptions.add(descriptionBean);

      situationBean.setDescriptions(descriptions);      
      transitDataService.updateServiceAlert(situationBean);
    }
  }

  private void handleCreateSituationBean(String agencyId, String notice,
      boolean hasDetour, String direction) {
    // we don't create the situation bean if the notice is empty and there's no
    // detour
    if ((notice == null || notice.trim().length() == 0) && !hasDetour)
      return;

    ServiceAlertBean situationBean = new ServiceAlertBean();
    NaturalLanguageStringBean descriptionBean = new NaturalLanguageStringBean();
    descriptionBean.setValue(notice);

    // add the affected lines
    ArrayList<SituationAffectsBean> situationAffectsBeans = new ArrayList<SituationAffectsBean>();

    SituationAffectsBean affect = new SituationAffectsBean();
    affect.setAgencyId(agencyId);
    affect.setRouteId("B63");
    affect.setDirectionId(direction);
    situationAffectsBeans.add(affect);    

    situationBean.setAllAffects(situationAffectsBeans);

    transitDataService.createServiceAlert(agencyId, situationBean);
  }

  public String submit() {
    
    String agencyId = configService.getDefaultAgencyId();
    
    // keep track of whether we handled some cases, so we know if we need
    // to create new situation beans or not
    boolean bayRidgeHandled = false;
    boolean cobbleHillHandled = false;

    // the case where we have situation beans that already exist
    // and we either have to update or remove them
    List<ServiceAlertBean> serviceAlerts = fetchServiceAlerts(agencyId);
    for (ServiceAlertBean situationBean : serviceAlerts) {
      List<SituationAffectsBean> affects = situationBean.getAllAffects();
      for (SituationAffectsBean situationAffect : affects) {
        String lineId = situationAffect.getRouteId();
        String direction = situationAffect.getDirectionId();
        if (lineId.equals("B63")) {
          if (direction.equals(cobbleHillDirection)) {
            handleSituationUpdateOrDelete(situationBean, noticeB63CobbleHill,
                detourB63CobbleHill);
            cobbleHillHandled = true;
          } else if (direction.equals(bayRidgeDirection)) {
            handleSituationUpdateOrDelete(situationBean, noticeB63BayRidge,
                detourB63BayRidge);
            bayRidgeHandled = true;
          }
        }
      }
    }

    // handle the case where we might have to create new situation beans
    if (!bayRidgeHandled)
      handleCreateSituationBean(agencyId, noticeB63BayRidge,
          detourB63BayRidge, bayRidgeDirection);

    if (!cobbleHillHandled)
      handleCreateSituationBean(agencyId, noticeB63CobbleHill,
          detourB63CobbleHill, cobbleHillDirection);

    return "redirect";
  }

  public String getNoticeB63CobbleHill() {
    return noticeB63CobbleHill;
  }

  public void setNoticeB63CobbleHill(String noticeB63CobbleHill) {
    this.noticeB63CobbleHill = noticeB63CobbleHill;
  }

  public String getNoticeB63BayRidge() {
    return noticeB63BayRidge;
  }

  public void setNoticeB63BayRidge(String noticeB63BayRidge) {
    this.noticeB63BayRidge = noticeB63BayRidge;
  }

  public boolean getDetourB63CobbleHill() {
    return detourB63CobbleHill;
  }

  public void setDetourB63CobbleHill(boolean detourB63CobbleHill) {
    this.detourB63CobbleHill = detourB63CobbleHill;
  }

  public boolean getDetourB63BayRidge() {
    return detourB63BayRidge;
  }

  public void setDetourB63BayRidge(boolean detourB63BayRidge) {
    this.detourB63BayRidge = detourB63BayRidge;
  }

}
