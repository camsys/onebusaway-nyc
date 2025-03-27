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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

public class UrlUtility {

    private static final Logger log = LoggerFactory.getLogger(UrlUtility.class);

    private static final int DEFAULT_READ_TIMEOUT = Integer.parseInt(System.getProperty("oba.defaultReadTimeout", "60000"));
    private static final int DEFAULT_CONNECTION_TIMEOUT = Integer.parseInt(System.getProperty("oba.defaultConnectTimeout", "60000"));

    public static BufferedReader readUrl(String urlString) throws IOException {
        return readUrl(new URL(urlString));
    }

    public static BufferedReader readUrl(URL url) throws IOException {
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    public static Set<String> readAsStringSet(String url) throws IOException {
        return readAsStringSet(new URL(url));
    }

    public static Set<String> readAsStringSet(URL url) throws IOException {
        try (BufferedReader reader = readUrl(url)) {
            Set<String> set = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                set.add(line);
            }
            return set;
        }
    }

    public static String readAsString(String url) throws IOException {
        return readAsString(new URL(url), DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static String readAsString(URL url) throws IOException {
        return readAsString(url, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static String readAsString(URL url, Integer connectionTimeout, Integer readTimeout) throws IOException {
        try {
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(connectionTimeout != null ? connectionTimeout : DEFAULT_CONNECTION_TIMEOUT);
            conn.setReadTimeout(readTimeout != null ? readTimeout : DEFAULT_READ_TIMEOUT);

            try (InputStream in = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                StringBuilder output = new StringBuilder();
                int cp;
                while ((cp = reader.read()) != -1) {
                    output.append((char) cp);
                }
                return output.toString();
            }

        } catch (IOException e) {
            log.error("Error getting contents of url: {}", url != null ? url.toExternalForm() : "url unavailable", e);
            throw e;
        }
    }

    public static InputStream readAsInputStream(String path) throws IOException {
        return readAsInputStream(new URL(path), DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static InputStream readAsInputStream(URL url) throws IOException{
        return readAsInputStream(url, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static InputStream readAsInputStream(String url, Integer connectionTimeout, Integer readTimeout) throws IOException {
        return readAsInputStream(url, connectionTimeout, readTimeout);
    }

    public static InputStream readAsInputStream(URL url, Integer connectionTimeout, Integer readTimeout) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(connectionTimeout != null ? connectionTimeout : DEFAULT_CONNECTION_TIMEOUT);
        conn.setReadTimeout(readTimeout != null ? readTimeout : DEFAULT_READ_TIMEOUT);
        return conn.getInputStream();
    }

}
