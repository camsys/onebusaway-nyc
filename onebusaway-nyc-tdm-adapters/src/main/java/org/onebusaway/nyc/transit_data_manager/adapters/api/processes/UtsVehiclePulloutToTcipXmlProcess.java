package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.data.PulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.onebusaway.nyc.transit_data_manager.adapters.tcip.ListPullOutsGenerator;

import tcip_final_4_0_0.ObjectFactory;
import tcip_final_4_0_0.SchPullOutList;

public class UtsVehiclePulloutToTcipXmlProcess extends
    FileToFileConverterProcess {
	
	private VehicleAssignmentsOutputConverter converter;

  public UtsVehiclePulloutToTcipXmlProcess(File inputFile, File outputFile, VehicleAssignmentsOutputConverter converter) {
    super(inputFile, outputFile);
    this.converter = converter;
  }

  @Override
  public void executeProcess() throws IOException {
    UtsPulloutsToDataCreator dataCreator = new UtsPulloutsToDataCreator(inputFile);

    dataCreator.setDepotIdTranslator(depotIdTranslator);
    
    dataCreator.setConverter(converter);
    
    PulloutData data = dataCreator.generateDataObject();
    
    List<VehiclePullInOutInfo> allPullouts = data.getAllPullouts();
    
    DateTimeFormatter dtf = ISODateTimeFormat.date();
    DateTime firstServiceDate = dtf.parseDateTime(allPullouts.get(0).getPullOutInfo().getDate());
    
    ListPullOutsGenerator vehAssignsGen = new ListPullOutsGenerator(
        firstServiceDate);
    SchPullOutList pullOutList = vehAssignsGen.generateFromVehAssignList(allPullouts);
    
    try {
      pullOutList.setCreated(getDefaultRequiredTcipAttrCreated());
    } catch (DatatypeConfigurationException e1) {
      throw new IOException(e1);
    }
    pullOutList.setSchVersion(getDefaultRequiredTcipAttrSchVersion());
    pullOutList.setSourceapp(getDefaultRequiredTcipAttrSourceapp());
    pullOutList.setSourceip(getDefaultRequiredTcipAttrSourceip());
    pullOutList.setSourceport(getDefaultRequiredTcipAttrSourceport());
    pullOutList.setNoNameSpaceSchemaLocation(getDefaultRequiredTcipAttrNoNameSpaceSchemaLocation());
    
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

/**
 * @param converter the converter to set
 */
public void setConverter(VehicleAssignmentsOutputConverter converter) {
	this.converter = converter;
}

}
