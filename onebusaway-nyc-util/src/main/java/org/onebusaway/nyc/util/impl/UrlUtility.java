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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class UrlUtility {

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
}
