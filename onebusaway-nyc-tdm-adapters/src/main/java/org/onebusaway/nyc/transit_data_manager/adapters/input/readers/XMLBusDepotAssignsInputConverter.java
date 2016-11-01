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

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.busAssignment.NewDataSet;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.busAssignment.NewDataSet.Table;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.SpearDepotsMappingTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLBusDepotAssignsInputConverter implements
    BusDepotAssignsInputConverter {

  private static Logger _log = LoggerFactory.getLogger(XMLBusDepotAssignsInputConverter.class);
  
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
      _log.error("file issues with xmlFile=" + xmlFile, e);
      throw e;
    } catch (XMLStreamException e) {
      _log.error("xmlissues with xmlFile=" + xmlFile, e);
      throw new IOException(e);
    } catch (JAXBException e) {
      _log.error("JAXB issues with xmlFile=" + xmlFile, e);
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
      
      if (StringUtils.isEmpty(tableDepotAssign.getBUSNUMBER()) 
          || StringUtils.isEmpty(tableDepotAssign.getDEPOT())) {
        _log.warn("Found invalid table: " + tableDepotAssign);
        continue;
      }
      
      depAssign = new MtaBusDepotAssignment();
      depAssign.setAgencyId(mappingTool.getAgencyIdFromAgency(tableDepotAssign.getAGENCY()));
      if (tableDepotAssign.getBUSNUMBER().matches("[0-9]*")){
    	  depAssign.setBusNumber(Integer.decode(stripLeadingZeros(tableDepotAssign.getBUSNUMBER())));
        // Convert any lowercase depot letters to uppercase (Jira issue OBANYC-2258)
        if (!tableDepotAssign.getDEPOT().matches("[^a-z]*")) {
          depAssign.setDepot(tableDepotAssign.getDEPOT().toUpperCase());
          _log.warn("found depot with lowercase char: " + tableDepotAssign.getDEPOT());
        } else {    
          depAssign.setDepot(tableDepotAssign.getDEPOT());
        }
        assignments.add(depAssign);
      }
      else{
    	  _log.warn("found non-numeric bus number: "+tableDepotAssign.getBUSNUMBER());
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

  private String stripLeadingZeros(String s) {
    if (s == null) return null;
    if (s.startsWith("0"))
      return stripLeadingZeros(s.substring(1, s.length()));
    return s;
  }
}
