package org.onebusaway.nyc.queue_test;

import java.util.HashMap;

import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

public class SimplePropertyNamingStrategy extends PropertyNamingStrategy {
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
