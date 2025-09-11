package org.onebusaway.api.serializers.xml;

final class CDataSanitizer {
  static String sanitize(String text) {
    return text == null ? "" : text.replace("]]>", "]]]]><![CDATA[>");
  }
}
