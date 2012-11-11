package org.onebusaway.nyc.transit_data_manager.config;

import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

/**
 * Names things like 'this-is-a-value' or 'value-type' in JSON.
 * @author sclark
 *
 */
public class AllLowerWithDashesNamingStrategy extends PropertyNamingStrategy {

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

  /**
   * Replace all capital letters with a dash and the lowercase version of the capital letter.
   * @param defaultName
   * @return
   */
  private String convert(String defaultName) {
    StringBuilder result = new StringBuilder();
    
    for (int i = 0; i < defaultName.length(); i++){
      char ch = defaultName.charAt(i);
      
      if (Character.isUpperCase(ch)) {
        result.append('-');
        ch = Character.toLowerCase(ch);
      }
      result.append(ch);
    }
    
    return result.toString();
  }

}
