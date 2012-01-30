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
package org.onebusaway.nyc.vehicle_tracking.model.csv;

import org.onebusaway.csv_entities.TokenizerStrategy;

import java.util.Arrays;
import java.util.List;

public class TabTokenizerStrategy implements TokenizerStrategy {
  @Override
  public List<String> parse(String line) {
    return Arrays.asList(line.split("\t"));
  }

  @Override
  public String format(Iterable<String> tokens) {
    boolean seenFirst = false;
    StringBuilder b = new StringBuilder();
    for (String token : tokens) {
      if (seenFirst)
        b.append('\t');
      else
        seenFirst = true;

      b.append(token);
    }
    return b.toString();
  }
}