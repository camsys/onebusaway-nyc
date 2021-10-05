package org.onebusaway.api.serializers.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.rest.handler.AbstractContentTypeHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


public class CustomJsonLibHandler extends AbstractContentTypeHandler {

        private String defaultEncoding = "ISO-8859-1";
        private ObjectMapper mapper = new ObjectMapper();

        public void toObject(ActionInvocation invocation, Reader in, Object target) throws IOException {
                this.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
                ObjectReader or = this.mapper.readerForUpdating(target);
                or.readValue(in);
        }

        public String fromObject(ActionInvocation invocation, Object obj, String resultCode, Writer stream) throws IOException {
                String callback = getCallback();
                return fromObject(invocation, obj, resultCode, stream, callback);
        }

        public String fromObject(ActionInvocation invocation, Object obj, String resultCode, Writer stream, String callback) throws IOException {
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
                HttpServletRequest req = ServletActionContext.getRequest();
                if (req != null) {
                        callback = req.getParameter("callback");
                }
                return callback;
        }

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

        @Inject("struts.i18n.encoding")
        public void setDefaultEncoding(String val) {
                this.defaultEncoding = val;
        }
}