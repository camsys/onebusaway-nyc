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
package org.onebusaway.nyc.transit_data_federation.impl.queue;

import com.fasterxml.jackson.databind.ObjectReader;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.queue.IQueueListenerTask;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.onebusaway.nyc.transit_data_federation.impl.queue.interfaces.InferenceQueueListenerInterface;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class InferenceQueueListenerTask implements InferenceQueueListenerInterface, IQueueListenerTask {

	protected void processResult(NycQueuedInferredLocationBean inferredResult, String contents){};
	protected ObjectReader _reader;

	protected boolean _initialized = false;

	@Autowired
	@Qualifier("configurationService")
	ConfigurationService _configurationService;

	@Autowired
	@Qualifier("listener")
	IQueueListenerTask _queueListenerTask;

	public InferenceQueueListenerTask() {
		_reader = _mapper.reader(NycQueuedInferredLocationBean.class);
	}

	@Override
	public void initializeQueue(String host, String queueName, Integer port) throws InterruptedException {

	}

	@Override
	public boolean processMessage(String address, byte[] buff) {
	  String contents = null;
		try {
			contents= new String(buff);
			if (address == null || !address.equals(getQueueName())) {
				return false;
			}

			// jackson advice:  prefer reader over mapper
			NycQueuedInferredLocationBean inferredResult = _reader.readValue(contents);
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
	public String getQueueDisplayName() {
		return null;
	}

	@Override
	public Integer getQueuePort() {
		return _configurationService.getConfigurationValueAsInteger("tds.inputQueuePort", 5564);
	}

	@Override
	public void startDNSCheckThread() {

	}

	@SuppressWarnings("deprecation")
	@PostConstruct
	public void setup() {

		_queueListenerTask.setup();

		// use JAXB annotations so that we pick up anything from the
		// auto-generated XML classes
		// generated from XSDs
		AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
		_mapper.setAnnotationIntrospector(jaxb);

	}

	@PreDestroy
	public void destroy() {
		_queueListenerTask.destroy();

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
			_queueListenerTask.initializeQueue(host, queueName, port);;
		} catch (InterruptedException ie) {
			return;
		}
		_initialized = true;
	}

}
