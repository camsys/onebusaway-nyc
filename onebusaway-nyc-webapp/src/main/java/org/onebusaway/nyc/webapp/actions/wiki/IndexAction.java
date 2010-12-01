package org.onebusaway.nyc.webapp.actions.wiki;

import javax.servlet.jsp.JspException;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.wiki.api.WikiDocumentService;
import org.onebusaway.wiki.api.WikiPage;
import org.onebusaway.wiki.api.WikiRenderingService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;


@Result(location = "/WEB-INF/content/wiki/index.jspx")
@Namespace("/wiki/*")
public class IndexAction extends OneBusAwayNYCActionSupport {

	private static final long serialVersionUID = 1L;

	@Autowired
	private WikiDocumentService _wikiDocumentService;

	@Autowired
	private WikiRenderingService _wikiRenderingService;

	protected String namespace = "Main";
	protected String content;
	protected String toc;
	protected String editLink;
	protected boolean hasToc = false;
	
	public boolean isAdmin() {
		return _currentUserService.isCurrentUserAdmin();
	}
	
	public boolean getHasToc() {
		return hasToc;
	}
	
	public String getEditLink() {
		return editLink;
	}
	
	private String getTocPageName(String name) {
		if(name == null)
			return null;
		
        StringBuffer sb = new StringBuffer(name.length());

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (Character.isUpperCase(c) && i > 0)
            	break;
            else 
            	sb.append(c);
        }
        
        return sb.toString() + "TOC";
	}
	
	@Override
	public String execute() throws Exception {
	    ActionContext context = ActionContext.getContext();
	    ActionInvocation invocation = context.getActionInvocation();
	    ActionProxy proxy = invocation.getProxy();

	    String name = proxy.getActionName();
		
		if (namespace != null && name != null) {
			// try to get TOC page for this section
			try {
				WikiPage page = _wikiDocumentService.getWikiPage(namespace, this.getTocPageName(name), false);

				if (page != null) {
					toc = _wikiRenderingService.renderPage(page);	
					hasToc = true;
				} else {
					toc = null;
					hasToc = false;
				}
			} catch (Exception ex) {
				hasToc = false;
			}

			// content for page
			try {
				WikiPage page = _wikiDocumentService.getWikiPage(namespace, name, false);

				if (page != null) {
					content = _wikiRenderingService.renderPage(page);	
				    editLink = _wikiRenderingService.getEditLink(page);
				} else {
					content = null;
					editLink = null;
					
					return "NotFound";
				}
			} catch (Exception ex) {
				throw new JspException(ex);
			}
		}

		return SUCCESS;
	}

	public String getContent() {
		return content;
	}

	public String getToc() {
		return toc;
	}
}
