package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.onebusaway.nyc.transit_data_manager.importers.BusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.BusDepotAssignsToDepotMapTranslator;
import org.onebusaway.nyc.transit_data_manager.importers.FleetSubsetsGenerator;
import org.onebusaway.nyc.transit_data_manager.importers.GroupByPropInListObjectTranslator;
import org.onebusaway.nyc.transit_data_manager.importers.TCIPBusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.importers.XMLBusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.model.MtaBusDepotAssignment;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CptFleetSubsets;
import tcip_final_3_0_5_1.ObjectFactory;

public class MtaBusDepotsToTcipXmlProcess extends FileToFileConverterProcess {

  public MtaBusDepotsToTcipXmlProcess(File inputFile, File outputFile) {
    super(inputFile, outputFile);
  }

  public void executeProcess() throws FileNotFoundException {

    FileReader inputFileReader = new FileReader(inputFile);

    BusDepotAssignsInputConverter inConv = new XMLBusDepotAssignsInputConverter(
        inputFileReader);

    List<MtaBusDepotAssignment> assignments = inConv.getBusDepotAssignments();

    System.out.println("ran getVehicleAssignments and got "
        + assignments.size() + " results");

    // With Bus Depot Assignments we need to group MtaBusDepotAssignment s by
    // the depot field.
    // I'll add another class to do that.
    GroupByPropInListObjectTranslator<List<MtaBusDepotAssignment>, Map<String, List<MtaBusDepotAssignment>>> translator = new BusDepotAssignsToDepotMapTranslator();
    // Create a map to map depot codes to lists of MtaBusDepotAssignment s
    // corresponding to that depot.
    Map<String, List<MtaBusDepotAssignment>> depotBusesMap = translator.restructure(assignments);

    BusDepotAssignmentsOutputConverter converter = new TCIPBusDepotAssignmentsOutputConverter(
        depotBusesMap);
    List<CPTFleetSubsetGroup> fleetSSGroups = converter.convertAssignments();

    FleetSubsetsGenerator fleetSSGen = new FleetSubsetsGenerator();
    CptFleetSubsets fleetSubsets = fleetSSGen.generateFromSubsetGroups(fleetSSGroups);

    try {
      output = generateXml(fleetSubsets);
    } catch (JAXBException e) {
      e.printStackTrace();
    }

  }

  private String generateXml(CptFleetSubsets inputElement) throws JAXBException {
    String outputStr = null;

    ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
    JAXBElement<CptFleetSubsets> inputWrappedAsJaxbElement = tcipFinalObjectFactory.createCptFleetSubsets(inputElement);

    StringWriter wrtr = new StringWriter();

    JAXBContext jc = JAXBContext.newInstance(CptFleetSubsets.class);
    Marshaller m = jc.createMarshaller();
    m.setProperty("jaxb.formatted.output", new Boolean(true));
    m.marshal(inputWrappedAsJaxbElement, wrtr);
    outputStr = wrtr.toString();

    return outputStr;
  }

}
