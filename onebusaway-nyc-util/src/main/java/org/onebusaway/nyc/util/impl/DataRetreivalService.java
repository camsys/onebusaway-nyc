package org.onebusaway.nyc.util.impl;

import java.io.IOException;
import java.io.InputStream;

public interface DataRetreivalService {
    InputStream get(String path) throws IOException;

    String getAsString(String path) throws IOException;
}
