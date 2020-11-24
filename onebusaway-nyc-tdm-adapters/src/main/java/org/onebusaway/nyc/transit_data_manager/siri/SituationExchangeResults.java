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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

@XmlRootElement
public class SituationExchangeResults {
  public static final String ADDED = "added";
  @XmlElement
  String status = "OK";
  @XmlElement
  private
  List<DeliveryResult> delivery = new ArrayList<DeliveryResult>();

  void countPtSituationElementResult(DeliveryResult deliveryResult, ServiceAlertBean serviceAlertBean, String status) {
    countPtSituationElementResult(deliveryResult, serviceAlertBean.getId(), status);
  }

  public void countPtSituationElementResult(DeliveryResult deliveryResult,
      String serviceAlertId, String status) {
    PtSituationElementResult ptSituationElementResult = new PtSituationElementResult();
    ptSituationElementResult.id = serviceAlertId;
    ptSituationElementResult.result = status;
    deliveryResult.getPtSituationElement().add(ptSituationElementResult);
  }
  
  @Override
  public String toString() {
    List<String> s = new ArrayList<String>();
    s.add("status=" + status);
    for (DeliveryResult d: getDelivery()) {
      for (PtSituationElementResult p: d.getPtSituationElement()) {
        s.add("id=" + p.id + " result=" + p.result);
      }
    }
    return StringUtils.join(s,  "\n");
  }

  public List<DeliveryResult> getDelivery() {
    return delivery;
  }

}

class PtSituationElementResult {
  @XmlElement
  String id;
  @XmlElement
  String result;
}

class DeliveryResult {
  private
  List<PtSituationElementResult> ptSituationElement = new ArrayList<PtSituationElementResult>();

  public List<PtSituationElementResult> getPtSituationElement() {
    return ptSituationElement;
  }

  @XmlElement
  public void setPtSituationElement(List<PtSituationElementResult> ptSituationElement) {
    this.ptSituationElement = ptSituationElement;
  }
}
