package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

import java.io.Reader;
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
import org.onebusaway.nyc.transit_data_manager.adapters.tools.UtsMappingTool;

public class XMLBusDepotAssignsInputConverter implements
    BusDepotAssignsInputConverter {

  private UtsMappingTool mappingTool = null;

  private Reader inputReader = null;

  public XMLBusDepotAssignsInputConverter(Reader csvInputReader) {
    inputReader = csvInputReader;

    mappingTool = new UtsMappingTool();
  }

  public List<MtaBusDepotAssignment> getBusDepotAssignments() {
    List<MtaBusDepotAssignment> assignments = null;

    NewDataSet enclosingXml = null;

    XMLInputFactory xmlInputFact = XMLInputFactory.newInstance();

    XMLStreamReader reader = null;
    try {
      reader = xmlInputFact.createXMLStreamReader(inputReader);
    } catch (XMLStreamException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      return null;
    }

    if (reader != null) {
      try {
        enclosingXml = unmarshall(NewDataSet.class, reader);
      } catch (JAXBException e) {
        e.printStackTrace();
        return null;
      }

      assignments = new ArrayList<MtaBusDepotAssignment>();

      List<Table> xmlTables = enclosingXml.getTable();

      Iterator<Table> tableIt = xmlTables.iterator();

      Table tableDepotAssign = null;
      MtaBusDepotAssignment depAssign = null;
      while (tableIt.hasNext()) {
        tableDepotAssign = tableIt.next();

        depAssign = new MtaBusDepotAssignment();
        depAssign.setAgencyId(mappingTool.getAgencyIdFromDepotAssignAgency(tableDepotAssign.getAGENCY()));
        depAssign.setBusNumber(tableDepotAssign.getBUSNUMBER());
        depAssign.setDepot(tableDepotAssign.getDEPOT());

        assignments.add(depAssign);
      }

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
