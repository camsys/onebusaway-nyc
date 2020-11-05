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

package org.onebusaway.nyc.admin.service.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.*;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.admin.util.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

/**
 * Implements File operations over Amazon S3.
 * 
 */
public class FileServiceImpl implements FileService, ServletContextAware {

	private static Logger _log = LoggerFactory.getLogger(FileServiceImpl.class);
	private static int MAX_RESULTS = -1;

	private AWSCredentials _credentials;
	private AmazonS3Client _s3;

	private String _bucketName;

	// the gtfs directory relative to the bundle directory; e.g. gtfs_latest
	private String _gtfsPath;

	// the stif directory relative to the bundle directory; e.g. stif_latest
	private String _stifPath;
	// the transformation directory relative to the bundle directory; e.g. transformations_latest
	private String _transformationPath;
	// the config directory, relative to the bundle directory; e.g., config
	private String _configPath;
	private String _buildPath;
	private String _username;
	private String _password;

	@Override
	public void setS3User(String user) {
		_username = user;
	}

	@Override
	public void setS3Password(String password) {
		_password = password;
	}

	@Override
	public void setBucketName(String bucketName) {
		this._bucketName = bucketName;
	}

	@Override
	public String getBucketName() {
		return this._bucketName;
	}

	@Override
	public void setGtfsPath(String gtfsPath) {
		this._gtfsPath = gtfsPath;
	}

	@Override
	public String getGtfsPath() {
		return _gtfsPath;
	}

	@Override
	public void setStifPath(String stifPath) {
		this._stifPath = stifPath;
	}

	@Override
	public String getStifPath() {
		return _stifPath;
	}

	@Override
	public void setTransformationPath(String transformationPath) {
		this._transformationPath = transformationPath;
	}

