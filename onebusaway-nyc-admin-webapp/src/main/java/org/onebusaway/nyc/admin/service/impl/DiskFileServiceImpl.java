package org.onebusaway.nyc.admin.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskFileServiceImpl implements FileService {

	private static Logger _log = LoggerFactory.getLogger(DiskFileServiceImpl.class);
	private String _basePath;
	private String _gtfsPath;
	private String _stifPath;
	private String _buildPath;
	private String _configPath;
	@Override
	public void setup() {
		_log.info("DiskFileServiceImpl setup");
	}

	@Override
	public void setUser(String user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPassword(String password) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBucketName(String bucketName) {
		_basePath = bucketName;
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
		return _basePath;
	}

	@Override
	public boolean bundleDirectoryExists(String filename) {
		File f = new File(_basePath, filename);
		return f.exists();
	}

	@Override
	public boolean createBundleDirectory(String filename) {
		File f = new File(_basePath, filename);
		return f.mkdirs();
	}

	@Override
	public List<String[]> listBundleDirectories(int maxResults) {
		ArrayList<String[]> bundleDirs = new ArrayList<String[]>();
		File baseDir = new File(_basePath);
		String[] list  = baseDir.list();
		_log.info("empty list for bundleDirectories at basepath=" + _basePath);
		if (list == null) return bundleDirs;
		// need filename/flag/modified date
		for (String dir: list) {
			File fDir = new File(baseDir, dir);
			String[] a = {dir, " ", new Date(fDir.lastModified()).toString()};
			bundleDirs.add(a);
		}
		return bundleDirs;
	}

	@Override
	public String get(String s3path, String tmpDir) {
		File srcFile = new File(_basePath, s3path);
		FileUtils fs = new FileUtils();
		File destFile = new File(tmpDir, fs.parseFileName(s3path));
		FileUtils.copyFile(srcFile, destFile);
		return null;
	}

	@Override
	public InputStream get(String s3Path) {
		try {

		File f = new File(_basePath, s3Path);
		FileInputStream in = new FileInputStream(f);
		return in;
		} catch (FileNotFoundException e) {
			_log.error("get failed(" + s3Path + "):", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String put(String key, String directory) {
		// TODO
		_log.error("empty put(" + key + ", " + directory + "):  please implement");
		return null;
	}

	@Override
	public List<String> list(String directory, int maxResults) {
		File dir = new File(_basePath, directory);
		return Arrays.asList(dir.list());
	}

	@Override
	public String createOutputFilesZip(String directoryName) {
		// TODO
		_log.error("empty createOutputFileZip(" + directoryName + "):  please implement");
		return null;
	}

	@Override
	public void validateFileName(String fileName) {
		if(fileName.length() == 0) {
			throw new RuntimeException("File name contains characters that could lead to directory " +
					"traversal attack");
		}
		if(new File(fileName).isAbsolute()) {
			throw new RuntimeException("File name contains characters that could lead to directory " +
					"traversal attack");
		}
		if(fileName.contains("../") || fileName.contains("./")) {
			throw new RuntimeException("File name contains characters that could lead to directory " +
					"traversal attack");
		}
	}

}
