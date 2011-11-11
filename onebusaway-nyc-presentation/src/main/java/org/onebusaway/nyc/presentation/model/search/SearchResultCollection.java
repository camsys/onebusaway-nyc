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
package org.onebusaway.nyc.presentation.model.search;

import org.onebusaway.nyc.presentation.service.search.SearchResult;

import java.util.ArrayList;

public class SearchResultCollection extends ArrayList<SearchResult> {

  private static final long serialVersionUID = 1L;
  
  public String getTypeOfResults() {
    SearchResult item = this.get(0);
    
    if(item != null) {
      return item.getResultType();
    } else
      return null;
  }
  
}
