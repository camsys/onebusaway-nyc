package org.onebusaway.nyc.transit_data_manager.importers.api;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.importers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.PushOperatorAssignsGenerator;
import org.onebusaway.nyc.transit_data_manager.importers.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.model.MtaUtsCrewAssignment;

import tcip_final_3_0_5_1.ObjectFactory;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SchPushOperatorAssignments;

/***
 * Initially designed as a command line tool to open two files and run the
 * conversion logic to map input UTS Crew data (initially in CSV format) to TCIP
 * XML.
 * 
 * @author sclark
 * 
 */
public class CrewConverter {

  private FileReader inputFileReader = null;
  private FileWriter outputFileWriter = null;

  public static void main(String[] args) {
    if (2 != args.length) {
      System.out.println("Please add the input and output files as arguments");
    } else {
      CrewConverter ex = null;

      try {
        // This is where the good stuff happens.
        ex = new CrewConverter(args[0], args[1]);
        ex.convertAndWrite();
        System.out.println("done!");
      } catch (IOException e) {
        System.out.println("Could not deal with input and/or output files, please double check.");
        e.printStackTrace();
      }
    }
  }

  public CrewConverter(String inputFilePath, String outputFilePath)
      throws IOException {
    // Try opening the input file for reading
    System.out.println("opening " + inputFilePath);
    inputFileReader = new FileReader(inputFilePath);

    // Try opening the output file for writing
    System.out.println("Opening " + outputFilePath + " for writing.");
    outputFileWriter = new FileWriter(outputFilePath);
  }

  public void convertAndWrite() throws IOException {

    CrewAssignsInputConverter inConv = new CSVCrewAssignsInputConverter(
        inputFileReader);

    List<MtaUtsCrewAssignment> crewAssignments = inConv.getCrewAssignments();

    inputFileReader.close();

    System.out.println("ran getCrewAssignments and got "
        + crewAssignments.size() + " results");

    CrewAssignmentsOutputConverter converter = new TCIPCrewAssignmentsOutputConverter(
        crewAssignments);
    List<SCHOperatorAssignment> opAssignments = converter.convertAssignments();

    DateTime nowCal = new DateTime();
    PushOperatorAssignsGenerator opAssignsGen = new PushOperatorAssignsGenerator(
        nowCal);
    SchPushOperatorAssignments opAssignsPush = opAssignsGen.generateFromOpAssignList(opAssignments);

    String xmlResult;
    try {
      xmlResult = generateXml(opAssignsPush);
      writeXmlToOutputFile(xmlResult);
    } catch (JAXBException e) {
      xmlResult = "";
      e.printStackTrace();
    }

    outputFileWriter.close();
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

  private void writeXmlToOutputFile(String inputXml) throws IOException {
    outputFileWriter.write(inputXml);
  }
}