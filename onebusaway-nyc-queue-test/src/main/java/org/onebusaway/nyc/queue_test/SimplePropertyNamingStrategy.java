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

package org.onebusaway.nyc.queue_test;

import java.util.HashMap;

import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplePropertyNamingStrategy extends PropertyNamingStrategy {
	protected static Logger _log = LoggerFactory
			.getLogger(SimplePropertyNamingStrategy.class);
  // custom mappings for various fields
  private HashMap<String, String> map = null;

  public SimplePropertyNamingStrategy(HashMap<String, String> map) {
    this.map = map;
  }

  @Override
  public String nameForField(MapperConfig<?> config, AnnotatedField field,
      String defaultName) {
    return convert(defaultName);
  }

  @Override
  public String nameForGetterMethod(MapperConfig<?> config,
      AnnotatedMethod method, String defaultName) {
    return convert(defaultName);
  }

  @Override
  public String nameForSetterMethod(MapperConfig<?> config,
      AnnotatedMethod method, String defaultName) {

    return convert(defaultName);
  }

  private String convert(String input) {
    // if custom map contains this field, return the custom value
    if (map.containsKey(input)) {
      return map.get(input);
    }
    // easy: replace capital letters with dash, lower-cases equivalent
    StringBuilder result = new StringBuilder();
    for (int i = 0, len = input.length(); i < len; ++i) {
      char c = input.charAt(i);
      if (Character.isUpperCase(c)) {
        result.append('-');
        c = Character.toLowerCase(c);
      }
      result.append(c);
    }
    return result.toString();
  }
}
