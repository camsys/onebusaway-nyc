package org.onebusaway.nyc.webapp.actions.wiki;

import javax.servlet.jsp.JspException;

import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.wiki.api.WikiDocumentService;
import org.onebusaway.wiki.api.WikiPage;
import org.onebusaway.wiki.api.WikiRenderingService;
import org.springframework.beans.factory.annotation.Autowired;

public class IndexAction extends OneBusAwayNYCActionSupport {

	private static final long serialVersionUID = 1L;

	@Autowired
	private WikiDocumentService _wikiDocumentService;

	@Autowired
	private WikiRenderingService _wikiRenderingService;

	protected String namespace = "Main";
	protected String name;
	protected String content;
	protected boolean isHelp;
	
	@Override
	public String execute() throws Exception {

		if (namespace != null && name != null) {
			isHelp = (name.length() >= 4 && name.substring(0, 4).compareTo("Help") == 0);					
			
			try {
				WikiPage page = _wikiDocumentService.getWikiPage(namespace, name, false);

				if (page != null) {
					content = _wikiRenderingService.renderPage(page);					
				} else {
					content = "<h2>404 Not Found</h2><p>The requested page was not found on the server.</p>";
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

	public void setContent(String content) {
		this.content = content;
	}

	public boolean getIsHelp() {
		return isHelp;
	}
	
	public void setIsHelp(boolean isHelp) {
		this.isHelp = isHelp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
