package org.onebusaway.api.impl;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.rest.handler.ContentTypeHandler;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

public class CustomJacksonJsonHandler implements ContentTypeHandler {

  private ObjectMapper _mapper;

  public CustomJacksonJsonHandler() {
    super();
    
    _mapper = new ObjectMapper();
    
    AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
    SerializationConfig config = _mapper.getSerializationConfig();
    config.setSerializationInclusion(Inclusion.NON_NULL);
    config.setAnnotationIntrospector(jaxb);
  }
  
  public void toObject(Reader in, Object target) throws IOException {
    throw new IOException("Deserialization not implemented");
  }

  public String fromObject(Object obj, String resultCode, Writer stream)
      throws IOException {

    String callback = null;
    HttpServletRequest req = ServletActionContext.getRequest();
    if (req != null)
      callback = req.getParameter("callback");

    fromObject(obj, resultCode, stream, callback);
    
    return null;
  }

  public String fromObject(Object obj, String resultCode, Writer stream, String callback)
      throws IOException {

    fromObject(obj, stream, callback);
    
    return null;
  }

  public void fromObject(Object obj, Writer stream, String callback)
      throws IOException {
    
    if(callback != null) {
      stream.write(callback);
      stream.write("(");
      stream.flush();
    }
    
    _mapper.writeValue(stream, obj);

    if(callback != null) {
      stream.write(")");
    }
    stream.flush(); 
  }

  public String getContentType() {
    return "text/javascript";
  }

  public String getExtension() {
    return "json";
  }
}
