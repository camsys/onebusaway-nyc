package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;

import org.joda.time.DateMidnight;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.tcip.PushOperatorAssignsGenerator;

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

    UtsCrewAssignsToDataCreator dataCreator = new UtsCrewAssignsToDataCreator(inputFile);
    dataCreator.setDepotIdTranslator(depotIdTranslator);
    
    OperatorAssignmentData data = dataCreator.generateDataObject();

    List<DateMidnight> serviceDates = data.getAllServiceDates();
    
    DateMidnight firstServiceDate = serviceDates.get(0);
    
    List<SCHOperatorAssignment> assignsForAllDates = new ArrayList<SCHOperatorAssignment>();
    
    Iterator<DateMidnight> sDateIt = serviceDates.iterator();
    while (sDateIt.hasNext()) {
      DateMidnight date = sDateIt.next();
      
      assignsForAllDates.addAll(data.getOperatorAssignmentsByServiceDate(date));
    }
    
    PushOperatorAssignsGenerator opAssignsGen = new PushOperatorAssignsGenerator(
        firstServiceDate);
    SchPushOperatorAssignments opAssignsPush = opAssignsGen.generateFromOpAssignList(assignsForAllDates);
    
  try {
    opAssignsPush.setCreated(getDefaultRequiredTcipAttrCreated());
  } catch (DatatypeConfigurationException e1) {
    throw new IOException(e1);
  }
  opAssignsPush.setSchVersion(getDefaultRequiredTcipAttrSchVersion());
  opAssignsPush.setSourceapp(getDefaultRequiredTcipAttrSourceapp());
  opAssignsPush.setSourceip(getDefaultRequiredTcipAttrSourceip());
  opAssignsPush.setSourceport(getDefaultRequiredTcipAttrSourceport());
  opAssignsPush.setNoNameSpaceSchemaLocation(getDefaultRequiredTcipAttrNoNameSpaceSchemaLocation());

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
