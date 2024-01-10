/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.web.serializers.csv;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.List;

import com.opensymphony.xwork2.ActionInvocation;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.ListWithReferencesBean;
import org.onebusaway.api.web.serializers.xml.CustomXStreamHandler;
import org.onebusaway.csv_entities.CsvEntityWriterFactory;
import org.onebusaway.csv_entities.EntityHandler;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class CustomCsvHandler extends CustomXStreamHandler {

  public CustomCsvHandler(){
    super(MediaType.TEXT_PLAIN_TYPE);
  }

  public void toObject(Reader in, Object target) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void toObject(ActionInvocation actionInvocation, Reader in, Object target) throws IOException {
    throw new UnsupportedOperationException();
  }

  public String fromObject(Object obj, String resultCode, Writer stream)
      throws IOException {
    return null;
  }

  public String fromObject(ActionInvocation actionInvocation, Object obj, String resultCode, Writer stream) throws IOException {
    return fromObject(obj,stream);
  }

  public String fromObject(Object obj, Writer stream) throws IOException {
    CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
    Class<?> entityType = getEntityType(obj);
    EntityHandler csvHandler = factory.createWriter(entityType, stream);

    List<?> values = getEntityValues(obj);
    for (Object value : values)
      csvHandler.handleEntity(value);

    return null;
  }

  public String getContentType() {
    return "text/plain";
  }

  public String getExtension() {
    return "csv";
  }

  /****
   * 
   ****/

  private Class<?> getEntityType(Object obj) {
    if (obj instanceof ResponseBean) {
      ResponseBean response = (ResponseBean) obj;
      if (response.getData() == null)
        return response.getClass();
      return getEntityType(response.getData());
    } else if (obj instanceof EntryWithReferencesBean) {
      EntryWithReferencesBean<?> entry = (EntryWithReferencesBean<?>) obj;
      return entry.getEntry().getClass();
    } else if (obj instanceof ListWithReferencesBean) {
      ListWithReferencesBean<?> list = (ListWithReferencesBean<?>) obj;
      List<?> values = list.getList();
      if (values.isEmpty())
        return Object.class;
      return values.get(0).getClass();
    }
    return obj.getClass();
  }

  @SuppressWarnings("unchecked")
  private List<?> getEntityValues(Object obj) {
    if (obj instanceof ResponseBean) {
      ResponseBean response = (ResponseBean) obj;
      if (response.getData() == null)
        return Arrays.asList(response);
      return getEntityValues(response.getData());
    } else if (obj instanceof EntryWithReferencesBean) {
      EntryWithReferencesBean<?> entry = (EntryWithReferencesBean<?>) obj;
      return Arrays.asList(entry.getEntry());
    } else if (obj instanceof ListWithReferencesBean) {
      ListWithReferencesBean<?> list = (ListWithReferencesBean<?>) obj;
      return list.getList();
    }
    return Arrays.asList(obj);
  }


  @Override
  public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
    Writer outputWriter = new OutputStreamWriter(entityStream);
    fromObject(o,outputWriter);
    outputWriter.close();

  }


  @Override
  public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
    throw new UnsupportedOperationException();
  }
}
