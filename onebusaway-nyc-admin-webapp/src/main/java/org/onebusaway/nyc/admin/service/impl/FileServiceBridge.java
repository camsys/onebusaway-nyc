package org.onebusaway.nyc.admin.service.impl;

import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.onebusaway.nyc.admin.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

public class FileServiceBridge implements FileService, ServletContextAware {

	private static Logger _log = LoggerFactory.getLogger(FileServiceBridge.class);
	private FileService _s3;
	private FileService _disk;
	private String _user;
	private String _password;
	private String _bucketName;
	private boolean _isS3 = false;
	private String _gtfsPath;
	private String _stifPath;
	private String _buildPath;
	private String _configPath;
	
	
	@PostConstruct
	@Override
	public void setup() {
		_log.info("setup with _isS3=" + _isS3 + " and bucketName=" + _bucketName);
		if (_isS3) {
			_s3 = new S3FileServiceImpl();
			_s3.setUser(_user);
			_s3.setPassword(_password);
			_s3.setBucketName(_bucketName);
			_s3.setGtfsPath(_gtfsPath);
			_s3.setStifPath(_stifPath);
			_s3.setBuildPath(_buildPath);
			_s3.setConfigPath(_configPath);
			_s3.setup();
		} else {
			_disk = new DiskFileServiceImpl();
			_disk.setBucketName(_bucketName);
			_disk.setGtfsPath(_gtfsPath);
			_disk.setStifPath(_stifPath);
			_disk.setBuildPath(_buildPath);
			_disk.setConfigPath(_configPath);
			_disk.setup();
		}
	}

	@Override
	public void setUser(String user) {
		_user = user;
	}

	@Override
	public void setPassword(String password) {
		_password = password;
	}

	@Override
	public void setBucketName(String bucketName) {
		_bucketName = bucketName;
	}

	@Override
	public void setGtfsPath(String gtfsPath) {
		_gtfsPath = gtfsPath;
	}

	@Override
	public String getGtfsPath() {
		return _gtfsPath;
	}

	@Override
	public void setStifPath(String stifPath) {
		_stifPath = stifPath;
	}

	@Override
	public String getStifPath() {
		return _stifPath;
	}

	@Override
	public void setBuildPath(String buildPath) {
		_buildPath = buildPath;
	}

	@Override
	public String getConfigPath() {
		return _configPath;
	}

	@Override
	public void setConfigPath(String configPath) {
		_configPath = configPath;
	}

	@Override
	public String getBuildPath() {
		return _buildPath;
	}

	@Override
	public String getBucketName() {
		return _bucketName;
	}

	@Override
	public boolean bundleDirectoryExists(String filename) {
		if (_isS3) {
			return _s3.bundleDirectoryExists(filename);
		}
		return _disk.bundleDirectoryExists(filename);
	}

	@Override
	public boolean createBundleDirectory(String filename) {
		if (_isS3) {
			return _s3.createBundleDirectory(filename);
		}
		return _disk.createBundleDirectory(filename);
	}

	@Override
	public List<String[]> listBundleDirectories(int maxResults) {
		if (_isS3) {
			return _s3.listBundleDirectories(maxResults);
		}
		return _disk.listBundleDirectories(maxResults);
	}

	@Override
	public String get(String basePath, String tmpDir) {
		if (_isS3) {
			return _s3.get(basePath, tmpDir);
		}
		return _disk.get(basePath, tmpDir);
	}

	@Override
	public InputStream get(String s3Path) {
		if (_isS3) {
			return _s3.get(s3Path);
		}
		return _disk.get(s3Path);
	}

	@Override
	public String put(String key, String directory) {
		if (_isS3) {
			return _s3.put(key, directory);
		}
		return _disk.put(key, directory);
	}

	@Override
	public List<String> list(String directory, int maxResults) {
		if (_isS3) {
			return _s3.list(directory, maxResults);
		}
		return _disk.list(directory, maxResults);
	}

	@Override
	public String createOutputFilesZip(String directoryName) {
		if (_isS3) {
			return _s3.createOutputFilesZip(directoryName);
		}
		return _disk.createOutputFilesZip(directoryName);
	}

	@Override
	public void validateFileName(String fileName) {
		if (_isS3) {
			_s3.validateFileName(fileName);
		} else {
			_disk.validateFileName(fileName);
		}
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		_log.info("setServletContext called");
		if (servletContext != null) {
			String user = servletContext.getInitParameter("s3.user");
			_log.info("servlet context provided s3.user=" + user);
			if (user != null) {
				_isS3 = true;
				setUser(user);
			}
			String password = servletContext.getInitParameter("s3.password");
			if (password != null) {
				_isS3 = true;
				setPassword(password);
			}
			String bucketName = servletContext.getInitParameter("s3.bundle.bucketName");
			if (bucketName != null) {
			  _log.info("servlet context provided bucketName=" + bucketName);
			  setBucketName(bucketName);
			} else {
				bucketName = servletContext.getInitParameter("file.bundle.bucketName");
				if (bucketName != null) {
					_log.info("servlet context provided bucketName=" + bucketName);
					setBucketName(bucketName);
				} else {
					_log.info("servlet context missing bucketName, using " + getBucketName());
				}
			}

		}
	}

}
