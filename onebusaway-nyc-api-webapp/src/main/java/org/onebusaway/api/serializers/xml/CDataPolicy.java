package org.onebusaway.api.serializers.xml;

@FunctionalInterface
interface CDataPolicy {
  boolean shouldWrap(XmlScope scope, String nodeName);
}
