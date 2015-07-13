package org.onebusaway.nyc.sms.actions.api;

import org.apache.struts2.ServletActionContext;

import com.google.gson.JsonObject;
import com.opensymphony.xwork2.ActionSupport;

public class PingAction extends ActionSupport {
  

private static final long serialVersionUID = 1L;

private String _response = null;


@Override
  public String execute() throws Exception {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("success", "true");
    
    _response = jsonObject.toString();
    
    ServletActionContext.getResponse().getWriter().write(_response);
    
    return null;
  }
  
}
