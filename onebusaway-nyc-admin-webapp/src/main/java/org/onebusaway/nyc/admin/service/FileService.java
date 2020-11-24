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

package org.onebusaway.nyc.admin.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileService {
  void setup();
  void setS3User(String user);
  void setS3Password(String password);
  void setBucketName(String bucketName);
  void setGtfsPath(String gtfsPath);
  String getGtfsPath();
  void setStifPath(String stifPath);
  String getStifPath();
  void setTransformationPath(String transformationPath);
  String getTransformationPath();
  void setBuildPath(String buildPath);
  String getConfigPath();
  void setConfigPath(String configPath);
  String getBuildPath();
  String getBucketName();
  
  boolean bundleDirectoryExists(String filename);

  boolean createBundleDirectory(String filename);

  List<String[]> listBundleDirectories(int maxResults);

  String get(String s3path, String tmpDir);
  InputStream get(String s3Path);
  String put(String key, String directory);
  
  List<String> list(String directory, int maxResults);
  
  /**
   * Creates a zip of all the output files generated in the given bundle directory during bundle building process
   * @param directoryName bundle outpur directory name
   * @return name of the zip file created
   */
  String createOutputFilesZip(String directoryName);
  
  /**
   * Validates that given file name does not contain characters which could lead to directory 
   * traversal attack.
   * @param fileName the given file name
   */
  void validateFileName(String fileName);


  /**
   * Return tabular data (filename, flag, modified date) about bundle directories.
   */
   List<String> listBundleBuilds(String directoryName, int maxResults);


  /**
   * Return fileName of objects on S3.
   */
  public List<String> listObjects (String directoryName, int maxResults);

  /**
   * Return tabular data (filename, flag, modified date) about objects on S3.
   */
  public List<Map<String,String>> listObjectsTabular (String directoryName, int maxResults);


  /**
   * Return files' names at a specified location.
   */
  public List<String> listFiles(String directoryName, int maxResults);

  /**
   * delete an object from s3
   */
  public void deleteObject(String filename);

  /**
   * Copies an s3 object from one location to another
   */
  public void copyS3Object(String fromObjectKey, String toObjectKey);
}
