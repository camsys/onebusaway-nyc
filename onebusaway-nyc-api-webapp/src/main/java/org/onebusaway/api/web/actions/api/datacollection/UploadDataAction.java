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
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//@RestController
//@RequestMapping("/datacollection/upload-data")
public class UploadDataAction extends ApiActionSupport {

  private static final int V1 = 1;
  
  public UploadDataAction() {
    super(V1);
  }

  private static final long serialVersionUID = 1L;

  @Autowired
  private DataCollectionService _data;

//  @RequestMapping
  public ResponseEntity<ResponseBean> update(@RequestParam(name ="Id", required = false) String id,
                                             @RequestParam(name ="File", required = false) File file) throws IOException {

    File dataDirectory = _data.getDataDirectory();
    File target = new File(dataDirectory, id);

    InputStream from = new ByteArrayInputStream(new byte[0]);
    if( file.exists() )
      from = new FileInputStream(file);
    OutputStream to = new FileOutputStream(target);

    byte[] buffer = new byte[4096];
    int bytesRead;

    while ((bytesRead = from.read(buffer)) != -1)
      to.write(buffer, 0, bytesRead); // write

    return getOkResponseBean(null);
  }
}
