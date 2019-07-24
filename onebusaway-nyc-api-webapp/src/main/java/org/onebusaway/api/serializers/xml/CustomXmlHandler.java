package org.onebusaway.api.serializers.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.opensymphony.xwork2.ActionInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class CustomXmlHandler {
    private static final Logger LOG = LogManager.getLogger(CustomXmlHandler.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/xml";
    private XmlMapper mapper = new XmlMapper();

    public CustomXmlHandler() {
    }

    public void toObject(ActionInvocation invocation, Reader in, Object target) throws IOException {
        LOG.debug("Converting input into an object of: {}", target.getClass().getName());
        ObjectReader or = this.mapper.readerForUpdating(target);
        or.readValue(in);
    }

    public String fromObject(ActionInvocation invocation, Object obj, String resultCode, Writer stream) throws IOException {
        LOG.debug("Converting an object of {} into string", obj.getClass().getName());
        //this.mapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

       /* JaxbAnnotationModule jaxbAnnotationModule = new JaxbAnnotationModule();
        mapper.registerModule(jaxbAnnotationModule);
*/

        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.writeValue(stream, obj);
        return null;
    }

    public String getContentType() {
        return "application/xml";
    }

    public String getExtension() {
        return "xml";
    }
}
