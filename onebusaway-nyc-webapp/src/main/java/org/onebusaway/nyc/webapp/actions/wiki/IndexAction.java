package org.onebusaway.nyc.webapp.actions.wiki;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspException;

import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.wiki.api.WikiDocumentService;
import org.onebusaway.wiki.api.WikiRenderingService;
import org.springframework.beans.factory.annotation.Autowired;

public class IndexAction extends OneBusAwayNYCActionSupport {

	private static final long serialVersionUID = 1L;

	@Autowired
	private WikiDocumentService _wikiDocumentService;

	@Autowired
	private WikiRenderingService _wikiRenderingService;

	protected String namespace;
	protected String name;

	private String content;
	private Date lastModifiedTimestamp;
	private String editLink;
	
	private String toc;
	private boolean hasToc = false;
	private static final Pattern tocLinkPattern = Pattern.compile("<a[^>]?href=\"([^\"]*)\"[^>]?>[^<]*</a>");
	
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
	  // find all links in the TOC; add class="active" to the one that points to 
	  // the page we're viewing now.
	  Matcher m = tocLinkPattern.matcher(toc);
	  while (m.find()) {
		String match = m.group();
		String matchLinkUrl = m.group(1);		
		if(matchLinkUrl != null) {
			String urlEnd = this.namespace + "/" + this.name;
			if(matchLinkUrl.endsWith(urlEnd)) {
				String newMatch = match.replace("href=", "class=\"active\" href=");
				return toc.replace(match, newMatch);
			}
		}
	  }
	  return toc;
	}
	
	public String getLastModifiedTimestamp() {
		if(lastModifiedTimestamp == null)
			return "Unknown";
		
		return DateFormat.getDateInstance().format(lastModifiedTimestamp) + " at " + 
				DateFormat.getTimeInstance().format(lastModifiedTimestamp);
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
		if(namespace == null || namespace.isEmpty()) {
			namespace = "Main";
		}
		
		if(name == null || name.isEmpty()) {
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
				    lastModifiedTimestamp = page.getLastModified();
				} else {
					content = null;
					editLink = null;
					lastModifiedTimestamp = null;
					
					return "NotFound";
				}
			} catch (Exception ex) {
				throw new JspException(ex);
			}
		}

		return SUCCESS;
	}
}
