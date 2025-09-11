package org.onebusaway.api.serializers.xml;

import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import java.io.Writer;

final class SituationCDataWriter extends PrettyPrintWriter {
  private final XmlScope scope = new SituationXmlScope();
  private final CDataPolicy policy;
  private boolean cdata;

  SituationCDataWriter(Writer out, CDataPolicy policy) {
    super(out);
    this.policy = policy;
  }

  @Override
  public void startNode(String name, @SuppressWarnings("rawtypes") Class clazz) {
    cdata = policy.shouldWrap(scope, name);
    if(cdata){
      System.out.println("true");
    }
    scope.onStartNode(name);
    super.startNode(name, clazz);
  }

  @Override
  public void addAttribute(String key, String value) {
    scope.onAttribute(key, value);
    super.addAttribute(key, value);
  }

  @Override
  public void endNode() {
    super.endNode();
    scope.onEndNode();
    cdata = false;
  }

  @Override
  protected void writeText(QuickWriter w, String text) {
    if (cdata && text != null && !text.isEmpty()) {
      w.write("<![CDATA[");
      w.write(CDataSanitizer.sanitize(text));
      w.write("]]>");
    } else {
      super.writeText(w, text);
    }
  }
}
