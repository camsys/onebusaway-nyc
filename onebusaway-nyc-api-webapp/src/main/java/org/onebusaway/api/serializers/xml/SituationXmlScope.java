package org.onebusaway.api.serializers.xml;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;


final class SituationXmlScope implements XmlScope, Serializable {
  private final Deque<String> nameStack = new ArrayDeque<>();
  private final Deque<Boolean> situationNodeStack = new ArrayDeque<>();
  private int situationDepth = 0; // >0 means inside <entry class="situation">

  @Override
  public String parentName() { return nameStack.peek(); }

  @Override
  public boolean hasElement() { return situationDepth > 0; }

  @Override
  public void onStartNode(String name) {
    nameStack.push(name);
    situationNodeStack.push(Boolean.FALSE);
  }

  @Override
  public void onAttribute(String key, String value) {
    if ("entry".equals(nameStack.peek()) && "class".equals(key) && "situation".equals(value)) {
      if (!Boolean.TRUE.equals(situationNodeStack.peek())) {
        situationNodeStack.pop();
        situationNodeStack.push(Boolean.TRUE);
        situationDepth++;
      }
    }
  }

  @Override
  public void onEndNode() {
    if (Boolean.TRUE.equals(situationNodeStack.pop())) {
      situationDepth--;
    }
    nameStack.pop();
  }
}
