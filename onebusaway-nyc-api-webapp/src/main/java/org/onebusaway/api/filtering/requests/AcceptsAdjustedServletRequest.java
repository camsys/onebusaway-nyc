package org.onebusaway.api.filtering.requests;

import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class AcceptsAdjustedServletRequest extends HttpServletRequestWrapper {

    Enumeration headerNames;
    Enumeration acceptHeaders;
    String acceptHeader;
    String uri;

    public AcceptsAdjustedServletRequest(HttpServletRequest request){
        super(request);
        headerNames = request.getHeaderNames();
        acceptHeaders = request.getHeaders("accept");
        acceptHeader = request.getHeader("accept");
        uri = request.getRequestURI();
        String format = uri.substring(uri.length()-4);
        if(format.equals("json")){
            acceptHeader = MediaType.APPLICATION_JSON_VALUE;
            acceptHeaders = addToHeaders(MediaType.APPLICATION_JSON_VALUE,acceptHeaders);
            uri = uri.substring(0,uri.length()-".json".length());
        }
        else if(format.equals(".xml")){
            acceptHeader = MediaType.APPLICATION_XML_VALUE;
            acceptHeaders = addToHeaders(MediaType.APPLICATION_XML_VALUE,acceptHeaders);
            uri = uri.substring(0,uri.length()-".xml".length());
        }
    }

    @Override
    public String getRequestURI(){
        return uri;
    }

    @Override
    public String getHeader(String name) {
        if(name.equals("accept"))
            return acceptHeader;
        return super.getHeader(name);
    }

    @Override
    public Enumeration getHeaders(String name){
        if(name.equals("accept"))
            return acceptHeaders;
        return super.getHeaders(name);
    }

    @Override
    public Enumeration getHeaderNames() {
        return headerNames;
    }

    private Enumeration<String> addToHeaders(String value,Enumeration<String> headers){
        List<String> out = new ArrayList<String>();
        out.add(value);
//        while(headers.hasMoreElements()){
//            out.add(headers.nextElement());
//        }
        return Collections.enumeration(out);
    }


}
