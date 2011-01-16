package org.onebusaway.nyc.webapp.actions.wiki;

import java.util.Map;

import javax.servlet.jsp.JspException;

import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.wiki.api.WikiDocumentService;
import org.onebusaway.wiki.api.WikiRenderingService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;

@Result(location = "/WEB-INF/content/wiki/index.jspx")
public class IndexAction extends OneBusAwayNYCActionSupport {

	private static final long serialVersionUID = 1L;

	@Autowired
	private WikiDocumentService _wikiDocumentService;

	@Autowired
	private WikiRenderingService _wikiRenderingService;

	protected String namespace;
	protected String name;

	protected String content;
	protected String toc;
	protected boolean hasToc = false;

	protected String editLink;
	
	public boolean isAdmin() {
		return _currentUserService.isCurrentUserAdmin();
	}
	
	public boolean getHasToc() {
		return hasToc;
	}
	
	// FIXME: should replace namespace at the service level?
	public String getEditLink() {
		return editLink.replace("%{namespace}", namespace);
	}

	// FIXME: should replace namespace at the service level?
	public String getContent() {
		return content.replace("%{namespace}", namespace);
	}

	public String getToc() {
		return toc;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	@Override
	public String execute() throws Exception {		
		if(namespace == null) {
			namespace = "Main";
		}
		
		if(name == null) {
			name = "Index";
		}
		
		if (namespace != null && name != null) {
			// try to get TOC page for this namespace
			try {
				WikiPageWrapper page = new WikiPageWrapper(_wikiDocumentService.getWikiPage(namespace, "TOC", false));

				if(page.pageExists()) {
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
				WikiPageWrapper page = new WikiPageWrapper(_wikiDocumentService.getWikiPage(namespace, name, false));

				if(page.pageExists()) {
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
}
