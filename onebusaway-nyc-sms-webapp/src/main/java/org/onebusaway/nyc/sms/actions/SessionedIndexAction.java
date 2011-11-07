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

import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.presentation.impl.NextActionSupport;

import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.InterceptorRefs;
import org.apache.struts2.interceptor.SessionAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@InterceptorRefs({@InterceptorRef("onebusaway-nyc-sms-webapp-stack")})
public abstract class SessionedIndexAction extends NextActionSupport implements SessionAware {
  
  private static final long serialVersionUID = 1L;
  
  private Map<String, Object> _session;
  
  protected List<SearchResult> _searchResults = new ArrayList<SearchResult>();

  protected Integer _searchResultsCursor = 0;
  
  protected String _lastQuery = null;

  protected String _query = null;
  
  public void setSession(Map<String, Object> session) {
    this._session = session;
    
    if(session != null) {
      @SuppressWarnings("unchecked")
      List<SearchResult> results = (List<SearchResult>)session.get("searchResults");
      if(results != null)
        _searchResults = results; 
      
      _searchResultsCursor = (Integer)session.get("searchResultsCursor");
      if(_searchResultsCursor == null)
        _searchResultsCursor = 0;
      
      _lastQuery = (String)session.get("lastQuery");
    }
  }
  
  public void syncSession() {
    _session.put("searchResults", _searchResults);
    _session.put("searchResultsCursor", _searchResultsCursor);
    _session.put("lastQuery", _query);
  }
  
  public void setArgs(String args) {
    if(args != null)
      this._query = args.trim();
    else
      this._query = null;
  }

  public abstract String execute() throws Exception;

}
