package org.onebusaway.nyc.admin.service.impl;

import org.onebusaway.nyc.admin.service.FileService;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
			_credentials = new BasicAWSCredentials(_username, _password);
			_s3 = new AmazonS3Client(_credentials);
		} catch (Exception ioe) {
			_log.error(ioe.toString());
			throw new RuntimeException(ioe);
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
	
}
