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

package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import java.io.IOException;

public class VehiclePipoUploadsFilePicker extends DateUbarTimeTimestampFilePicker {

	private static String FILE_PREFIX = "UTSPUPUFULL_";
	private static String FILE_SUFFIX = ".txt";
	  
  private String filePrefix;
  private String fileSuffix;

  public VehiclePipoUploadsFilePicker(String timestampedUploadsDirProperty)
      throws IOException {
    super(System.getProperty(timestampedUploadsDirProperty));
    filePrefix = FILE_PREFIX;
    fileSuffix = FILE_SUFFIX;
  }

  public VehiclePipoUploadsFilePicker(String timestampedUploadsDirProperty, String prefix, String suffix)
      throws IOException {
    super(System.getProperty(timestampedUploadsDirProperty));
    filePrefix = prefix;
    fileSuffix = suffix;
  }

	@Override
	protected String getFilePrefix() {
		return filePrefix;
	}

	@Override
	protected String getFileSuffix() {
		return fileSuffix;
	}

	void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}

	void setFileSuffix(String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}

}
