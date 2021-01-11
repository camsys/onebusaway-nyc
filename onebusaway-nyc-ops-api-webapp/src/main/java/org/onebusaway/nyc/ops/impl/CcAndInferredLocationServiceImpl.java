/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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