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

package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("fileInputService")
public class FileInputServiceImpl extends InputServiceImpl implements
		InputService {

	@Override
	public String replaceMessageContents(String contents) {
		if(StringUtils.isNotBlank(contents)){
			contents = addSurroundingBrackets(contents);
			contents = replaceFirstStringOccurances(contents);
			contents = replaceAllStringOccurances(contents);
		}
		return contents;	
	}
	
	private String addSurroundingBrackets(String contents){
		if(contents.charAt(0) != '{')
			 contents = "{" + contents + "}";	
		return contents;
	}
	
	private String replaceFirstStringOccurances(String contents){
		return contents.replaceFirst("UUID.*UUID", "UUID");
	}
	
	private String replaceAllStringOccurances(String contents){
		String[] searchList = new String[] { "vehiclepowerstate" };
		String[] replacementList = new String[] { "vehiclePowerState" };
		return StringUtils.replaceEach(contents, searchList, replacementList);
	}
	
	@Override
	@PostConstruct
	public void setup() {
		super.setup();
	}

}
