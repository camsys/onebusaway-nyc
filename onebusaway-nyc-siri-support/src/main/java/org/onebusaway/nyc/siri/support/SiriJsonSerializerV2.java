package org.onebusaway.nyc.siri.support;

import org.springframework.util.ReflectionUtils;

import uk.org.siri.siri_2.Siri;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/** 
 * Serializer for XSD-generated SIRI classes, creating JSON in the format suitable
 * for Bus Time front-ends and third-party apps.
 * 
 * @author jmaki
 *
 */
public class SiriJsonSerializerV2 {
  
  public String getJson(Siri siri) throws Exception {    
    return getJson(siri, null);
  }
  
  public String getJson(Siri siri, String callback) throws Exception {    
    ObjectMapper mapper = new ObjectMapper();    
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);   
    
    mapper.setDateFormat(new RFC822SimpleDateFormat());

    mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(
			mapper.getTypeFactory()));

    mapper.registerModule(new JacksonModule());

    String output = "";

    if(callback != null)
      output = callback + "(";

    output += mapper.writeValueAsString(siri);

    if(callback != null)
      output += ")";

    return output;
  }
  
  private static class RFC822SimpleDateFormat extends SimpleDateFormat {
    private static final long serialVersionUID = 1L;

    public RFC822SimpleDateFormat() {
      super("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }
    
    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
      StringBuffer sb = super.format(date, toAppendTo, pos);
      sb.insert(sb.length() - 2, ":");
      return sb;
    }
  }
  
  private static class JacksonModule extends Module {
    @SuppressWarnings("deprecation")
	private final static Version VERSION = new Version(1,0,0, null);
    
    @Override
    public String getModuleName() {
      return "CustomSerializer";
    }

    @Override
    public Version version() {
      return VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
      context.addBeanSerializerModifier(new CustomBeanSerializerModifier());
    }
  }
  
  private static class CustomBeanSerializerModifier extends BeanSerializerModifier {

    public JsonSerializer<?> modifySerializer(SerializationConfig config,
    		BeanDescription beanDesc, JsonSerializer<?> serializer) {
      
      if(serializer instanceof BeanSerializer) {
        List<BeanPropertyDefinition> properties = beanDesc.findProperties();
        for(BeanPropertyDefinition property : properties) {
          if(property.getName().equals("value") || property.getName().equals("any")) {
            String fieldName = property.getField().getName();
            if(fieldName != null)
              return super.modifySerializer(config, beanDesc, new CustomValueObjectSerializer((BeanSerializer)serializer, fieldName));
          }
        }
        
      }
      
      return super.modifySerializer(config, beanDesc, serializer);
    }
  }
  
  private static class CustomValueObjectSerializer extends BeanSerializerBase {

    private String fieldName = null;
    
    protected CustomValueObjectSerializer(BeanSerializer src, String fieldName) {
      super(src);
      this.fieldName = fieldName;
    }

    @Override
    public void serialize(Object bean, JsonGenerator jgen,
        SerializerProvider provider) throws IOException, JsonGenerationException {
      
      try {
        Class<? extends Object> beanClass = bean.getClass();
        Field valueField = ReflectionUtils.findField(beanClass, fieldName);
        valueField.setAccessible(true);

        Object value = valueField.get(bean);
  
        provider.defaultSerializeValue(value, jgen);
      } catch(Exception e) {
        jgen.writeNull();
      }
    }

	@Override
	public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected BeanSerializerBase withIgnorals(Set<String> toIgnore) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected BeanSerializerBase asArraySerializer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BeanSerializerBase withFilterId(Object filterId) {
		// TODO Auto-generated method stub
		return null;
	}
    
  }

  
}