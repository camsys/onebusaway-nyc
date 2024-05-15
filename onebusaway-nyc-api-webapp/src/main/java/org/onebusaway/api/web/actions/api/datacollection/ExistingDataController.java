/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.web.actions.api.datacollection;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.services.DataCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/datacollection")
public class ExistingDataController extends ApiActionSupport {
  
  private static final int V1 = 1;

  @Autowired
  private DataCollectionService _data;
  
  public ExistingDataController() {
    super(V1);
  }

  private static final long serialVersionUID = 1L;

  @GetMapping("/existing-data")
  public ResponseEntity<ResponseBean> index() {
    List<String> values = new ArrayList<String>();
    File dataDirectory = _data.getDataDirectory();
    File[] files = dataDirectory.listFiles();
    if( files != null) {
      for( File file : files)
        values.add(file.getName());
    }
    return getOkResponseBean(values);
  }

}
