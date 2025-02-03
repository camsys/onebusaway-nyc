package org.onebusaway.nyc.sms.impl;

import org.onebusaway.nyc.sms.services.GoogleAnalyticsSessionAware;
import org.onebusaway.nyc.sms.services.SessionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Component
public class MobileCommonsSessionInterceptor implements HandlerInterceptor {

  private SessionManager _sessionManager;

  private String _sessionIdParameterName = "profile_id";

  public void setSessionIdParameterName(String sessionIdParameterName) {
    _sessionIdParameterName = sessionIdParameterName;
  }

  @Override
  public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) throws Exception {
    // Only process for handler methods
    if (!(handler instanceof HandlerMethod)) {
      return true;
    }

    String sessionId = request.getParameter(_sessionIdParameterName);
    if (sessionId == null || sessionId.isEmpty()) {
      return true;
    }

    boolean sessionIsNew = !_sessionManager.contextExistsFor(sessionId);
    Map<String, Object> persistentSession = _sessionManager.getContext(sessionId);

    // Store original session and attributes
    HttpSession originalSession = request.getSession();
    RequestAttributes originalAttributes = RequestContextHolder.getRequestAttributes();

    // Create custom request attributes
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attributes);

    // Get the handler method and target object
    HandlerMethod handlerMethod = (HandlerMethod) handler;
    Object targetObject = handlerMethod.getBean();

    if (targetObject instanceof GoogleAnalyticsSessionAware && sessionIsNew) {
      ((GoogleAnalyticsSessionAware) targetObject).initializeSession(sessionId);
    }

    // Store persistent session and context for post-handle processing
    request.setAttribute("_persistentSession", persistentSession);
    request.setAttribute("_originalSession", originalSession);
    request.setAttribute("_sessionId", sessionId);
    request.setAttribute("_originalAttributes", originalAttributes);

    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request,
                         HttpServletResponse response,
                         Object handler,
                         ModelAndView modelAndView) throws Exception {
    // Save session context if session ID exists
    String sessionId = (String) request.getAttribute("_sessionId");
    if (sessionId != null) {
      _sessionManager.saveContext(sessionId);
    }
  }

  @Override
  public void afterCompletion(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler,
                              Exception ex) throws Exception {
    // Restore original request attributes and close session
    String sessionId = (String) request.getAttribute("_sessionId");
    if (sessionId != null) {
      RequestAttributes originalAttributes =
              (RequestAttributes) request.getAttribute("_originalAttributes");
      RequestContextHolder.setRequestAttributes(originalAttributes);

      HttpSession originalSession =
              (HttpSession) request.getAttribute("_originalSession");

      _sessionManager.close();
    }
  }
}