package org.onebusaway.nyc.transit_data_federation.siri;

import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleActivityStructure;

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
public class SiriXmlSerializer {

  protected static Logger _log = LoggerFactory.getLogger(SiriXmlSerializer.class);
  private JAXBContext context = null;
  
  public SiriXmlSerializer() {
    try {
      context = JAXBContext.newInstance(uk.org.siri.siri.Siri.class, SiriExtensionWrapper.class, SiriDistanceExtension.class);
    } catch(Exception e) {
      // discard
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

    // FIXME: strip off ns5 namespaces on siri root namespace. super hack, please fix me!
    String outputAsString = output.toString();    
    outputAsString = outputAsString.replaceAll("<ns5:", "<");
    outputAsString = outputAsString.replaceAll("</ns5:", "</");
    outputAsString = outputAsString.replaceAll("xmlns:ns5", "xmlns");

    outputAsString = outputAsString.replaceAll("<siriExtensionWrapper>", "");
    outputAsString = outputAsString.replaceAll("</siriExtensionWrapper>", "");

    return outputAsString;
  }
  
  public Siri fromXml(String xml) throws JAXBException {
    // we need to undo the extension wrapper hack up above so it will parse correctly
    xml = xml.replaceAll("<Extensions>", "<Extensions><SiriExtensionWrapper>");
    xml = xml.replaceAll("</Extensions>", "</SiriExtensionWrapper></Extensions>");
    Unmarshaller u = context.createUnmarshaller();
    Siri siri = (Siri) u.unmarshal(new StringReader(xml));
    // cleanup from output hack
    parseMonitoredCallExtensions(u, siri);
    return siri;
  }
  
  /**
   * The SiriExtensionWrapper is not parsed as presented.  Manually inspect the XML and populate the
   * extension and distance objects.
   * TODO find an elegant way to do this without changing the end result 
   */
  private void parseMonitoredCallExtensions(Unmarshaller u, Siri siri) throws JAXBException {
    if (siri != null && siri.getServiceDelivery() != null && siri.getServiceDelivery().getVehicleMonitoringDelivery() != null) {
      for (uk.org.siri.siri.VehicleMonitoringDeliveryStructure vmds : siri.getServiceDelivery().getVehicleMonitoringDelivery()) {
        if (vmds.getVehicleActivity() != null) {
          for (VehicleActivityStructure vas : vmds.getVehicleActivity()) {
            if (vas.getMonitoredVehicleJourney() != null 
                && vas.getMonitoredVehicleJourney().getMonitoredCall() != null
                && vas.getMonitoredVehicleJourney().getMonitoredCall().getExtensions() != null) {
              if (vas.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny() == null) continue;
              org.w3c.dom.Element extensions = (org.w3c.dom.Element) vas.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
              
              if (extensions != null && extensions.getFirstChild() != null) {
                org.w3c.dom.Node child = extensions.getFirstChild();
                SiriExtensionWrapper sew = new SiriExtensionWrapper();
                SiriDistanceExtension sed = new SiriDistanceExtension();
                if (child.getChildNodes().getLength() < 3) continue; // we didn't find what we expected
                // TODO find a better way to retrieve that does not rely on ordering
                sed.setCallDistanceAlongRoute(Double.parseDouble(child.getChildNodes().item(0).getTextContent()));
                sed.setDistanceFromCall(Double.parseDouble(child.getChildNodes().item(1).getTextContent()));
                sed.setPresentableDistance(child.getChildNodes().item(2).getTextContent());
                sed.setStopsFromCall(Integer.parseInt(child.getChildNodes().item(3).getTextContent()));
                sew.setDistances(sed);
                // swap ExtensionStructure for SiriExtensionWrapper
                vas.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().setAny(
                    sew);
              }
            } // found extensions
          } // vehicle activity structure
        } // vehicle activity
      } // vehicleMonitoringDeliveryStructure
    } // vehicleMonitoringDelivery
  }
  

}