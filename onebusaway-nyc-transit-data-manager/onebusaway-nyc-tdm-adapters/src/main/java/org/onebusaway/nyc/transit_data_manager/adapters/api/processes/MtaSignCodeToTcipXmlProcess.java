package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.onebusaway.nyc.transit_data_manager.adapters.data.SignCodeData;

import tcip_final_3_0_5_1.CcAnnouncementInfo;
import tcip_final_3_0_5_1.ObjectFactory;
import tcip_final_3_0_5_1.SchPullOutList;

public class MtaSignCodeToTcipXmlProcess extends FileToFileConverterProcess {

  public MtaSignCodeToTcipXmlProcess(File inputFile, File outputFile) {
    super(inputFile, outputFile);
  }

  @Override
  public void executeProcess() throws IOException {

    CsvSignCodeToDataCreator dataCreator = new CsvSignCodeToDataCreator(inputFile);
    
    SignCodeData data = dataCreator.generateDataObject();
    
    CcAnnouncementInfo.Destinations destinations = new CcAnnouncementInfo.Destinations();
    destinations.getDestination().addAll(data.getAllDisplays());
    
    CcAnnouncementInfo info = new CcAnnouncementInfo();
    info.setDestinations(destinations);
    
    try {
      output = generateXml(info);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }

  
  private String generateXml(CcAnnouncementInfo inputElement)
      throws JAXBException {
    String outputStr = null;

    ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
    JAXBElement<CcAnnouncementInfo> announceInfoJaxBElement = tcipFinalObjectFactory.createCcAnnouncementInfo(inputElement);
    
    StringWriter wrtr = new StringWriter();

    JAXBContext jc = JAXBContext.newInstance(CcAnnouncementInfo.class);
    Marshaller m = jc.createMarshaller();
    m.setProperty("jaxb.formatted.output", new Boolean(true));
    m.marshal(announceInfoJaxBElement, wrtr);
    outputStr = wrtr.toString();

    return outputStr;
  }
  
}
