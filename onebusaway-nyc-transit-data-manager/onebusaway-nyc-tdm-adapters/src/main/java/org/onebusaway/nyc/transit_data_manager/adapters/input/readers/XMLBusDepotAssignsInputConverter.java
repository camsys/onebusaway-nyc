package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.busAssignment.NewDataSet;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.busAssignment.NewDataSet.Table;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.SpearDepotsMappingTool;

public class XMLBusDepotAssignsInputConverter implements
    BusDepotAssignsInputConverter {

  // private UtsMappingTool mappingTool = null;
  private SpearDepotsMappingTool mappingTool;
  private File xmlFile;

  public void setMappingTool(SpearDepotsMappingTool mappingTool) {
    this.mappingTool = mappingTool;
  }

  public XMLBusDepotAssignsInputConverter(File xmlInputFile) {
    xmlFile = xmlInputFile;

    mappingTool = new SpearDepotsMappingTool();
  }

  public List<MtaBusDepotAssignment> getBusDepotAssignments() throws IOException {
    List<MtaBusDepotAssignment> assignments = null;

    NewDataSet enclosingXml = null;

    XMLInputFactory xmlInputFact = XMLInputFactory.newInstance();

    XMLStreamReader reader = null;
    try {
      reader = xmlInputFact.createXMLStreamReader(
          new FileReader(xmlFile));
      enclosingXml = unmarshall(NewDataSet.class, reader);
    } catch (FileNotFoundException e) {
      throw e;
    } catch (XMLStreamException e) {
      throw new IOException(e);
    } catch (JAXBException e) {
      throw new IOException(e);
    } finally {
      if (reader != null)
        try {
          reader.close();
        } catch (XMLStreamException e) {}
    }

    assignments = new ArrayList<MtaBusDepotAssignment>();

    List<Table> xmlTables = enclosingXml.getTable();

    Iterator<Table> tableIt = xmlTables.iterator();

    MtaBusDepotAssignment depAssign = null;
    while (tableIt.hasNext()) {
      Table tableDepotAssign = tableIt.next();

      depAssign = new MtaBusDepotAssignment();
      depAssign.setAgencyId(mappingTool.getAgencyIdFromAgency(tableDepotAssign.getAGENCY()));
      depAssign.setBusNumber(tableDepotAssign.getBUSNUMBER());
      depAssign.setDepot(tableDepotAssign.getDEPOT());

      assignments.add(depAssign);
    }

    return assignments;
  }

  // This function was pretty much lifted from the jaxb tutorial page.
  private <T> T unmarshall(Class<T> docClass, XMLStreamReader inputReader)
      throws JAXBException {
    String packageName = docClass.getPackage().getName();
    JAXBContext jc = JAXBContext.newInstance(packageName);
    Unmarshaller u = jc.createUnmarshaller();
    JAXBElement<T> doc = (JAXBElement<T>) u.unmarshal(inputReader, docClass);
    return doc.getValue();
  }

}
