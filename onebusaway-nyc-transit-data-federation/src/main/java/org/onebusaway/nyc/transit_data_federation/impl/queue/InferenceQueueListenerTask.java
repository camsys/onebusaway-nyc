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
package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.queue.QueueListenerTask;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public abstract class InferenceQueueListenerTask extends QueueListenerTask {

	protected abstract void processResult(NycQueuedInferredLocationBean inferredResult, String contents);

	@Override
	public boolean processMessage(String address, byte[] buff) {
	  String contents = new String(buff);
		try {
			if (address == null || !address.equals(getQueueName())) {
				return false;
			}

			
			NycQueuedInferredLocationBean inferredResult = _mapper.readValue(contents, NycQueuedInferredLocationBean.class);
			processResult(inferredResult, contents);
			
			return true;
		} catch (Exception e) {
			_log.warn("Received corrupted message from queue; discarding: " + e.getMessage(), e);
			_log.warn("Contents=" + contents);
			return false;
		}
	}

	@Override
	public String getQueueHost() {
		return _configurationService.getConfigurationValueAsString("tds.inputQueueHost", null);
	}

	@Override
	public String getQueueName() {
		return _configurationService.getConfigurationValueAsString("tds.inputQueueName", null);
	}

	@Override
	public Integer getQueuePort() {
		return _configurationService.getConfigurationValueAsInteger("tds.inputQueuePort", 5564);
	}

	@SuppressWarnings("deprecation")
	@PostConstruct
	public void setup() {
		super.setup();

		// use JAXB annotations so that we pick up anything from the
		// auto-generated XML classes
		// generated from XSDs
		_mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(
				_mapper.getTypeFactory()));
	}

	@PreDestroy
	public void destroy() {
		super.destroy();
	}

	@Refreshable(dependsOn = { "tds.inputQueueHost", "tds.inputQueuePort", "tds.inputQueueName" })
	public void startListenerThread() {
		if (_initialized == true) {
			_log.warn("Configuration service tried to reconfigure TDS input queue reader; this service is not reconfigurable once started.");
			return;
		}

		String host = getQueueHost();
		String queueName = getQueueName();
		Integer port = getQueuePort();

		if (host == null) {
			_log.info("TDS input queue is not attached; input hostname was not available via configuration service.");
			return;
		}

		_log.info("queue listening on " + host + ":" + port + ", queue=" + queueName);

		try {
			initializeQueue(host, queueName, port);
		} catch (InterruptedException ie) {
			return;
		}

		_initialized = true;
	}

}
