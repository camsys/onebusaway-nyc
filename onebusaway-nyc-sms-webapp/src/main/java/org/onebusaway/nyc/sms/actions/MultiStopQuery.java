package org.onebusaway.nyc.sms.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Parser for explicit SMS stop lists and route filters; no proximity lookups. */
final class MultiStopQuery {
  static final class Request {
    private final List<String> stopIds;
    private final Set<String> routes;
    private final String error;

    Request(List<String> stopIds, Set<String> routes, String error) {
      this.stopIds = stopIds;
      this.routes = routes;
      this.error = error;
    }
    List<String> getStopIds() { return stopIds; }
    Set<String> getRoutes() { return routes; }
    String getError() { return error; }
  }

  static Request parse(String text) {
    if (text == null) return null;
    text = text.trim().toUpperCase(Locale.US).replaceAll("\\s*,\\s*", ",");
    if (text.isEmpty()) return null;
    String[] tokens = text.split("\\s+");
    // Commas separate stop IDs; route filters are whitespace-separated.
    // Single-stop/single-route requests keep their legacy execution path.
    if (!tokens[0].matches("[0-9]+(?:,[0-9]+)*")) return null;
    if (!tokens[0].contains(",") && tokens.length <= 2) return null;
    for (int i = 1; i < tokens.length; i++) {
      if (!tokens[i].matches("[A-Z0-9]+")) return null;
    }
    String[] ids = tokens[0].split(",");
    if (ids.length > 3 || tokens.length > 4) {
      return new Request(Collections.<String>emptyList(), Collections.<String>emptySet(),
          "Max 3 stops and 3 routes.");
    }
    List<String> stops = new ArrayList<String>();
    for (String id : ids) {
      if (!stops.contains(id)) stops.add(id);
    }
    Set<String> routes = new HashSet<String>();
    for (int i = 1; i < tokens.length; i++) routes.add(tokens[i]);
    return new Request(stops, routes, null);
  }
}
