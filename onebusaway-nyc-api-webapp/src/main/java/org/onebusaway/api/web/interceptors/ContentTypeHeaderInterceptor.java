package org.onebusaway.api.web.interceptors;

import org.onebusaway.api.web.interceptors.filtering.requests.AcceptsAdjustedServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class ContentTypeHeaderInterceptor implements HandlerInterceptor {

    @PostConstruct
    public void sample(){
        int i = 11;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String currentContentType = response.getContentType();
        if (currentContentType==null)
            currentContentType="";
        uri = request.getRequestURI();
        String format = uri.substring(uri.length()-4);
        if(format.equals("json")){
            currentContentType = MediaType.APPLICATION_JSON_VALUE + ";" + currentContentType;
        }
        else if(format.equals(".xml")){
            currentContentType = MediaType.APPLICATION_XML_VALUE + ";" + currentContentType;
        }
        else if(format.equals(".csv")){
            currentContentType = MediaType.TEXT_PLAIN_VALUE + ";" + currentContentType;
        }
        response.setContentType(currentContentType);
        return true;
    }



    Enumeration headerNames;
    Enumeration acceptHeaders;
    String acceptHeader;
    String uri;


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }


    private Enumeration<String> addToHeaders(String value,Enumeration<String> headers){
        List<String> out = new ArrayList<String>();
        out.add(value);
//        while(headers.hasMoreElements()){
//            out.add(headers.nextElement());
//        }
        return Collections.enumeration(out);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        int i = 11;
        // This is called after the complete request has finished
        // It's called after rendering the view, hence useful for cleanup tasks
    }
}
