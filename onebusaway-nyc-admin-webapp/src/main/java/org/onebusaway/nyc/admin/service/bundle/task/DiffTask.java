package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import difflib.*;

public abstract class DiffTask implements Runnable {
	protected Logger _log = LoggerFactory.getLogger(DiffTask.class);
	
	@Autowired
	private MultiCSVLogger logger;
	
	@Autowired
	public void setLogger(MultiCSVLogger logger) {
		this.logger = logger;
	}

	protected String _diff_log_filename;
	protected String _filename1;
	protected String _filename2;
	
	public abstract void initFilename();
	
	@Override
	public void run() {
		initFilename();
		_log.info("Starting DiffTask between (" + _filename1 + ") and (" + _filename2 + ")");
		try {
      diff();
    } catch (Exception e) {
      _log.error("diff failed:", e);
    }
		_log.info("exiting difftask");
	}

	protected void diff() throws Exception {
	   logger.changelogHeader();
     List<String> original = fileToLines(_filename1);
     List<String> revised  = fileToLines(_filename2);
     Patch patch = DiffUtils.diff(original, revised);
     logger.difflogHeader(_diff_log_filename);
     
     int offset;
     for (Delta delta: patch.getDeltas()) {
       offset=0;
       for (Object line: delta.getOriginal().getLines()){
         logger.difflog((delta.getOriginal().getPosition()+offset),"-" + line);
         offset++;
       }
       offset=0;
       for (Object line: delta.getRevised().getLines()){
         logger.difflog((delta.getRevised().getPosition()+offset),"+" + line);
         offset++;
       }
     }
	}
	private List<String> fileToLines(String filename) {
		List<String> lines = new LinkedList<String>();
		String line = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}
}