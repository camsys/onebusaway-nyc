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
package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
/**
 * Controller for git status. 
 *
 */
@Controller
public class ReleaseController {

	ObjectMapper _mapper = new ObjectMapper();
	private GitRepositoryState gitState = null;

	@RequestMapping(value="/release.do", method=RequestMethod.GET)
	public void gitDetailsInJSON(HttpServletResponse response) throws IOException {
		if (gitState == null) {
			gitState = new GitRepositoryHelper().getGitRepositoryState();
		}
		OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
		_mapper.writeValue(writer, gitState);
	}

}
