package org.onebusaway.api.serializers.xml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class SituationPolicy implements CDataPolicy {
  private static final Set<String> PARENTS = new HashSet<>(Arrays.asList("summary", "description"));
  @Override
  public boolean shouldWrap(XmlScope scope, String nodeName) {
    String parent = scope.parentName();
    return scope.hasElement()
        && "value".equals(nodeName)
        && parent != null
        && PARENTS.contains(parent);
  }
}
