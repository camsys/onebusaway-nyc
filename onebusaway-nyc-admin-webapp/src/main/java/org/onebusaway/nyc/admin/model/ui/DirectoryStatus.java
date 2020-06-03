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

package org.onebusaway.nyc.admin.model.ui;

/**
 * DTO for sending created/selected directory status to UI
 * @author abelsare
 *
 */
public class DirectoryStatus {
	private String directoryName;
	private String message;
	private boolean selected;
	private String gtfsPath;
	private String stifPath;
	private String bucketName;

	public DirectoryStatus(String directoryName, String message, boolean selected) {
		this.directoryName = directoryName;
		this.message = message;
		this.selected = selected;
	}

	public String getDirectoryName() {
		return directoryName;
	}

	public String getMessage() {
		return message;
	}

	public boolean isSelected() {
		return selected;
	}

	/**
	 * @return the gtfsPath
	 */
	public String getGtfsPath() {
		return gtfsPath;
	}

	/**
	 * @param gtfsPath the gtfsPath to set
	 */
	public void setGtfsPath(String gtfsPath) {
		this.gtfsPath = gtfsPath;
	}

	/**
	 * @return the stifPath
	 */
	public String getStifPath() {
		return stifPath;
	}

	/**
	 * @param stifPath the stifPath to set
	 */
	public void setStifPath(String stifPath) {
		this.stifPath = stifPath;
	}

	/**
	 * @return the bucketName
	 */
	public String getBucketName() {
		return bucketName;
	}

	/**
	 * @param bucketName the bucketName to set
	 */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
}
