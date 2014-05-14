package org.onebusaway.nyc.webapp.actions.api;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.struts2.ServletActionContext;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PingAction extends OneBusAwayNYCActionSupport {
  
  private static Logger _log = LoggerFactory.getLogger(PingAction.class);
  private TransitDataService _tds;
  
  @Autowired
  public void setTransitDataService(TransitDataService tds) {
    _tds = tds;
  } 
  
  @Override
  public String execute() throws Exception {
    String pingStatus = null;
    try {
      pingStatus = getPing();
      ServletActionContext.getResponse().setContentType("application/json");
      ServletActionContext.getResponse().getWriter().write(pingStatus);
    } catch (Throwable t) {
      ServletActionContext.getResponse().setStatus(500);
    }
    return null;
  }
  
  public String getPing() throws RuntimeException {
    try {
      List<AgencyWithCoverageBean> count = _tds.getAgenciesWithCoverage();
      if (count == null || count.isEmpty()) {
        _log.error("Ping action found agencies = " + count);
        throw new ServletException("No agencies supported in current bundle");
      }
      return "" + count.size();
    } catch (Throwable t) {
      _log.error("Ping action failed with ", t);
      throw new RuntimeException(t);
    }
  }
}
