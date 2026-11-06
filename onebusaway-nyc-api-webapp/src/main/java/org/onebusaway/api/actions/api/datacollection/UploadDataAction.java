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
package org.onebusaway.api.actions.api.datacollection;

import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.services.DataCollectionService;
import org.apache.struts2.action.UploadedFilesAware;
import org.apache.struts2.dispatcher.multipart.UploadedFile;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Saves an uploaded data file into the data collection directory, named by the
 * request's id parameter.
 */
public class UploadDataAction extends ApiActionSupport implements UploadedFilesAware {

  private static final Logger _log = LoggerFactory.getLogger(UploadDataAction.class);

  private static final int V1 = 1;

  public UploadDataAction() {
    super(V1);
  }

  private static final long serialVersionUID = 1L;

  @Autowired
  private DataCollectionService _data;

  private String _id;

  private File _file;

  public void setId(String id) {
    _id = id;
  }

  /**
   * Receives the uploaded data from Struts 6's ActionFileUpload interceptor,
   * from the multipart field named "upload". Replaces the old setUpload(File)
   * setter, which only the deprecated FileUploadInterceptor called
   * (CVE-2024-53677).
   *
   * @param uploadedFiles every file in the multipart request
   */
  @Override
  public void withUploadedFiles(List<UploadedFile> uploadedFiles) {
    for (UploadedFile uploadedFile : uploadedFiles) {
      if ("upload".equals(uploadedFile.getInputName()) && uploadedFile.isFile()) {
        _file = (File) uploadedFile.getContent();
      }
    }
  }

  /**
   * Copies the uploaded file to the data directory under the name given by id.
   * Writes an empty file when the request carried no upload.
   *
   * @return OK, or a validation error when id is not a plain file name
   */
  public DefaultHttpHeaders update() throws IOException {
    // id becomes a file name inside the data directory, so refuse anything
    // that could point outside it (e.g. "../../etc/x")
    if (!isPlainFileName(_id)) {
      _log.warn("Rejected data upload with invalid id: " + _id);
      addActionError("id must be a plain file name");
      return setValidationErrorsResponse();
    }

    File target = new File(_data.getDataDirectory(), _id);

    try (InputStream from = (_file != null && _file.exists())
            ? new FileInputStream(_file) : new ByteArrayInputStream(new byte[0]);
         OutputStream to = new FileOutputStream(target)) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = from.read(buffer)) != -1)
        to.write(buffer, 0, bytesRead); // write
    }

    return setOkResponse(null);
  }

  /**
   * @return true when id is a single file name with no path parts
   */
  static boolean isPlainFileName(String id) {
    return id != null && !id.isEmpty() && !id.equals(".") && !id.equals("..")
        && id.indexOf('/') < 0 && id.indexOf('\\') < 0 && id.indexOf('\0') < 0;
  }
}
