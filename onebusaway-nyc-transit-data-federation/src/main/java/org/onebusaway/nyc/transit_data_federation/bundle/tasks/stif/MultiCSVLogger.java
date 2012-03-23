package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

public class MultiCSVLogger {
  HashMap<String, PrintStream> streams;
  File basePath;

  public MultiCSVLogger(String path) {
    streams = new HashMap<String, PrintStream>();
    basePath = new File(path);
    if (!basePath.exists()) {
      basePath.mkdirs();
    }
  }

  public void log(String file, Object... args) {
    PrintStream stream = streams.get(file);
    if (stream == null) {
      throw new RuntimeException("log called before header for file " + file);
    }
    for (int i = 0; i < args.length; ++i) {
      Object arg = args[i];
      stream.print(arg);
      if (i != args.length - 1)
        stream.print(",");
    }
    stream.print("\n");
  }

  public void header(String file, String header) {
  
    PrintStream stream = streams.get(file);
    if (stream == null) {
      FileOutputStream outputStream;
      try {
        outputStream = new FileOutputStream(new File(basePath, file));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
      stream = new PrintStream(outputStream);
      streams.put(file, stream);
    } else {
      throw new RuntimeException("header called more than once for file " + file);
    }
    stream.print(header + "\n");
  }
}
