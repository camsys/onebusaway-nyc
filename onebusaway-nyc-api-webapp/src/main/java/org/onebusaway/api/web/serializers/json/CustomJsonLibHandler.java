package org.onebusaway.api.web.serializers.json;

import com.fasterxml.jackson.databind.*;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import org.onebusaway.api.web.serializers.CustomUniversalHandler;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class CustomJsonLibHandler extends CustomUniversalHandler {

        // todo: struts to spring: look into removing extraneous methods

        private String defaultEncoding = "ISO-8859-1";
        private ObjectMapper mapper = new ObjectMapper();

        @Context
        HttpServletRequest request;

        public CustomJsonLibHandler(){
                super(MediaType.APPLICATION_JSON_TYPE);
        }

        public void toObject(Reader in, Object target) throws IOException {
                this.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                ObjectReader or = this.mapper.readerForUpdating(target);
                or.readValue(in);
        }

        public String fromObject(Object obj, Writer stream, String callback) throws IOException {
                mapper.setSerializerProvider(new CustomSerializerProvider());
                mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
                mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
                mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

                String value = mapper.writeValueAsString(obj);

                if (callback != null) {
                        stream.write(callback + "(" + value + ")");
                }
                else {
                        stream.write(value);
                }
                return null;
        }

        public String getCallback(){
                String callback = null;
                if (request != null) {
                        callback = request.getParameter("callback");
                }
                return callback;
        }



        @Inject("struts.i18n.encoding")
        public void setDefaultEncoding(String val) {
                this.defaultEncoding = val;
        }


        @Override
        public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
                this.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                return mapper.readValue(entityStream,type);
        }

        @Override
        public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                Writer writer = new OutputStreamWriter(entityStream);
                fromObject(o,writer,getCallback());
                writer.close();

        }




        // delete these after migration:

        public String getContentType() {
                String callback = getCallback();
                if(callback != null){
                        return ("application/javascript");
                }
                return "application/json;charset=" + this.defaultEncoding;
        }

        public String getExtension() {
                return "json";
        }

        public String fromObject(ActionInvocation invocation, Object obj, String resultCode, Writer stream) throws IOException {
                String callback = getCallback();
                return fromObject(invocation, obj, resultCode, stream, callback);
        }

        public String fromObject(ActionInvocation invocation, Object obj, String resultCode, Writer stream, String callback) throws IOException {
                return fromObject(obj,stream,callback);
        }
}