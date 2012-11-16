package org.onebusaway.nyc.util.git;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to load properties from git-commit-id plugin. 
 *
 */
public class GitRepositoryHelper {

	private static Logger _log = LoggerFactory.getLogger(GitRepositoryHelper.class);
	public Properties getProperties() 	    {
		Properties properties = new Properties();
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("git.properties");
			if (inputStream != null) {
				properties.load(inputStream);
			} else {
				_log.error("git.properties file not found");
			}
		} catch (IOException ioe) {
		    _log.error("properties file not found:", ioe);
		}
		try {
		    return properties;
		} catch (Exception any) {
		    _log.error("exception creating properties:", any);
		    return null;
		}
	    }

	public GitRepositoryState getGitRepositoryState() {
		Properties properties = new Properties();
		try {
		    properties.load(getClass().getClassLoader().getResourceAsStream("git.properties"));
		} catch (IOException ioe) {
		    _log.error("properties file not found:", ioe);
		}
		try {
		    return new GitRepositoryState(properties);
		} catch (Exception any) {
		    _log.error("exception creating properties:", any);
		    return null;
		}
	}

}
