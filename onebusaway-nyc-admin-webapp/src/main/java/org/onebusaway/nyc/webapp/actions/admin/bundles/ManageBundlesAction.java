package org.onebusaway.nyc.webapp.actions.admin.bundles;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;

/**
 * Base class to hold properties and methods required across all bundle building UI pages
 * @author abelsare
 *
 */
public class ManageBundlesAction extends OneBusAwayNYCAdminActionSupport {

	private static final long serialVersionUID = 1L;
	protected String bundleDirectory;
	
	/**
	 * @return the bundleDirectory
	 */
	public String getBundleDirectory() {
		return bundleDirectory;
	}
	
	/**
	 * @param bundleDirectory the bundleDirectory to set
	 */
	public void setBundleDirectory(String bundleDirectory) {
		this.bundleDirectory = bundleDirectory;
	}
	

}
