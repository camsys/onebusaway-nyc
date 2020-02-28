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
