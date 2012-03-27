package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class MultiCSVLogger {
  class Log {
    int lines;
    PrintStream stream;
    Log(String file) {
      FileOutputStream outputStream;
      try {
        outputStream = new FileOutputStream(new File(basePath, file));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
      stream = new PrintStream(outputStream);
    }
  }
  HashMap<String, Log> logs;
  
  File basePath;

  public MultiCSVLogger(String path) {
    logs = new HashMap<String, Log>();
    basePath = new File(path);
    if (!basePath.exists()) {
      basePath.mkdirs();
    }
  }

  public void log(String file, Object... args) {
    Log log = logs.get(file);
    if (log == null) {
      throw new RuntimeException("log called before header for file " + file);
    }
    for (int i = 0; i < args.length; ++i) {
      Object arg = args[i];
      log.stream.print(arg);
      if (i != args.length - 1)
        log.stream.print(",");
    }
    log.stream.print("\n");
  }

  public void header(String file, String header) {
  
    Log log = logs.get(file);
    if (log == null) {
      log = new Log(file);
      logs.put(file, log);
    } else {
      throw new RuntimeException("header called more than once for file " + file);
    }
    log.stream.print(header + "\n");
  }
  
  private Log Log(String file) {
    // TODO Auto-generated method stub
    return null;
  }

  public void summarize() {
    FileOutputStream outputStream;
    try {
      outputStream = new FileOutputStream(new File(basePath, "summary.txt"));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    PrintStream stream = new PrintStream(outputStream);
    stream.println("Task Status\n===========");
    for (Map.Entry<String, Log> entry : logs.entrySet()) {
      Log log = entry.getValue();
      String name = entry.getKey().replace("_", " ").replace(".csv", "");
      stream.println(name + ": " + (log.lines == 0 ? "OK" : "FAIL"));
    }
    stream.println("\nStatus\n======");

    for (Map.Entry<String, Log> entry : logs.entrySet()) {
      Log log = entry.getValue();
      String name = entry.getKey().replace("_", " ").replace(".csv", "");
      stream.println(name + ": " + log.lines);
    }
  }
}
