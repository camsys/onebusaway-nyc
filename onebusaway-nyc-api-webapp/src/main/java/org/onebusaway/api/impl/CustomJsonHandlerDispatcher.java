package org.onebusaway.api.impl;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.rest.handler.ContentTypeHandler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

/**
 * Choose between the new Jackson JOSN serializer (for SIRI) or the legacy one
 * for OBA API. The latter and its conventions are maintained for backwards compatability, 
 * but might (hopefully) be removed someday? This is a super hack, but don't want to break 
 * existing OBA API conventions/apps.
 * 
 * @author jmaki
 *
 */
public class CustomJsonHandlerDispatcher implements ContentTypeHandler {

  private ContentTypeHandler getHandlerForRequest() {
    HttpServletRequest req = ServletActionContext.getRequest();

    String contextPath = req.getContextPath();
    String mungedRequestPath = req.getRequestURI().substring(contextPath.length());
    
    boolean isSiriRequest = mungedRequestPath.startsWith("/siri/");
    
    if(isSiriRequest)
      return new CustomJacksonJsonHandler();
    else
      return new CustomJsonLibHandler();
  }
  
  public void toObject(Reader in, Object target) throws IOException {
    ContentTypeHandler handler = getHandlerForRequest();
    handler.toObject(in, target);
  }

  public String fromObject(Object obj, String resultCode, Writer stream)
      throws IOException {

    ContentTypeHandler handler = getHandlerForRequest();
    return handler.fromObject(obj, resultCode, stream); 
  }

  public String getContentType() {
    return "text/javascript";
  }

  public String getExtension() {
    return "json";
  }
}