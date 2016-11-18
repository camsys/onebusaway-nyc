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
package org.onebusaway.nyc.sms.actions;

import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.sms.services.GoogleAnalyticsSessionAware;
import org.onebusaway.presentation.impl.NextActionSupport;

import com.dmurph.tracking.VisitorData;

import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.InterceptorRefs;
import org.apache.struts2.interceptor.SessionAware;

import java.util.Map;

@InterceptorRefs({@InterceptorRef("onebusaway-nyc-sms-webapp-stack")})
public abstract class SessionedIndexAction extends NextActionSupport 
  implements SessionAware, GoogleAnalyticsSessionAware {
    
  private static final long serialVersionUID = 1L;
  
  private static final int SESSION_RESET_WINDOW_IN_SECONDS = 60 * 20; // 20m
  
  private Map<String, Object> _session;
    
  protected SearchResultCollection _searchResults = null;

  protected Integer _searchResultsCursor = null;
  
  protected String _lastQuery = null;

  protected String _query = null;
  
  protected VisitorData _visitorCookie;
  
  protected Boolean _needsGlobalAlert;
  
  protected String _lastCommandString;
  
  public void initializeSession(String sessionId) {
    _searchResults = new SearchResultCollection();
    _searchResultsCursor = 0; 
    _visitorCookie = VisitorData.newVisitor();
    _needsGlobalAlert = true;
  }
  
  public void setSession(Map<String, Object> session) {
    this._session = session;
    
    if(session != null) {
      _searchResults = (SearchResultCollection)session.get("searchResults");
      _searchResultsCursor = (Integer)session.get("searchResultsCursor");
      _lastQuery = (String)session.get("lastQuery");
      _visitorCookie = (VisitorData)session.get("visitorData");
      _needsGlobalAlert = (Boolean)session.get("needsGlobalAlert");
      _lastCommandString = (String) session.get("lastCommandString");

      // if another request comes in before SESSION_RESET_WINDOW_IN_SECONDS, 
      // count it as another request in the same session--otherwise a new session from
      // an existing visitor.
      if(_visitorCookie != null) {
        if(_visitorCookie.getTimestampCurrent() - _visitorCookie.getTimestampPrevious() > SESSION_RESET_WINDOW_IN_SECONDS) {
          _visitorCookie.resetSession();
        } else {
          _visitorCookie.newRequest();
        }
      }
    }
  }
  
  public void syncSession() {
    _session.put("searchResults", _searchResults);
    _session.put("searchResultsCursor", _searchResultsCursor);
    _session.put("lastQuery", _lastQuery);
    _session.put("visitorData", _visitorCookie);
    _session.put("needsGlobalAlert", _needsGlobalAlert);
    _session.put("lastCommandString", _lastCommandString);
  }
  
  // user input/query
  public void setArgs(String args) {
    if(args != null) {
      this._query = args.trim().toUpperCase();
    } else {
      this._query = null;
    }
  }

  public abstract String execute() throws Exception;
}
