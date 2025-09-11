package org.onebusaway.api.serializers.xml;

public interface XmlScope {
  String parentName();

  boolean hasElement();

  void onStartNode(String name);

  void onAttribute(String key, String value);

  void onEndNode();
}
