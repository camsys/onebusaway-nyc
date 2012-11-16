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
package org.onebusaway.nyc.webapp.actions.api;


import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.codehaus.jackson.map.ObjectMapper;

import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;

/**
 * Action for release (status)  page
 * 
 */
public class ReleaseAction extends OneBusAwayNYCActionSupport {

  private static Logger _log = LoggerFactory.getLogger(ReleaseAction.class);
  private static final long serialVersionUID = 1L;
  private GitRepositoryState gitRepositoryState;
  private ObjectMapper _mapper = new ObjectMapper();    

  public GitRepositoryState getGitRepositoryState() {
	if (gitRepositoryState == null)
		gitRepositoryState = new GitRepositoryHelper().getGitRepositoryState();
	return gitRepositoryState;
  }

  public String getCommitId() {
      if (getGitRepositoryState() != null) {
	  return getGitRepositoryState().getCommitId();
      }
      return "unknown";
  }

  public String getDetails() throws IOException {
      if (getGitRepositoryState() != null) {
	  return _mapper.writeValueAsString(getGitRepositoryState());
      }
      return "unknown";
  }
}