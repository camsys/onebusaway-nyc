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
package org.onebusaway.nyc.presentation.impl;

/**
 * Utility to remove the agency id from an entity id
 */
public class WebappIdParser {
  
  public String parseIdWithoutAgency(String id) {
    if (id == null) throw new NullPointerException("id is null");
    id = id.trim();
    String[] fields = id.split("_", 2);
    if (fields.length != 2) throw new IllegalArgumentException("'" + id + "' does not look like an id with an agency");
    return fields[1];    
  }

}
