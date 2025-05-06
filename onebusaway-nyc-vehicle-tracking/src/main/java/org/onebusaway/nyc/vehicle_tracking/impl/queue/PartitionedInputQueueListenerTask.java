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

import org.onebusaway.nyc.queue.IQueueListenerTask;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.ServletContextAware;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

/**
 * Listen for vehicles assigned to one or more depots and inject them into the inference
 * process
 * 
 * @author jmaki
 *
 */
public class PartitionedInputQueueListenerTask
        implements PartitionedInputQueueListener, ServletContextAware, IQueueListenerTask {
  
  private String _depotPartitionKey;

  InputService _inputService;

  @Autowired
  @Qualifier("listener")
  IQueueListenerTask _queueListenerTask;

  @Override
  public void setServletContext(ServletContext servletContext) {
    // check for depot partition keys in the servlet context
    if (servletContext != null) {
    	setDepotPartitionKey(servletContext.getInitParameter("depot.partition.key"));
      _log.info("servlet context provied depot.partition.key=" + _depotPartitionKey);
    }
  }
  
  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {
	  _depotPartitionKey = depotPartitionKey;
  }
  
  @Override
  public String getDepotPartitionKey() {
    return _depotPartitionKey;
  }

  @Override
  public void initializeQueue(String host, String queueName, Integer port) throws InterruptedException {

  }

  @Override
  public boolean processMessage(String address, byte[] buff) throws Exception{
	  return _inputService.processMessage(address, buff);
  }

  @Override
  public void startListenerThread() {}

  @Override
  public String getQueueHost() {return null;}

  @Override
  public String getQueueName() {return null;}

  @Override
  public String getQueueDisplayName() {
    return "PartitionedInputQueueListenerTask";
  }

  @Override
  public Integer getQueuePort() {return null;}

  @Override
  public void startDNSCheckThread() {}

  @Override
  @PostConstruct
  public void setup() {
	_inputService.setDepotPartitionKey(_depotPartitionKey);
    _queueListenerTask.setup();
  }

  @Override
  @PreDestroy
  public void destroy() {
    _inputService.setDepotPartitionKey(_depotPartitionKey);
    _queueListenerTask.destroy();
  }



}
