package org.onebusaway.api.serializers.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;

import java.io.Writer;

final class XStreamFactory {
  static XStream createWithPolicy(CDataPolicy policy) {
    XStream xs = new XStream(new XppDriver() {
      @Override
      public HierarchicalStreamWriter createWriter(Writer out) {
        if(policy instanceof SituationPolicy) {
          return new SituationCDataWriter(out, policy);
        }
        return super.createWriter(out);
      }
    });
    xs.setMode(XStream.NO_REFERENCES);
    return xs;
  }
}
