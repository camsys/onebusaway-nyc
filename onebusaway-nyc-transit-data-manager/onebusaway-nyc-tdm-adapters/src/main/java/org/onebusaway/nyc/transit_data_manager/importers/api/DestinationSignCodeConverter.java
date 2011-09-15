package org.onebusaway.nyc.transit_data_manager.importers.api;

import tcip_final_3_0_5_1.*;

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
import org.onebusaway.nyc.transit_data_manager.importers.CcAnnouncementInfoConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CSVCcAnnouncementInfoConverter;

/**
 * Command line tool for converting CSV destination sign code data to TCIP.
 * 
 * TCIP expected format is: ROUTE,DEST. SIGN CODE, DESTINATION SIGN CODE
 * MESSAGE, DIR, DEPOT
 */
public class DestinationSignCodeConverter {

    private FileReader inputFileReader = null;
    private FileWriter outputFileWriter = null;

    public static void main (String[] args) {
	if (2 != args.length) {
	    System.out.println("Usage: basename inputfile outputfile");
	} else {
	    DestinationSignCodeConverter ex = null;
	    try {
		ex = new DestinationSignCodeConverter(args[0], args[1]);
		ex.convertAndWrite();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    public DestinationSignCodeConverter (String inputFilePath, String outputFilePath) throws IOException {
	inputFileReader = new FileReader(inputFilePath);
	outputFileWriter = new FileWriter(outputFilePath);
    }

    public void convertAndWrite() throws IOException {
	CcAnnouncementInfoConverter inConv = new CSVCcAnnouncementInfoConverter(inputFileReader);
	CcAnnouncementInfo info = new CcAnnouncementInfo();
	CcAnnouncementInfo.Destinations destinations = inConv.getDestinations();
	info.setDestinations(destinations);
	inputFileReader.close();

	String xmlResult;
	try {
	    xmlResult = generateXml(info);
	    writeXmlToOutputFile(xmlResult);
	} catch (JAXBException e) {
	    xmlResult = "";
	    e.printStackTrace();
	}
	outputFileWriter.close();
    }

    private String generateXml(CcAnnouncementInfo inputElement) throws JAXBException {
	String outputStr = null;
	ObjectFactory tcipFinalObjectFactory = new ObjectFactory();
	JAXBElement<CcAnnouncementInfo> destinations = tcipFinalObjectFactory.createCcAnnouncementInfo(inputElement);
	StringWriter wrtr = new StringWriter();
	JAXBContext jc = JAXBContext.newInstance(CcAnnouncementInfo.class);
	Marshaller m = jc.createMarshaller();
        m.setProperty("jaxb.formatted.output", new Boolean(true));
	m.marshal(destinations, wrtr);
	outputStr = wrtr.toString();

	return outputStr;
    }

    private void writeXmlToOutputFile (String inputXml) throws IOException {
    	outputFileWriter.write(inputXml);
    }
}
