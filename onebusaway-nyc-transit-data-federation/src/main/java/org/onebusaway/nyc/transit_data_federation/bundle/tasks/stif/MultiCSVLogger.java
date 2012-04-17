package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiCSVLogger {
  private Logger _log = LoggerFactory.getLogger(MultiCSVLogger.class);

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
    // integration tests may not have a path
    if (path == null) {
      path = System.getProperty("java.io.tmpdir");
      _log.warn("MultiCSVLogger initialized without path:  using "
          + path);
    }
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
    log.lines += 1;
    for (int i = 0; i < args.length; ++i) {
      Object arg = args[i];
      String argStr = "" + arg; //arg.toString() fails for null, while this works
      if (argStr.contains(",") || argStr.contains("\"")) {
        argStr = "\"" + argStr.replace("\"", "\"\"") + "\"";
      }

      log.stream.print(argStr);
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
      if (!name.equals("dsc statistics")) {
        stream.println(name + ": " + (log.lines == 0 ? "OK" : "FAIL"));
      }
    }
    stream.println("\nStatus\n======");

    for (Map.Entry<String, Log> entry : logs.entrySet()) {
      Log log = entry.getValue();
      String name = entry.getKey().replace("_", " ").replace(".csv", "");
      stream.println(name + ": " + log.lines);
    }
  }
}
