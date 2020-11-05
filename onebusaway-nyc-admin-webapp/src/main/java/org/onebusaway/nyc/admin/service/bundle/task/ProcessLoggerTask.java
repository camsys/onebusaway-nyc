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

package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessLoggerTask implements Runnable{
	private static Logger _log = LoggerFactory.getLogger(ProcessLoggerTask.class);
	private InputStream _input;
	private Writer _writer;
    
	public ProcessLoggerTask(InputStream in, String outputFile) throws FileNotFoundException {
    	_input = in;
		_writer = new PrintWriter(outputFile);
	}
  	
  	public void run() {
  		try {
			IOUtils.copy(_input,_writer);
			_writer.close();
		} catch (IOException e) {
			_log.error("Error occurred when attempting to copy validation process output to file: ", e);
		}
  		_log.warn("ProcessLogger thread is being closed");
  	}
  }