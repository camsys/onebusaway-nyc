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

package org.onebusaway.nyc.util.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

public class UrlUtility {

    private static Logger log = LoggerFactory.getLogger(UrlUtility.class);

    private static int DEFAULT_READ_TIMEOUT = Integer.parseInt(System.getProperty("oba.defaultReadTimeout", "60000")); // 60 * 1000;
    private static int DEFAULT_CONNECTION_TIMEOUT = Integer.parseInt(System.getProperty("oba.defaultConnectTimeout", "60000")); // 60 * 1000;

    public static BufferedReader readUrl(String urlString) throws IOException {
        return readUrl(new URL(urlString));
    }

    public static BufferedReader readUrl(URL url) throws IOException {
        InputStream in = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        return reader;
    }

    public static Set<String> readAsStringSet(String url) throws IOException {
        return readAsStringSet(readUrl(url));
    }

    public static Set<String> readAsStringSet(URL url) throws IOException {
        return readAsStringSet(readUrl(url));
    }


    private static Set<String> readAsStringSet(BufferedReader reader) throws IOException {
        HashSet<String> set = new HashSet<String>();
        String line = null;
        while ((line = reader.readLine()) != null) {
                set.add(line);
        }
        return set;
    }

    public static String readAsString(URL requestUrl) throws IOException {
        return readAsString(requestUrl, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static String readAsString(URL requestUrl, Integer connectionTimeout, Integer readTimeout) throws IOException {
        if(connectionTimeout == null){
            connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        }
        if(readTimeout == null){
            readTimeout = DEFAULT_READ_TIMEOUT;
        }

        BufferedReader br = null;
        InputStream inStream = null;
        URLConnection conn = null;

        try{
            conn = requestUrl.openConnection();
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(readTimeout);
            inStream = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(inStream));
            StringBuilder output = new StringBuilder();

            int cp;
            while ((cp = br.read()) != -1) {
                output.append((char) cp);
            }

            return output.toString();
        }
        catch(IOException ioe){
            String url = requestUrl != null ? requestUrl.toExternalForm() : "url unavailable";
            log.error("Error getting contents of url: " + url);
            throw ioe;
        }finally{
            try{
                if(br != null) br.close();
                if(inStream != null) inStream.close();
            }
            catch(IOException ioe2){
                log.error("Error closing connection");
            }
        }

    }
}
