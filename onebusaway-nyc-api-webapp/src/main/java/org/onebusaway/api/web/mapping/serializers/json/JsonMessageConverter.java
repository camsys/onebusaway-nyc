package org.onebusaway.api.web.mapping.serializers.json;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Component
public class JsonMessageConverter implements HttpMessageConverter {

    Set<MediaType> myMediaTypes;

    CustomJsonLibHandler handler;
    public JsonMessageConverter(){
        handler = new CustomJsonLibHandler();
        myMediaTypes = new HashSet<>(List.of(MediaType.APPLICATION_JSON));
    }
    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        if(mediaType==null){return true;}
        if(myMediaTypes.contains(mediaType)){
            return true;
        }
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.copyOf(myMediaTypes);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return handler.readFrom(clazz,null,null,null,null,inputMessage.getBody());
    }

    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        handler.writeTo(o,null,null,null,null,null,outputMessage.getBody());
    }
}
