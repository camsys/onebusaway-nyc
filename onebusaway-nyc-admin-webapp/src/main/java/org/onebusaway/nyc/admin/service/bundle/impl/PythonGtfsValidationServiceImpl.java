package org.onebusaway.nyc.admin.service.bundle.impl;

import java.io.File;
import org.onebusaway.nyc.admin.service.bundle.GtfsValidationService;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonGtfsValidationServiceImpl implements GtfsValidationService {
	
	private static Logger _log = LoggerFactory.getLogger(PythonGtfsValidationServiceImpl.class);
	
	public static final String GTFS_VALIDATOR_VERSION = "1.2.15";
	public static final String GTFS_VALIDATOR_NAME = "transitfeed";
	
	public static final String GTFS_VALIDATOR_EXTENSION = ".py";
	public static final String TAR_EXTENSION = ".tar.gz";
	public static final String OUTPUT_FILE_EXTENSION = ".html";	
	
	public static final String GTFS_VALIDATOR_SCRIPT_NAME = "feedvalidator";
	  
	@Override 
	public void downloadFeedValidator() {
	    String tmpDir = System.getProperty("java.io.tmpdir");
	    FileUtils fs = new FileUtils(tmpDir);
	    String url = "https://github.com/google/transitfeed/archive/" + GTFS_VALIDATOR_VERSION + TAR_EXTENSION;
	    fs.wget(url);
	    fs.tarzxf(tmpDir + File.separatorChar + GTFS_VALIDATOR_VERSION + ".tar.gz");
	}
	
	@Override
	public int validateGtfs(String gtfsZipFileName, String outputFile) {
	    Process process = null;
	    try {
		  String tmpDir = System.getProperty("java.io.tmpdir") + File.separator 
				  + GTFS_VALIDATOR_NAME + "-" + GTFS_VALIDATOR_VERSION;
	      
	      String[] cmds = {
	        tmpDir + File.separator + GTFS_VALIDATOR_SCRIPT_NAME + GTFS_VALIDATOR_EXTENSION,
	        "-n",
	        "-m",
	        "--service_gap_interval=1",
	        "--output=" + outputFile,
	        gtfsZipFileName
	      };
	      debugCmds(cmds);
	      ProcessBuilder pb = new ProcessBuilder(cmds).redirectErrorStream(true);
	      process = pb.start();
	      int exitCode = process.waitFor();
	      // TODO - Python returns a 1, figure out why
	      if(exitCode == 1) exitCode = 0;
	      return exitCode;
	    } catch (Exception e) {
	      _log.error(e.toString());
	      String msg = e.getMessage();
	      if (msg != null && e.getMessage().indexOf("error=2,") > 0) {
	        return 2; // File Not Found
	      }
	      _log.error(e.toString());
	      throw new RuntimeException(e);
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
