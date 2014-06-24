package org.onebusaway.nyc.admin.service.bundle.task;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.FeatureEvent.Type;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import difflib.*;
import difflib.Delta.TYPE;

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
	protected int context = 0;

	public abstract void initFilename();

	@Override
	public void run() {
		initFilename();
		try {
			diff();
		} catch (Exception e) {
			_log.error("diff failed:", e);
		}
		_log.info("exiting difftask");
	}

	protected List<String> diff() throws Exception {
		_log.info("Called diff " + this.getClass().getName() +" between (" + _filename1 + ") and (" + _filename2 + ")");
		List<String> original = fileToLines(_filename1);
		List<String> revised  = fileToLines(_filename2);
		if (_diff_log_filename!=null){
			logger.difflogHeader(_diff_log_filename);
		}
		List<String> unifiedDiff = DiffUtils.generateUnifiedDiff(
				_filename1, _filename2, fileToLines(_filename1), DiffUtils.diff(original, revised), context);
		return transform(unifiedDiff);
	}
	
	abstract List<String> transform(List<String> preTransform);
	
	protected List<String> fileToLines(String filename) {
		List<String> lines = new LinkedList<String>();
		if (filename == null) return lines;
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
	
	void log(int lineNum, String line){
		if (_diff_log_filename != null)
			logger.difflog(lineNum, line);
	}
}