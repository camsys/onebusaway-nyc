package org.onebusaway.nyc.transit_data_manager.adapters.api;

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
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPVehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVVehicleAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.VehicleAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.tcip.ListPullOutsGenerator;

import tcip_final_3_0_5_1.ObjectFactory;
import tcip_final_3_0_5_1.SCHPullInOutInfo;
import tcip_final_3_0_5_1.SchPullOutList;

public class VehicleAssignment {

  private FileReader inputFileReader = null;
  private FileWriter outputFileWriter = null;

  public static void main(String[] args) {
    if (2 != args.length) {
      System.out.println("Please add the input and output files as arguments");
    } else {
      VehicleAssignment ex = null;

      try {
        // This is where the good stuff happens.
        ex = new VehicleAssignment(args[0], args[1]);
        ex.convertAndWrite();
        System.out.println("done!");
      } catch (IOException e) {
        System.out.println("Could not deal with input and/or output files, please double check.");
        e.printStackTrace();
      }
    }
  }

  public VehicleAssignment(String inputFilePath, String outputFilePath)
      throws IOException {
    // Try opening the input file for reading
    System.out.println("opening " + inputFilePath);
    inputFileReader = new FileReader(inputFilePath);

    // Try opening the output file for writing
    System.out.println("Opening " + outputFilePath + " for writing.");
    outputFileWriter = new FileWriter(outputFilePath);
  }

  public void convertAndWrite() throws IOException {

    DateTime firstServiceDate = null;

    VehicleAssignsInputConverter inConv = new CSVVehicleAssignsInputConverter(
        inputFileReader);

    List<MtaUtsVehiclePullInPullOut> vehicleAssignments = inConv.getVehicleAssignments();

    System.out.println("ran getVehicleAssignments and got "
        + vehicleAssignments.size() + " results");

    firstServiceDate = vehicleAssignments.get(0).getServiceDate();

    VehicleAssignmentsOutputConverter converter = new TCIPVehicleAssignmentsOutputConverter(
        vehicleAssignments);
    List<SCHPullInOutInfo> opAssignments = converter.convertAssignments();

    ListPullOutsGenerator vehAssignsGen = new ListPullOutsGenerator(
        firstServiceDate);
    SchPullOutList pullOutList = vehAssignsGen.generateFromVehAssignList(opAssignments);

    String xmlResult;
    try {
      xmlResult = generateXml(pullOutList);
      writeXmlToOutputFile(xmlResult);
    } catch (JAXBException e) {
      xmlResult = "";
      e.printStackTrace();
    }

    outputFileWriter.close();
  }

  private String generateXml(SchPullOutList inputElement) throws JAXBException {
    String outputStr = null;

    ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
    JAXBElement<SchPullOutList> vehAssignsPushJaxbElement = tcipFinalObjectFactory.createSchPullOutList(inputElement);

    StringWriter wrtr = new StringWriter();

    JAXBContext jc = JAXBContext.newInstance(SchPullOutList.class);
    Marshaller m = jc.createMarshaller();
    m.setProperty("jaxb.formatted.output", new Boolean(true));
    // m.setProperty("jaxb.fragment", new Boolean(true));
    m.marshal(vehAssignsPushJaxbElement, wrtr);
    outputStr = wrtr.toString();

    return outputStr;
  }

  private void writeXmlToOutputFile(String inputXml) throws IOException {
    outputFileWriter.write(inputXml);
  }

}