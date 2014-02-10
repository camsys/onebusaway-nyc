package org.onebusaway.nyc.util.impl.tdm;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class ConfigurationSerivceClientFileImpl implements
		ConfigurationServiceClient {

	private static Logger _log = LoggerFactory
			.getLogger(ConfigurationSerivceClientFileImpl.class);
	@Override
	public URL buildUrl(String baseObject, String... params) throws Exception {
		// TODO Auto-generated method stub
		_log.info("empty buildUrl");
		return null;
	}

	@Override
	public void setConfigItem(String baseObject, String component, String key,
			String value) throws Exception {
		// TODO Auto-generated method stub
		_log.info("empty setConfigItem");
	}

	@Override
	public String log(String baseObject, String component, Integer priority,
			String message) {
		// TODO Auto-generated method stub
		_log.info("empty log");
		return null;
	}

	@Override
	public List<JsonObject> getItemsForRequest(String baseObject,
			String... params) throws Exception {
		// TODO Auto-generated method stub
		_log.info("empty getItemsForRequest");
		return null;
	}

	@Override
	public List<Map<String, String>> getItems(String baseObject,
			String... params) throws Exception {
		// TODO Auto-generated method stub
		_log.info("empty getItems");
		return null;
	}

}
