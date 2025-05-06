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

package org.onebusaway.nyc.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

/**
 * Represents an interface to simply listen to queue operations. This
 * is not attempting to be JMS. This is merely allowing us to
 */
public interface IQueueListenerTask {

	ExecutorService _executorService = null;
	ObjectMapper _mapper = new ObjectMapper().registerModule(new JaxbAnnotationModule());
	Logger _log = LoggerFactory.getLogger(IQueueListenerTask.class);

	Properties properties = new Properties();

	void initializeQueue(String host, String queueName,
						 Integer port) throws InterruptedException;

	boolean processMessage(String address, byte[] buff) throws Exception;

	void startListenerThread();

	String getQueueHost();

	String getQueueName();

	/**
	 * Return the name of the queue for display of statistics in logs.
	 */
	String getQueueDisplayName();

	Integer getQueuePort();

	void startDNSCheckThread();

	void destroy();

	void setup();

}