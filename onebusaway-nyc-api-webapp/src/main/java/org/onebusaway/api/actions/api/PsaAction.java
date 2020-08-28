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

package org.onebusaway.api.actions.api;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;
import org.onebusaway.util.service.psa.PsaService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@ParentPackage("struts-default")
public class PsaAction {

  private ObjectMapper _mapper = new ObjectMapper();
  private PsaService _service;

  @Autowired
  public void setPsaService(PsaService service) {
    _service = service;
  }

  public DefaultHttpHeaders index() throws Exception {
    List<PublicServiceAnnouncement> psas = _service.getAllPsas();
    String body = _mapper.writeValueAsString(psas);
    HttpServletResponse response = ServletActionContext.getResponse();
    response.setContentType("application/json");
    response.getWriter().write(body);
    return null;
  }


}
