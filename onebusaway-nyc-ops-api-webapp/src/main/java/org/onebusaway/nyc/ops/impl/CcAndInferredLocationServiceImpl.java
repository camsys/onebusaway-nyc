package org.onebusaway.nyc.ops.impl;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import org.onebusaway.nyc.ops.services.CcAndInferredLocationService;
import org.onebusaway.nyc.report.api.json.JsonTool;
import org.onebusaway.nyc.report.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.ops.util.OpsApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.reflect.TypeToken;

@Component
public class CcAndInferredLocationServiceImpl implements CcAndInferredLocationService {
	
	protected static Logger _log = LoggerFactory.getLogger(CcAndInferredLocationServiceImpl.class);
		
	@Autowired
	private OpsApiLibrary _apiLibrary;
	
	@Autowired
	private JsonTool _jsonTool;

	public List<CcAndInferredLocationRecord> getAllLastKnownRecords() throws Exception {
		String json = _apiLibrary.getItemsForRequestAsString("last-known", "list");
		StringReader reader = new StringReader(json);
		Type collectionType = new TypeToken<Collection<CcAndInferredLocationRecord>>(){}.getType();
		return _jsonTool.readJson(reader, collectionType);
	}
	
	public OpsApiLibrary get_apiLibrary() {
		return _apiLibrary;
	}

	public void set_apiLibrary(OpsApiLibrary _apiLibrary) {
		this._apiLibrary = _apiLibrary;
	}

	public JsonTool get_jsonTool() {
		return _jsonTool;
	}

	public void set_jsonTool(JsonTool _jsonTool) {
		this._jsonTool = _jsonTool;
	}
}