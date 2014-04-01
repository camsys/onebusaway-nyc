package org.onebusaway.nyc.admin.service;

import java.io.File;
import java.util.Map;


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
	String postContent(String url, Map<String,String> params);
	
	/**
	 * Posts binary data to the given url and returns response of the given type
	 * @param url remote server url
	 * @param data binary data to post
	 * @param responseType desired type of post response
	 * @return response returned by remote server 
	 */
	<T> T postBinaryData(String url, File data, Class<T> responseType);

}
