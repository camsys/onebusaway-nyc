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
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.onebusaway.nyc.vehicle_tracking.impl.queue.InputQueueListenerTask;
import org.onebusaway.nyc.vehicle_tracking.services.aws.AwsMetadataService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
/**
 * Controller for git status. 
 *
 */
@Controller
public class StatusController {

	private ObjectMapper _mapper = new ObjectMapper();
	private GitRepositoryState gitState = null;
	
	@Autowired
	private OutputQueueSenderService queueSenderService;
	
	@Autowired
	private InputQueueListenerTask queueListener;
	
	@Autowired
	private AwsMetadataService awsMetadataService;
	
	
  @RequestMapping(value="/status.do", method=RequestMethod.GET)
  public ModelAndView index() {
	  return new ModelAndView("status.jspx", "sm", getStatus());
  }

 @RequestMapping(value="/status-json.do", method=RequestMethod.GET)
  public void json(HttpServletResponse response) throws IOException {
	  OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
	  _mapper.writeValue(writer, getStatus());
  }

  private StatusModel getStatus() {
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
	  status.setInstanceId(awsMetadataService.getInstanceId());
	  status.setInstanceType(awsMetadataService.getInstanceType());
	  status.setAmiId(awsMetadataService.getAmiId());
	  status.setPublicHostname(awsMetadataService.getPublicHostname());
	  status.setInternalHostname(awsMetadataService.getInternalHostname());
	  return status;
  }
  
  public static class StatusModel {

	private String hostname;
    private boolean isPrimary;
	private String depotList;
	private String outputService;
	private String listenerTask;
	private String gitDescribe;
	private String instanceId;
	private String instanceType;
	private String amiId;
	private String publicHostname;
	private String internalHostname;

    public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
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
	public String getInstanceId() {
		return instanceId;
	}
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	public String getInstanceType() {
		return instanceType;
	}
	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}
	public String getAmiId() {
		return amiId;
	}
	public void setAmiId(String amiId) {
		this.amiId = amiId;
	}
	public String getPublicHostname() {
		return publicHostname;
	}
	public void setPublicHostname(String publicHostname) {
		this.publicHostname = publicHostname;
	}
	public String getInternalHostname() {
		return internalHostname;
	}
	public void setInternalHostname(String internalHostname) {
		this.internalHostname = internalHostname;
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
