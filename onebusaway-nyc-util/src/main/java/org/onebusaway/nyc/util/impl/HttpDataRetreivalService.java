package org.onebusaway.nyc.util.impl;

import java.io.IOException;
import java.io.InputStream;

public class HttpDataRetreivalService implements DataRetreivalService{


    @Override
    public InputStream get(String path) throws IOException {
        return UrlUtility.readAsInputStream(path);
    }

    @Override
    public String getAsString(String path) throws IOException {
        return UrlUtility.readAsString(path);
    }


}
