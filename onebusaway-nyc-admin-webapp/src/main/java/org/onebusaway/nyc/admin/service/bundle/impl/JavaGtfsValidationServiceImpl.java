package org.onebusaway.nyc.admin.service.bundle.impl;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.onebusaway.nyc.admin.service.bundle.GtfsValidationService;
import org.onebusaway.nyc.admin.service.bundle.task.ProcessLoggerTask;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaGtfsValidationServiceImpl implements GtfsValidationService {
	
	private static Logger _log = LoggerFactory.getLogger(JavaGtfsValidationServiceImpl.class);
	public static final String GTFS_VALIDATOR_VERSION = "0.1.3";
	public static final String GTFS_VALIDATOR_NAME = "gtfs-validator";
	public static final String GTFS_VALIDATOR_EXTENSION = ".jar";
	public static final String OUTPUT_FILE_EXTENSION = ".txt";	 
	
	@Override
	public void downloadFeedValidator() {
	    String tmpDir = System.getProperty("java.io.tmpdir");
	    FileUtils fs = new FileUtils(tmpDir);
	    String url = "https://github.com/laidig/gtfs-validator/releases/download/" 
	    			+ GTFS_VALIDATOR_VERSION 
	    			+ "/" 
	    			+ GTFS_VALIDATOR_NAME 
	    			+ GTFS_VALIDATOR_EXTENSION;
	    fs.wget(url);
	}
	
	@Override
	public int validateGtfs(String gtfsZipFileName, String outputFile) {
	    Process process = null;
	    Future<?> future = null;
	    ExecutorService service = Executors.newFixedThreadPool(1);
	    try {
		  String tmpDir = System.getProperty("java.io.tmpdir") + File.separator;
	      
		  String[] cmds = {
	        "java",
	        "-jar",
	    	tmpDir + GTFS_VALIDATOR_NAME + GTFS_VALIDATOR_EXTENSION,
	    	gtfsZipFileName
	      };
	     
	      debugCmds(cmds);
	      ProcessBuilder pb = new ProcessBuilder(cmds).redirectErrorStream(true);
	      process = pb.start();
	      future =  service.submit(new ProcessLoggerTask(process.getInputStream(), outputFile));
	      future.get(5, TimeUnit.MINUTES);
	      return process.waitFor();
	    
	    } catch (Exception e) {
	      _log.error(e.toString());
	      String msg = e.getMessage();
	      if (msg != null && e.getMessage().indexOf("error=2,") > 0) {
	        return 2; // File Not Found
	      }
	      _log.error(e.toString());
	      throw new RuntimeException(e);
	    } finally{
	    	if(future != null){
	    		future.cancel(true);
	    	}
	    }
	    
	}
	
	@Override
	public String getOutputExtension(){
		return OUTPUT_FILE_EXTENSION;
	}
	
	private void debugCmds(String[] array) {
	    FileUtils.debugCmds(array);
	}
}
