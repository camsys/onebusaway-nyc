package org.onebusaway.nyc.transit_data_manager.importers.api;

import tcip_final_3_0_5_1.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.importers.CcAnnouncementInfoConverter;
import org.onebusaway.nyc.transit_data_manager.importers.CSVCcAnnouncementInfoConverter;

/**
 * Command line tool for converting CSV destination sign code data to TCIP.
 *
 * TCIP expected format is:
 * ROUTE,DEST. SIGN CODE, DESTINATION SIGN CODE MESSAGE, DIR, DEPOT
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
	CcAnnouncementInfo.Destinations destinations = inConv.getDestinations();
	inputFileReader.close();
    }

}
