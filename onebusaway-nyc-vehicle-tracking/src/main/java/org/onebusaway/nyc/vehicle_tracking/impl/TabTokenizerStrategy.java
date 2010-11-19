package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.Arrays;
import java.util.List;

import org.onebusaway.gtfs.csv.TokenizerStrategy;

public class TabTokenizerStrategy implements TokenizerStrategy {
  @Override
  public List<String> parse(String line) {
    return Arrays.asList(line.split("\t"));
  }

  @Override
  public String format(Iterable<String> tokens) {
    boolean seenFirst = false;
    StringBuilder b = new StringBuilder();
    for (String token : tokens) {
      if (seenFirst)
        b.append('\t');
      else
        seenFirst = true;

      b.append(token);
    }
    return b.toString();
  }
}