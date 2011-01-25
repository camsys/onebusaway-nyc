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
package org.onebusaway.nyc.vehicle_tracking.webapp.servlets;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.thoughtworks.xstream.XStream;

public class VehicleLocationCollectionServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private XStream _xstream;

  private VehicleLocationService _vehicleLocationService;

  @Autowired
  public void setVehicleLocationService(
      VehicleLocationService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @Override
  public void init() {
    _xstream = new XStream();
    _xstream.processAnnotations(Siri.class);
    _xstream.processAnnotations(VehicleActivity.class);

    WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    context.getAutowireCapableBeanFactory().autowireBean(this);

  }
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    BufferedReader reader = req.getReader();
    StringBuffer out = new StringBuffer();
    char[] buf = new char[4096];
    while (true) {
      int n = reader.read(buf);
      if (n == -1) {
        break;
      }
      out.append(new String(buf, 0, n));
    }
    String body = out.toString();

    Siri siri = (Siri) _xstream.fromXML(body);
    _vehicleLocationService.handleVehicleLocation(siri, body);

    resp.setStatus(200);
    resp.setContentType("text/html;charset=UTF-8");
    ServletOutputStream writer = resp.getOutputStream();
    writer.print("ok");
    writer.close();
  }
}