	@Override
	public String getTransformationPath() {
		return _transformationPath;
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
	public void setBuildPath(String buildPath) {
		this._buildPath = buildPath;
	}

	@Override
	public String getBuildPath() {
		return _buildPath;
	}

	@PostConstruct
	@Override
	public void setup() {
		try {
			_log.info("setting up s3user=" + _username);
			_credentials = new BasicAWSCredentials(_username, _password);
			_s3 = new AmazonS3Client(_credentials);
			_log.info("setup complete");
		} catch (Throwable t) {
			_log.error("FileServiceImpl setup failed, likely due to missing or invalid credentials");
			_log.error(t.toString());
		}

	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (servletContext != null) {
			String user = servletContext.getInitParameter("s3.user");
			_log.info("servlet context provided s3.user=" + user);
			if (user != null) {
				setS3User(user);
			}
			String password = servletContext.getInitParameter("s3.password");
			if (password != null) {
				setS3Password(password);
			}
			String bucketName = servletContext.getInitParameter("s3.bundle.bucketName");
			if (bucketName != null) {
				_log.info("servlet context provided bucketName=" + bucketName);
				setBucketName(bucketName);
			} else {
				_log.info("servlet context missing bucketName, using " + getBucketName());
			}
		}
	}

	@Override
	/**
	 * check to see if the given bundle directory exists in the configured bucket.
	 * Do not include leading slashes in the filename(key).
	 */
	public boolean bundleDirectoryExists(String filename) {
		ListObjectsRequest request = new ListObjectsRequest(_bucketName, filename,
				null, null, 1);
		ObjectListing listing = _s3.listObjects(request);
		return listing.getObjectSummaries().size() > 0;
	}

	@Override
	/**
	 * delete an object from s3
	 */
	public void deleteObject(String filename) {
		try {
			_s3.deleteObject(_bucketName, filename);
		} catch (AmazonClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean createBundleDirectory(String filename) {
		try {
			/*
			 * a file needs to be written for a directory to exist create README file,
			 * which could optionally contain meta-data such as creator, production
			 * mode, etc.
			 */
			File tmpFile = File.createTempFile("README", "txt");
			String contents = "Root of Bundle Build";
			FileWriter fw = new FileWriter(tmpFile);
			fw.append(contents);
			fw.close();
			PutObjectRequest request = new PutObjectRequest(_bucketName, filename
					+ "/README.txt", tmpFile);
			PutObjectResult result = _s3.putObject(request);
			// now create tree structure
			request = new PutObjectRequest(_bucketName, filename + "/" +
					this.getGtfsPath() + "/README.txt", tmpFile);
			result = _s3.putObject(request);
			request = new PutObjectRequest(_bucketName, filename + "/" +
					this.getStifPath() + "/README.txt", tmpFile);
			result = _s3.putObject(request);
			request = new PutObjectRequest(_bucketName, filename + "/" +
					this.getTransformationPath() + "/README.txt", tmpFile);
			result = _s3.putObject(request);
			request = new PutObjectRequest(_bucketName, filename + "/" +
					this.getBuildPath() + "/README.txt", tmpFile);
			result = _s3.putObject(request);
			return result != null;
		} catch (Exception e) {
			_log.error(e.toString(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	/**
	 * Return tabular data (filename, flag, modified date) about bundle directories.
	 */
	public List<String[]> listBundleDirectories(int maxResults) {
		List<String[]> rows = new ArrayList<String[]>();
		HashMap<String, String> map = new HashMap<String, String>();
		ListObjectsRequest request = new ListObjectsRequest(_bucketName, null, null,
				"/", maxResults);

		ObjectListing listing = null;
		do {
			if (listing == null) {
				listing = _s3.listObjects(request);
				if (listing.getCommonPrefixes() != null) {
					// short circuit if common prefixes works
					List<String> commonPrefixes = listing.getCommonPrefixes();
					for (String key : commonPrefixes) {
						Date lastModified = getLastModifiedTimeForKey(key);
						String lastModifiedStr = "n/a";
						if (lastModified != null) {
							lastModifiedStr = "" + lastModified.toString();
						}
						String[] columns = {
								parseKey(key), getStatus(key), lastModifiedStr
						};
						rows.add(columns);
					}
					return rows;
				}
				_log.error("prefixes=" + listing.getCommonPrefixes());
			} else {
				listing = _s3.listNextBatchOfObjects(listing);
			}
			for (S3ObjectSummary summary : listing.getObjectSummaries()) {
				String key = parseKey(summary.getKey());
				if (!map.containsKey(key)) {
					String[] columns = {
							key, " ", "" + summary.getLastModified().getTime()};
					rows.add(columns);
					map.put(key, key);
				}
			}

		} while (listing.isTruncated());
		return rows;
	}


	@Override
	/**
	 * Copies an s3 object from one location to another
	 */
	public void copyS3Object(String fromObjectKey, String toObjectKey) {
		try {
			_s3.copyObject(_bucketName, fromObjectKey, _bucketName, toObjectKey);
		} catch( Exception e){
			throw new RuntimeException(e);
		}
	}






@Override
	/**
	 * Return tabular data (filename, flag, modified date) about bundle directories.
	 */
	public List<String> listBundleBuilds(String directoryName, int maxResults) {
		List<String> rows = new ArrayList<String>();
		HashMap<String, String> map = new HashMap<String, String>();
		ListObjectsRequest request = new ListObjectsRequest(_bucketName, directoryName, null,
				"/", maxResults);

		ObjectListing listing = null;
		do {
			if (listing == null) {
				listing = _s3.listObjects(request);
				if (listing.getCommonPrefixes() != null) {
					// short circuit if common prefixes works
					List<String> commonPrefixes = listing.getCommonPrefixes();
					for (String key : commonPrefixes) {
						rows.add(key);
					}
					return rows;
				}
				_log.error("prefixes=" + listing.getCommonPrefixes());
			} else {
				listing = _s3.listNextBatchOfObjects(listing);
			}
			for (S3ObjectSummary summary : listing.getObjectSummaries()) {
				String key = parseKey(summary.getKey());
				if (!map.containsKey(key)) {
					rows.add(key);
					map.put(key, key);
				}
			}

		} while (listing.isTruncated());
		return rows;
	}

	@Override
	/**
	 * Return files' names at a specified location.
	 */
	public List<String> listFiles(String directoryName, int maxResults) {
		List<String> rows = new ArrayList<String>();
		ListObjectsRequest request = new ListObjectsRequest(_bucketName, directoryName, null,
				"/", maxResults);

		ObjectListing listing = null;
		do {
			if (listing == null) {
			listing = _s3.listObjects(request);
			}
			for (S3ObjectSummary summary : listing.getObjectSummaries()) {
				rows.add(summary.getKey());
			}

		} while (listing.isTruncated());
		return rows;
	}


	@Override
	/**
	 * Return fileName of objects in an S3 file.
	 */
	public List<String> listObjects(String directoryName, int maxResults) {
		List<String> rows = new ArrayList<String>();
		HashMap<String, String> map = new HashMap<String, String>();
		ListObjectsRequest request = new ListObjectsRequest(_bucketName, directoryName, null,
				"/", maxResults);

		ObjectListing listing = null;
		if (listing == null) {
			listing = _s3.listObjects(request);
			if (listing.getObjectSummaries() != null) {
				// short circuit if common prefixes works
				List<S3ObjectSummary> s3objectSummaries = listing.getObjectSummaries();
				for (S3ObjectSummary s3ObjectSummary : s3objectSummaries) {
					String[] keyParts = s3ObjectSummary.getKey().split("/");
					rows.add(keyParts[keyParts.length - 1]);
				}
			}
		}
		return rows;
	}

	private Date getLastModifiedTimeForKey(String key) {
		ListObjectsRequest request = new ListObjectsRequest(_bucketName, key, null,
				"/", 1);
		ObjectListing listing = _s3.listObjects(request);
		if (!listing.getObjectSummaries().isEmpty())
			return listing.getObjectSummaries().get(0).getLastModified();
		return null;
	}

	// TODO return the status (production/experimental) of this directory
	private String getStatus(String key) {
		return " ";
	}

	@Override
	/**
	 * Retrieve the specified key from S3 and store in the given directory.
	 */
	public String get(String key, String tmpDir) {
		_log.debug("get(" + key + ", " + tmpDir + ")");
		FileUtils fs = new FileUtils();
		String filename = fs.parseFileName(key);
		_log.debug("filename=" + filename);
		GetObjectRequest request = new GetObjectRequest(this._bucketName, key);
		S3Object file = _s3.getObject(request);
		String pathAndFileName = tmpDir + File.separator + filename;
		fs.copy(file.getObjectContent(), pathAndFileName);
		return pathAndFileName;
	}

	public InputStream get(String key) {
		GetObjectRequest request = new GetObjectRequest(this._bucketName, key);
		S3Object file = _s3.getObject(request);
		return file.getObjectContent();
	}

	@Override
	/**
	 * push the contents of the directory to S3 at the given key location.
	 */
	public String put(String key, String file) {
		if (new File(file).isDirectory()) {
			File dir = new File(file);
			for (File contents : dir.listFiles()) {
				try {
					put(key, contents.getName(), contents.getCanonicalPath());
				} catch (IOException ioe) {
					_log.error(ioe.toString(), ioe);
				}
			}
			return null;
		}
		PutObjectRequest request = new PutObjectRequest(this._bucketName, key,
				new File(file));
		PutObjectResult result = _s3.putObject(request);
		return result.getVersionId();
	}

	public String put(String prefix, String key, String file) {
		if (new File(file).isDirectory()) {
			File dir = new File(file);
			for (File contents : dir.listFiles()) {
				try {
					put(prefix + "/" + key, contents.getName(),
							contents.getCanonicalPath());
				} catch (IOException ioe) {
					_log.error(ioe.toString(), ioe);
				}
			}
			return null;
		}
		String filename = prefix + "/" + key;
		_log.info("uploading " + file + " to " + filename);
		PutObjectRequest request = new PutObjectRequest(this._bucketName, filename,
				new File(file));
		PutObjectResult result = _s3.putObject(request);
		return result.getVersionId();

	}

	@Override
	/**
	 * list the files in the given directory.
	 */
	public List<String> list(String directory, int maxResults) {
		ListObjectsRequest request = new ListObjectsRequest(_bucketName, directory,
				null, null, maxResults);
		ObjectListing listing = _s3.listObjects(request);
		List<String> rows = new ArrayList<String>();
		for (S3ObjectSummary summary : listing.getObjectSummaries()) {
			// if its a directory at the root level
			if (!summary.getKey().endsWith("/")) {
				rows.add(summary.getKey());
			}
		}
		return rows;
	}

	@Override
	public String createOutputFilesZip(String s3Path) {
	  String directoryName = null;
	  // create tmp dir
	  FileUtils fs = new FileUtils();
	  directoryName = fs.createTmpDirectory();
	  
	  // pull down output directory files to this tmp directory

	  for (String s3File : list(s3Path, MAX_RESULTS)) {
	    get(s3File, directoryName);
	  }
	  
		final String zipFileName = directoryName + File.separator + "output.zip";
		File outputDirectory = new File(directoryName);
		String [] outputFiles = outputDirectory.list();
		//Buffer for reading the files
		byte [] buffer = new byte[1024];
		ZipOutputStream zout = null;
		try {
			zout = new ZipOutputStream(new FileOutputStream(zipFileName));
			for(int i=0; i<outputFiles.length; i++) {
				//Add each file in output directory to the zip
			  String entryName = directoryName + File.separator + outputFiles[i];
				FileInputStream in = new FileInputStream(entryName);
				//Add ZIP entry
				zout.putNextEntry(new ZipEntry(outputFiles[i]));
				int len;
				while((len = in.read(buffer)) > 0) {
					zout.write(buffer, 0, len);
				}
				//Close the zip entry and input stream
				zout.closeEntry();
				in.close();
				
				// clean up after ourselves
				new File(entryName).delete();
			}
			
		} catch(IOException e) {
			e.printStackTrace();
		}
		finally {
			//Close the zip
			try {
				zout.close();
				// finally remove the tmp directory
				new File(directoryName).delete();
			} catch (IOException e) {
			  _log.error("createOutputFileZip failed:", e);
			}
		}
		return zipFileName;
	}

	private String parseKey(String key) {
		if (key == null) return null;
		int pos = key.indexOf("/");
		if (pos == -1) return key;
		return key.substring(0, pos);
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


	/**
	 * Return tabular data (filename, flag, modified date) about objects on S3.
	 */
	public List<Map<String,String>> listObjectsTabular (String directory, int maxResults){

	ListObjectsRequest request = new ListObjectsRequest(_bucketName, directory,
			null, null, maxResults);
	ObjectListing listing = _s3.listObjects(request);
	List<Map<String,String>> rows = new ArrayList();
	for (S3ObjectSummary summary : listing.getObjectSummaries()) {
		HashMap<String,String> objectDetails = new HashMap<String,String>();
		objectDetails.put("bucketName",summary.getBucketName());
		objectDetails.put("key",summary.getKey());
		objectDetails.put("eTag",summary.getETag());
		objectDetails.put("lastModified",summary.getLastModified().toString());
		rows.add(objectDetails);
	}
	return rows;
}

	
}
