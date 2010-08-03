package org.onebusaway.nyc.vehicle_tracking.webapp.servlets;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PostServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    BufferedReader reader = req.getReader();
    String line = null;

    while ((line = reader.readLine()) != null) {
      System.err.println(line);
    }
  }
}
