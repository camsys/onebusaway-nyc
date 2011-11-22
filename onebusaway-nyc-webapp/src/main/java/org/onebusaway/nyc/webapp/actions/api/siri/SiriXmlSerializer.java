package org.onebusaway.nyc.webapp.actions.api.siri;

import org.onebusaway.nyc.presentation.model.realtime.SiriDistanceExtension;
import org.onebusaway.nyc.presentation.model.realtime.SiriExtensionWrapper;

import uk.org.siri.siri.Siri;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.DocumentBuilderFactory;

public class SiriXmlSerializer {
  
  public static String getXml(Siri siri) throws Exception {    
    JAXBContext context = JAXBContext.newInstance(uk.org.siri.siri.Siri.class, SiriExtensionWrapper.class, SiriDistanceExtension.class);

    Marshaller marshaller = context.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
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

    // FIXME: strip off ns5 namespaces on siri root namespace. super hack, please fix me!
    String outputAsString = output.toString();    
    outputAsString = outputAsString.replaceAll("<ns5:", "<");
    outputAsString = outputAsString.replaceAll("</ns5:", "</");
    outputAsString = outputAsString.replaceAll("xmlns:ns5", "xmlns");
    
    return outputAsString;
  }

}