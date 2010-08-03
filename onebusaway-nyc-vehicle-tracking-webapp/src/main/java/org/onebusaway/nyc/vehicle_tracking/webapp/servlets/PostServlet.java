package org.onebusaway.nyc.vehicle_tracking.webapp.servlets;

import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import org.onebusaway.siri.model.VehicleLocation;
import org.onebusaway.transit_data_federation.services.realtime.TripPositionService;

import com.thoughtworks.xstream.XStream;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PostServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private XStream xstream;
  @Autowired
  TripPositionService positionService;
  
  @Override
  public void init() {
    xstream = new XStream();
    xstream.processAnnotations(Siri.class);
    xstream.processAnnotations(VehicleActivity.class);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    BufferedReader reader = req.getReader();
    Siri siri = (Siri) xstream.fromXML(reader);
    ServiceDelivery delivery = siri.ServiceDelivery;
    VehicleActivity vehicleActivity = delivery.VehicleMonitoringDelivery.deliveries.get(0);
    VehicleLocation location = vehicleActivity.MonitoredVehicleJourney.VehicleLocation;
    /* insert into */
    if (positionService == null) {
      System.out.println("wrong!");
    }
    
  }
}
