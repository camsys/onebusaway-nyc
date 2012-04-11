/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.m.wiki;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.wiki.model.NycWikiPageWrapper;
import org.onebusaway.wiki.api.WikiDocumentService;
import org.onebusaway.wiki.api.WikiRenderingService;
import org.springframework.beans.factory.annotation.Autowired;

public class IndexAction extends OneBusAwayNYCActionSupport implements ServletResponseAware, ServletRequestAware {

	private static final long serialVersionUID = 1L;
	
	private HttpServletResponse httpServletResponse;
	private HttpServletRequest httpServletRequest;

	@Autowired
	private WikiDocumentService _wikiDocumentService;

	@Autowired
	private WikiRenderingService _wikiRenderingService;

	protected String namespace;
	protected String name;
	
	private boolean forceRefresh = false;

	private String content;
	private String title;
	private String editLink;
	private Date lastModifiedTimestamp;
	
	private String toc;
	private String adminToc;
	private boolean hasToc = false;

	private static final Pattern tocLinkPattern = 
			Pattern.compile("<a[^>]?href=\"([^\"]*)\"[^>]?>[^<]*</a>");
	
	public void setForceRefresh(boolean forceRefresh){
	  this.forceRefresh = forceRefresh;
	}
	
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

	public String getTitle() {
		return title;
	}

	public String getToc() {
	  String tocContent = toc;
	  if(adminToc != null)
		  tocContent += adminToc;
	  
	  // find all links in the TOC; add class="active" to the one that points to 
	  // the page we're viewing now.
	  Matcher m = tocLinkPattern.matcher(tocContent);
	  while (m.find()) {
		String match = m.group();
		String matchLinkUrl = m.group(1);		
		if(matchLinkUrl != null) {
			String urlEnd = this.namespace + "/" + this.name;
			if(matchLinkUrl.endsWith(urlEnd)) {
				String newMatch = match.replace("href=", "class=\"active\" href=");
				return tocContent.replace(match, newMatch);
			}
		}
	  }
	  return tocContent;
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
			namespace = "MainMobile";
		}
		
		if(name == null || name.isEmpty()) {
			name = "Index";
		}
		
		if (namespace != null && name != null) {
			// TODO: TOC stuff. Possible remove this.
			// try to get TOC page for this namespace
//			try {
//				NycWikiPageWrapper page = 
//						new NycWikiPageWrapper(_wikiDocumentService.getWikiPage(namespace, "TOC", forceRefresh));
//
//				if(page.pageExists()) {
//					toc = _wikiRenderingService.renderPage(page);	
//					hasToc = true;
//				} else {
//					toc = null;
//					hasToc = false;
//				}
//			} catch (Exception ex) {
//				toc = null;
//				hasToc = false;
//			}
//
//			if(this.isAdmin()) {
//				// try to get admin TOC page for this namespace
//				try {
//					NycWikiPageWrapper adminPage = 
//							new NycWikiPageWrapper(_wikiDocumentService.getWikiPage(namespace, "AdminTOC", forceRefresh));
//
//					if(adminPage.pageExists()) {
//						adminToc = _wikiRenderingService.renderPage(adminPage);	
//						hasToc = true;
//					} else {
//						adminToc = null;
//					}
//				} catch(Exception ex) {
//					adminToc = null;
//				}
//			} else {
//				adminToc = null;
//			}
			
			// content for page
			try {
				NycWikiPageWrapper page = 
						new NycWikiPageWrapper(_wikiDocumentService.getWikiPage(namespace, name, false));
				
				if(page.pageExists()) {
					content = _wikiRenderingService.renderPage(page);
					String contextPath = this.httpServletRequest.getContextPath();
					content = content.replaceAll("href=\"/wiki/(.*?)Mobile/(.*?)", "href=\"" + contextPath + "/m/wiki/$1/$2");
				    editLink = _wikiRenderingService.getEditLink(page);
					title = page.getTitle();
				    lastModifiedTimestamp = page.getLastModified();
				} else {
					content = null;
					editLink = null;
					title = null;
					lastModifiedTimestamp = null;
					
					return "NotFound";
				}
			} catch (Exception ex) {
				throw new JspException(ex);
			}
		}

		return SUCCESS;
	}

	@Override
	public void setServletResponse(HttpServletResponse httpServletResponse) {
		this.httpServletResponse = httpServletResponse;
	}

	@Override
	public void setServletRequest(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}
}
