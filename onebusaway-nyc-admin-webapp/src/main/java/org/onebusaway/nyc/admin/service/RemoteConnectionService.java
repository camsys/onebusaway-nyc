package org.onebusaway.nyc.admin.service;

/**
 * Makes connection to the remote server with the given URL and retrieves the required content
 * @author abelsare
 *
 */
public interface RemoteConnectionService {
	
	/**
	 * Opens a connection to the remote server and retrieves the content. Content is returned as string
	 * separated by new line
	 * @param url url of the remote server
	 * @return the required content as string
	 */
	String getContent(String url);

}
