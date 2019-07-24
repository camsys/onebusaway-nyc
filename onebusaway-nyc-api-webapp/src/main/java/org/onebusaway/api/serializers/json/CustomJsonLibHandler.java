package org.onebusaway.api.serializers.json;

import com.fasterxml.jackson.databind.*;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import org.apache.struts2.rest.handler.AbstractContentTypeHandler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


public class CustomJsonLibHandler extends AbstractContentTypeHandler {

        private static final String DEFAULT_CONTENT_TYPE = "application/json";
        private String defaultEncoding = "ISO-8859-1";
        private ObjectMapper mapper = new ObjectMapper();


        public void toObject(ActionInvocation invocation, Reader in, Object target) throws IOException {
                this.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                ObjectReader or = this.mapper.readerForUpdating(target);
                or.readValue(in);
        }

        public String fromObject(ActionInvocation invocation, Object obj, String resultCode, Writer stream) throws IOException {

                this.mapper.setSerializerProvider(new CustomSerializerProvider());
                this.mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
                this.mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
                this.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                this.mapper.writeValue(stream, obj);
                return null;
        }

        public String getContentType() {
                return "application/json;charset=" + this.defaultEncoding;
        }

        public String getExtension() {
                return "json";
        }

        @Inject("struts.i18n.encoding")
        public void setDefaultEncoding(String val) {
                this.defaultEncoding = val;
        }
}