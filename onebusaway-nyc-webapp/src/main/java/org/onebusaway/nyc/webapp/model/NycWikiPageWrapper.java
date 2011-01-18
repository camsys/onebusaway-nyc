package org.onebusaway.nyc.webapp.model;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.wiki.api.WikiPage;

/*
 * This class wraps an WikiPageImpl and fixes up the lack of page namespace handling in the 
 * existing WikiPageImpl class. 
 */
public class NycWikiPageWrapper implements WikiPage {
	private static final long serialVersionUID = 2L;

	private static final Pattern linkPattern = Pattern.compile("([^\\[|\\>]*)]]");
	
	private WikiPage page;
	
	public NycWikiPageWrapper(WikiPage realPage) {
		this.page = realPage;
	}

	public boolean pageExists() {
		return (this.page != null);
	}
	
	public String getNamespace() {
		return this.page.getNamespace();
	}

	public String getName() {
		return this.page.getName();
	}

	public String getTitle() {
		return this.page.getTitle();
	}
	
	public Date getLastModified() {
		return this.page.getLastModified();
	}

	public String getContent() {
		String content = this.page.getContent();

		if(content == null) 
			return null;
		
		// replace a "." with a "/" in a Wiki markup link
		Matcher m = linkPattern.matcher(content);
		while (m.find()) {
			String match = m.group();
			content = content.replace(match, match.replace(".", "/"));
		}

		return content;
	}
}