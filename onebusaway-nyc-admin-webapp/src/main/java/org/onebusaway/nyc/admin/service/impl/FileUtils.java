package org.onebusaway.nyc.admin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
/**
 * A collection of file handling utilities making life easier when working
 * with Java. 
 *
 */
public class FileUtils {
  private static Logger _log = LoggerFactory.getLogger(FileUtils.class);
  private static final int CHUNK_SIZE = 1024;
  private String _workingDirectory = null;

  public FileUtils() {
    _workingDirectory = System.getProperty("java.io.tmpdir");
  }

  public FileUtils(String workingDirectory) {
    _workingDirectory = workingDirectory;
  }

  /**
   * Retrieve a file at the remote URL.  THe file will be named the last portion of the URL, 
   * following the conventions of the UNIX tool wget. 
   */
  public void wget(String urlString) {
    URL url;
    InputStream is = null;
    DataInputStream dis = null;
    DataOutputStream dos = null;
    String fileName = parseFileName(urlString);
    _log.debug("downloading " + urlString + " to fileName " + _workingDirectory
        + File.separatorChar + fileName);
    byte[] buff = new byte[CHUNK_SIZE];
    int read = 0;
    try {
      url = new URL(urlString);
      is = url.openStream();
      dis = new DataInputStream(new BufferedInputStream(is));
      dos = new DataOutputStream(new FileOutputStream(_workingDirectory
          + File.separatorChar + fileName));
      while ((read = dis.read(buff)) > -1) {
        dos.write(buff, 0, read);
      }
    } catch (Exception any) {
      throw new RuntimeException(any);
    } finally {
      if (dis != null)
        try {
          dis.close();
        } catch (Exception e1) {
        }
      if (dos != null)
        try {
          dos.close();
        } catch (Exception e2) {
        }
    }
  }

  /**
   * Copy the input stream to the given destinationFileName (which includes path and filename).
   */
  public void copy(InputStream source, String destinationFileName) {
    byte[] buff = new byte[CHUNK_SIZE];
    DataOutputStream destination = null;
    int read = 0;
    try {
      destination = new DataOutputStream(new FileOutputStream(destinationFileName));
      // lazy copy -- not recommend
      while ((read = source.read(buff)) > -1) {
        destination.write(buff, 0, read);
      }
    } catch (Exception any) {
      _log.error(any.toString());
      throw new RuntimeException(any);
    } finally {
      if (source != null)
        try {
        source.close();
        } catch (Exception any) {}
      if (destination != null)
        try {
          destination.close();
        } catch (Exception any) {}
    }

  }
  
  public String parseFileName(String urlString) {
    int i = urlString.lastIndexOf("/");
    return urlString.substring(i, urlString.length());
  }

    public String parseBucket(String s3path) {
    if (s3path.indexOf("s3://") == -1) {
      throw new RuntimeException("Invalid s3path, missing protocol s3://; path=" + s3path);
    }
    int start = s3path.indexOf("/", 5);
    int end = s3path.indexOf("/", start+1);
    return s3path.substring(start, end);
  }

    public String parseKey(String s3path) {
    if (s3path.indexOf("s3://") == -1) {
      throw new RuntimeException("Invalid s3path, missing protocol s3://; path=" + s3path);
    }
    int bucketStart = s3path.indexOf("/", 5);
    int start = s3path.indexOf("/", bucketStart+1);
    return s3path.substring(start, s3path.length());
  }


  /**
   * untar and uncompress a tar file (.tar.gz)
   */
  public int tarzxf(String tarFile) {
    Process process = null;
    try {
      String cmd = "bash -c \"cd " + _workingDirectory + " && tar zxf " + tarFile + "\"";
      process = Runtime.getRuntime().exec(cmd);
      return process.waitFor();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * set UNIX permissions.  The permissions string is passed through as is so
   * it can be of the octal format 777 or o+x format.
   */
  public int chmod(String permissions, String destinationFileName) {
    Process process = null;
    try {
      String cmd = "chmod " + permissions + " " + destinationFileName;
      process = Runtime.getRuntime().exec(cmd);
      return process.waitFor();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // TODO not implemented yet
  public String createTmpDirectory() {
    // TODO Auto-generated method stub
    return System.getProperty("java.io.tmpdir");
  }
}
