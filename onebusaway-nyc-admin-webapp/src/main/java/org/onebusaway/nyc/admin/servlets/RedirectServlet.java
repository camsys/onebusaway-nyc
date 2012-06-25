package org.onebusaway.nyc.admin.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple redirect that redirects request to the value in URI request param
 * @author abelsare
 *
 */
public class RedirectServlet extends HttpServlet{
	
	private static final long serialVersionUID = 1L;

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws IOException{
		response.sendRedirect(request.getContextPath() + request.getParameter("uri"));
	}

}
