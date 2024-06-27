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
package org.onebusaway.nyc.webapp.actions.api;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@ParentPackage("json-default")
//@Result(type="json")
@Result(type="json", params={"callbackParameter", "callback"})
public class SearchV2Action extends OneBusAwayNYCActionSupport {

    private static final long serialVersionUID = 1L;

    @Autowired
    @Qualifier("NycSearchService")
    private SearchService _searchService;

    @Autowired
    private NycTransitDataService _nycTransitDataService;

    @Autowired
    @Qualifier("NycRealtimeService")
    private RealtimeService _realtimeService;

    private SearchResultCollection _results = null;

    private String _q = null;

    public void setQ(String query) {
        if(query != null) {
            _q = query.trim();
        }
    }

    @Override
    public String execute() {
        if(_q == null || _q.isEmpty())
            return SUCCESS;

        _results = _searchService.getSearchResults(_q, new SearchResultFactoryV2Impl(_searchService, _nycTransitDataService, _realtimeService));

        return SUCCESS;
    }

    /**
     * VIEW METHODS
     */
    public SearchResultCollection getSearchResults() {
        return _results;
    }

}
