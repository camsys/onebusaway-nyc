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

package org.onebusaway.nyc.transit_data_manager.adapters.api;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.Option;
import uk.co.flamingpenguin.jewel.cli.Unparsed;

public interface GenericOptions {

  @Option(helpRequest = true)
  boolean getHelp();

  // This specifies an option for the command line named type with a string value ie '$ <cmd> --type value'
  @Option
  String getType();
  
  // Make an option for the name of the depot id translation file.
  @Option(shortName="d",defaultToNull=true)
  File getDepotIdConfig();

  @Unparsed
  List<File> getFiles();
}
