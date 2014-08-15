package org.onebusaway.nyc.admin.service.bundle.task;

import org.onebusaway.nyc.admin.service.DiffService;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DiffTask implements Runnable {
	Logger _log = LoggerFactory.getLogger(DiffTask.class);
	protected DiffService diffService;
	MultiCSVLogger logger;
	
	@Autowired
	public void setDiffService(DiffService diffService) {
		this.diffService = diffService;
	}
	
	@Autowired
	public void setLogger(MultiCSVLogger logger) {
		this.logger = logger;
	}
	
	protected String _filename1;
	protected String _filename2;
	protected String _output;

	public void run() {
		logger.difflogHeader(_output);
		try {
			diffService.diff(_filename1, _filename2);
		} catch (Exception e) {
			_log.error("diff failed:", e);
		}
		_log.info("exiting difftask");
	}
	
	void log(int lineNum, String line){
		if (_output != null)
			logger.difflog(lineNum, line);
	}
}