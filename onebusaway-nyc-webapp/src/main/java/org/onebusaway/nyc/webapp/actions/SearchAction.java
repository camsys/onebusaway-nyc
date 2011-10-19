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
package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.presentation.model.EnumDisplayMedia;
import org.onebusaway.nyc.presentation.service.NycSearchService;
import org.onebusaway.nyc.presentation.service.search.SearchResult;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles requests for a generic search. Can return route/stop specific results.
 */
@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class SearchAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private List<SearchResult> searchResults = new ArrayList<SearchResult>();
  
  @Autowired
  private NycSearchService searchService;
  
  private String q;
  
  public void setQ(String query) {
    this.q = query.trim();
  }
  
  @Override
  public String execute() {
    if (q == null || q.isEmpty())
      return SUCCESS;

    searchResults = searchService.search(q, EnumDisplayMedia.DESKTOP_WEB);

    return SUCCESS;
  }

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }
}
