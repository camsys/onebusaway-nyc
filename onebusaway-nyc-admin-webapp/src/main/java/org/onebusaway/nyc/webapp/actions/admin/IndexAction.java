package org.onebusaway.nyc.webapp.actions.admin;

import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class IndexAction extends OneBusAwayNYCAdminActionSupport {

	@Autowired
	private ConfigurationServiceClient _configurationServiceClient;
	
	private static final long serialVersionUID = 1L;
	private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
	
	public boolean getConfig(String partialKey) {
		_log.info("partialKey=" + partialKey);
		boolean result = true; // default is to show feature
		try {
			List<Map<String, String>> components = _configurationServiceClient.getItems("api", "config", "list");
			if (components == null) {
				_log.info("getItems call failed");
				return result;
			}
			for (Map<String, String> component: components) {
				_log.info("component=" + component);
				if (component.containsKey("component") && "admin".equals(component.get("component"))) {
					_log.info("found admin component");
					if (partialKey.equals(component.get("key"))) {
						_log.info("found key=" + partialKey + ", and value=" + component.get("value"));
						return "true".equalsIgnoreCase(component.get("value"));
					}
				}
			}
		} catch (Exception e) {
			_log.error("config query broke:", e);
		}
		return result;
	}
}
