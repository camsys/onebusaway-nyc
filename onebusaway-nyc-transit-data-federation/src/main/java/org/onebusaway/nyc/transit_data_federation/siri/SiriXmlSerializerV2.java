package org.onebusaway.nyc.transit_data_federation.siri;

import uk.org.siri.siri_2.Siri;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Serializer for XSD-generated SIRI classes, creating XML in the format suitable
 * for Bus Time front-ends and third-party apps.
 * 
 * @author jmaki
 *
 */
public class SiriXmlSerializerV2 {

  private JAXBContext context = null;
  private static Logger _log = LoggerFactory.getLogger(SiriXmlSerializerV2.class);
  
  public SiriXmlSerializerV2() {
    try {
      context = JAXBContext.newInstance(Siri.class, SiriExtensionWrapper.class, SiriDistanceExtension.class);
    } catch(Exception e) {
    	_log.error("Failed to Serialize Siri to XML", e);
    }
  }
  
  public String getXml(Siri siri) throws Exception {    
    Marshaller marshaller = context.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
    marshaller.setEventHandler(
        new ValidationEventHandler() {
            public boolean handleEvent(ValidationEvent event ) {
                throw new RuntimeException(event.getMessage(), event.getLinkedException());
            }
        }
    );
    
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);

    Writer output = new StringWriter();
    marshaller.marshal(siri, output);

    // FIXME: strip off ns6 namespaces on siri root namespace. super hack, please fix me!
    String outputAsString = output.toString();    
    /*outputAsString = outputAsString.replaceAll("<ns6:", "<");
    outputAsString = outputAsString.replaceAll("</ns6:", "</");
    outputAsString = outputAsString.replaceAll("xmlns:ns6", "xmlns");
*/
    outputAsString = outputAsString.replaceAll("<siriExtensionWrapper>", "");
    outputAsString = outputAsString.replaceAll("</siriExtensionWrapper>", "");

    return outputAsString;
  }
  
  public Siri fromXml(String xml) throws JAXBException {
    Unmarshaller u = context.createUnmarshaller();
    Siri siri = (Siri) u.unmarshal(new StringReader(xml));
    
    return siri;
  }

}