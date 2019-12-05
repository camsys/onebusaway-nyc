package org.onebusaway.nyc.webapp.actions.api;

import org.apache.struts2.ServletActionContext;
import org.json.JSONObject;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

public class ActiveBundleAction extends OneBusAwayNYCActionSupport {

    private TransitDataService _tds;

    @Autowired
    public void setTransitDataService(TransitDataService tds) {
        _tds = tds;
    }

    @Override
    public String execute() {
        try {
            JSONObject json = new JSONObject();
            json.put("bundleid", _tds.getActiveBundleId());
            ServletActionContext.getResponse().setContentType("application/json");
            ServletActionContext.getResponse().getWriter().write(json.toString());
        } catch (Throwable t) {
            ServletActionContext.getResponse().setStatus(500);
        }
        return null;
    }
}
