package org.onebusaway.nyc.webapp.actions.wiki;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.wiki.api.WikiPage;

public class WikiPageWrapper implements WikiPage {
	private static final long serialVersionUID = 2L;

	private static final Pattern linkPattern = Pattern.compile("([^\\[|\\>]*)]]");
	
	private WikiPage page;
	
	public WikiPageWrapper(WikiPage realPage) {
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
		
		Matcher m = linkPattern.matcher(content);
		while (m.find()) {
			String match = m.group();
			content = content.replace(match, match.replace(".", "/"));
		}
		return content;
	}
}