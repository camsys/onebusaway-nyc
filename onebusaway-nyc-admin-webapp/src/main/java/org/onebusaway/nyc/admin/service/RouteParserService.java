/**
 * Copyright (C) 2016 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.admin.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.onebusaway.nyc.admin.model.ui.DataValidationMode;

/**
 * Provides a service to parse a file containing a FixedRouteDataValidation
 * report in a .csv format and return the parsed results.
 *
 * @author jpearson
 *
 */
public interface RouteParserService {
  public List<DataValidationMode> parseRouteReportFile(File checkFile);
  public List<DataValidationMode> parseRouteReportInputStream(InputStream fixedRouteReportInputStream, String fixedRouteReportPath);
}
