package org.onebusaway.nyc.webapp.api;

import org.onebusaway.users.impl.CreateApiKeyAction;

/**
 * Creates 'OBANYC' API key on context initialization if key creation is enabled by Maven profile. 
 * Used only in application context at this point.
 * @author abelsare
 *
 */
public class CreateAPIKeyOnInitAction extends CreateApiKeyAction {
	
	private boolean createAPIKey = false;
	
	public void init() {
		if(createAPIKey) {
			super.execute();
		}
	}

	/**
	 * @param createAPIKey the createAPIKey to set
	 */
	public void setCreateAPIKey(boolean createAPIKey) {
		this.createAPIKey = createAPIKey;
	}

}
