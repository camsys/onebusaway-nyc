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