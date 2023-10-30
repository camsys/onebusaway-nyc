package org.onebusaway.api.filtering.filters;

import org.onebusaway.api.filtering.requests.AcceptsAdjustedServletRequest;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AcceptHeaderFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request = new AcceptsAdjustedServletRequest((HttpServletRequest) request);

        chain.doFilter(request, response);
//        List<String> names = Collections.list(req.getHeaderNames());
//        names.addAll(Collections.list(req.getParameterNames()));
////        for(String name : names)
////        Enumeration<String> headers = req.getHeaders();
//        List<String> accepts = Collections.list(req.getHeaders("accept"));
//        Enumeration<String> acceptsActual = req.getHeaders("accept");
//        acceptsActual
//        int i = 11;
    }

    @Override
    public void destroy() {

    }
}
