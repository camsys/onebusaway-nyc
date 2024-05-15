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

import com.google.gson.JsonObject;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.services.DataCollectionService;
import org.onebusaway.api.web.actions.api.where.FieldErrorSupport;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/datacollection")
public class UploadDataController extends ApiActionSupport {

  private static final int V1 = 1;

  public UploadDataController() {
    super(V1);
  }

  private static final long serialVersionUID = 1L;

  @Autowired
  private DataCollectionService _data;

  @PostMapping("/upload-data")
  public ResponseEntity<ResponseBean> update(@RequestParam(name ="Id", required = false) String id,
                                             @RequestParam(name ="Data", required = false) MultipartFile data) throws IOException {

    FieldErrorSupport fes = new FieldErrorSupport();
    if (id == null || id.isEmpty()) {
      fes = fes.invalidValue("Id");
    }

    if (data == null || data.isEmpty()) {
      fes = fes.invalidValue("Data");
    }

    if(fes.hasErrors()){
      return getValidationErrorsResponseBean(fes.getErrors());
    }

    File target = new File(_data.getDataDirectory(), id);

    try (InputStream from = data.getInputStream();
         OutputStream to = new FileOutputStream(target)) {

      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = from.read(buffer)) != -1) {
        to.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      return getExceptionResponse("File processing error");
    }

    return getOkResponseBean("File uploaded successfully");
  }
}
