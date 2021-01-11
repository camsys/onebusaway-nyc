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
