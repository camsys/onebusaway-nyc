package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.data.PulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.tcip.ListPullOutsGenerator;

import tcip_final_3_0_5_1.ObjectFactory;
import tcip_final_3_0_5_1.SCHPullInOutInfo;
import tcip_final_3_0_5_1.SchPullOutList;

public class UtsVehiclePulloutToTcipXmlProcess extends
    FileToFileConverterProcess {

  public UtsVehiclePulloutToTcipXmlProcess(File inputFile, File outputFile) {
    super(inputFile, outputFile);
  }

  @Override
  public void executeProcess() throws IOException {
    UtsPulloutsToDataCreator dataCreator = new UtsPulloutsToDataCreator(inputFile);

    PulloutData data = dataCreator.generateDataObject();
    
    List<SCHPullInOutInfo> allPullouts = data.getAllPullouts();
    
    DateTimeFormatter dtf = ISODateTimeFormat.date();
    DateTime firstServiceDate = dtf.parseDateTime(allPullouts.get(0).getDate());
    
    ListPullOutsGenerator vehAssignsGen = new ListPullOutsGenerator(
        firstServiceDate);
    SchPullOutList pullOutList = vehAssignsGen.generateFromVehAssignList(allPullouts);
    
    try {
      output = generateXml(pullOutList);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }
  
  private String generateXml(SchPullOutList inputElement)
      throws JAXBException {
    String outputStr = null;

    ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
    JAXBElement<SchPullOutList> pulloutListJaxBElement = tcipFinalObjectFactory.createSchPullOutList(inputElement);
    
    StringWriter wrtr = new StringWriter();

    JAXBContext jc = JAXBContext.newInstance(SchPullOutList.class);
    Marshaller m = jc.createMarshaller();
    m.setProperty("jaxb.formatted.output", new Boolean(true));
    m.marshal(pulloutListJaxBElement, wrtr);
    outputStr = wrtr.toString();

    return outputStr;
  }

}
