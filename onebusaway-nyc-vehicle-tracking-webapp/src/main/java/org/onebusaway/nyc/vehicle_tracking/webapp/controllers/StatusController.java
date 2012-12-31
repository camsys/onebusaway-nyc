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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.onebusaway.nyc.vehicle_tracking.impl.queue.PartitionedInputQueueListenerTask;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
/**
 * Controller for git status. 
 *
 */
@Controller
public class StatusController {


	private GitRepositoryState gitState = null;
	
	@Autowired
	private OutputQueueSenderService queueSenderService;
	
	@Autowired
	private PartitionedInputQueueListenerTask queueListener;
	
	
  @RequestMapping(value="/status.do", method=RequestMethod.GET)
  public ModelAndView index() {
	  if (gitState == null) {
		  gitState = new GitRepositoryHelper().getGitRepositoryState();
	  }
	  
	  StatusModel status = new StatusModel();
	  status.setOutputService(queueSenderService.getClass().getName());
	  status.setHostname(queueSenderService.getPrimaryHostname());
	  status.setPrimary(queueSenderService.getIsPrimaryInferenceInstance());
	  status.setListenerTask(queueListener.getClass().getName());
	  status.setDepotList(queueListener.getDepotPartitionKey());
	  status.setGitDescribe(gitState.getDescribe());
	  return new ModelAndView("status.jspx", "sm", status);
  }

  public static class StatusModel {

	private String hostname;
    private boolean isPrimary;
	private String depotList;
	private String outputService;
	private String listenerTask;
	private String gitDescribe;

    public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public boolean isPrimary() {
		return isPrimary;
	}
	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	public boolean getPrimary() {
		return isPrimary;
	}
	public String getDepotList() {
		return depotList;
	}
	public void setDepotList(String depotList) {
		this.depotList = depotList;
	}
	public String getOutputService() {
		return outputService;
	}
	public void setOutputService(String outputService) {
		this.outputService = outputService;
	}
	public String getListenerTask() {
		return listenerTask;
	}
	public void setListenerTask(String listenerTask) {
		this.listenerTask = listenerTask;
	}
	public String getGitDescribe() {
		return gitDescribe;
	}
	public void setGitDescribe(String gitDescribe) {
		this.gitDescribe = gitDescribe;
	}

	
	public String toString() {
		return "Status:{"
				+ "listenerTask=" + this.listenerTask
				+ ", outputService=" + this.outputService
				+ ", hostname=" + this.hostname
				+ ", depotList=" + this.depotList + "}";
	}
  }
  

}
