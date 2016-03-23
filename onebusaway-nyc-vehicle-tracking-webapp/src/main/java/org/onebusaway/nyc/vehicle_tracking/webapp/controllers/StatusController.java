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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputTask;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
/**
 * Controller for git status. 
 *
 */
@Controller
public class StatusController {

	private static final String INSTANCE_ID_URL = "http://169.254.169.254/latest/meta-data/instance-id";
	private static final String INSTANCE_TYPE_URL = "http://169.254.169.254/latest/meta-data/instance-type";
	private static final String AMI_ID_URL = "http://169.254.169.254/latest/meta-data/ami-id";
	private static final String PUBLIC_HOSTNAME_URL = "http://169.254.169.254/latest/meta-data/public-hostname";
	private static final String INTERNAL_HOSTNAME_URL = "http://169.254.169.254/latest/meta-data/hostname";

	private ObjectMapper _mapper = new ObjectMapper();
	private GitRepositoryState gitState = null;
	
	@Autowired
	private OutputQueueSenderService queueSenderService;
	
	@Autowired
	private InputTask inputTask;
	
	
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
	  status.setListenerTask(inputTask.getClass().getName());
	  status.setDepotList(inputTask.getDepotPartitionKey());
	  status.setGitDescribe(gitState.getDescribe());
	  status.setInstanceId(slurp(INSTANCE_ID_URL));
	  status.setInstanceType(slurp(INSTANCE_TYPE_URL));
	  status.setAmiId(slurp(AMI_ID_URL));
	  status.setPublicHostname(slurp(PUBLIC_HOSTNAME_URL));
	  status.setInternalHostname(slurp(INTERNAL_HOSTNAME_URL));
	  return status;
  }
  
  private String slurp(String urlString) {
    URL url;
    InputStream is = null;
    BufferedInputStream bis = null;
    ByteArrayOutputStream baos = null;

    try {
      url = new URL(urlString);
      is = url.openStream();
      bis = new BufferedInputStream(is);
      baos = new ByteArrayOutputStream();
      IOUtils.copy(bis, baos);
    } catch (Exception any) {
      return any.toString();
    } finally {
      if (bis != null)
        try {
          bis.close();
        } catch (Exception e1) {
        }
      if (baos != null)
        try {
          baos.close();
        } catch (Exception e2) {
        }
    }
    return baos.toString();
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
