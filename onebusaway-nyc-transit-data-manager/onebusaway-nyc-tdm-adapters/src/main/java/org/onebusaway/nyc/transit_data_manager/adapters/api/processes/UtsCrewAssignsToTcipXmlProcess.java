package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.joda.time.DateMidnight;
import org.onebusaway.nyc.transit_data_manager.importers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.PushOperatorAssignsGenerator;
import org.onebusaway.nyc.transit_data_manager.importers.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import tcip_final_3_0_5_1.ObjectFactory;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SchPushOperatorAssignments;

/**
 * Designed to hold code moved from CrewConverter.java
 * 
 * 
 * 
 * @author sclark
 * 
 */
public class UtsCrewAssignsToTcipXmlProcess extends FileToFileConverterProcess {

  public UtsCrewAssignsToTcipXmlProcess(File inputFile, File outputFile) {
    super(inputFile, outputFile);
  }

  @Override
  public void executeProcess() throws IOException {

    FileReader inputFileReader = new FileReader(inputFile);

    CrewAssignsInputConverter inConv = new CSVCrewAssignsInputConverter(
        inputFileReader);

    List<MtaUtsCrewAssignment> crewAssignments = inConv.getCrewAssignments();

    inputFileReader.close();

    System.out.println("ran getCrewAssignments and got "
        + crewAssignments.size() + " results");

    CrewAssignmentsOutputConverter converter = new TCIPCrewAssignmentsOutputConverter(
        crewAssignments);
    List<SCHOperatorAssignment> opAssignments = converter.convertAssignments();

    DateMidnight firstServiceDate = new DateMidnight(
        crewAssignments.get(0).getDate());
    PushOperatorAssignsGenerator opAssignsGen = new PushOperatorAssignsGenerator(
        firstServiceDate);
    SchPushOperatorAssignments opAssignsPush = opAssignsGen.generateFromOpAssignList(opAssignments);

    try {
      output = generateXml(opAssignsPush);
    } catch (JAXBException e) {
      e.printStackTrace();
    }

  }

  private String generateXml(SchPushOperatorAssignments inputElement)
      throws JAXBException {
    String outputStr = null;

    ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
    JAXBElement<SchPushOperatorAssignments> opAssignsPushJaxbElement = tcipFinalObjectFactory.createSchPushOperatorAssignments(inputElement);

    StringWriter wrtr = new StringWriter();

    JAXBContext jc = JAXBContext.newInstance(SchPushOperatorAssignments.class);
    Marshaller m = jc.createMarshaller();
    m.setProperty("jaxb.formatted.output", new Boolean(true));
    m.marshal(opAssignsPushJaxbElement, wrtr);
    outputStr = wrtr.toString();

    return outputStr;
  }

}
